package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.BookingDto;
import com.nikhil.airbnb.dto.BookingRequest;
import com.nikhil.airbnb.dto.HotelReportDto;
import com.nikhil.airbnb.entity.*;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.exception.UnauthorizedException;
import com.nikhil.airbnb.repository.*;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import com.nikhil.airbnb.service.serviceInterfaces.BookingService;
import com.nikhil.airbnb.service.serviceInterfaces.CheckoutService;
import com.nikhil.airbnb.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingServiceImpl implements BookingService {
    // =====================================================================================================================
    BookingRepository bookingRepository;
    HotelRepository hotelRepository;
    RoomRepository roomRepository;
    InventoryRepository inventoryRepository;
    CheckoutService checkoutService;
    GuestRepository guestRepository;
    AppUserService appUserService;
    PricingService pricingService;
    ModelMapper modelMapper;

    @Value("${frontend.url}")
    @NonFinal String frontendUrl;
    // =====================================================================================================================

    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {
        log.info("Initialising booking for hotel: {}, room: {}, check-in: {}, check-out: {}, rooms: {}",
                bookingRequest.getHotelId(), bookingRequest.getRoomId(), bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id: " + bookingRequest.getHotelId()));
        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with this id: " + bookingRequest.getRoomId()));
        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(room.getId(),
                bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        validateCountOfRows(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), inventoryList.size());

        BigDecimal priceForOneRoom = pricingService.calculateTotalPrice(inventoryList);
        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        // Reserve the room/ update the booked count of inventories
        int modifiedRowsCount = inventoryRepository.initBooking(room.getId(), bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        validateCountOfRows(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), modifiedRowsCount);
        // create the booking :
        Booking booking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(appUserService.getCurrentUserFromSecurityContext())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalPrice)
                .bookingStatus(BookingStatus.RESERVED)
                .build();
        return modelMapper.map(bookingRepository.save(booking), BookingDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public BookingDto addGuests(Long bookingId, Set<Long> guestIdSet) {
        log.info("Adding guests to booking: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with this id: " + bookingId));
        validateStatusTransition(booking.getBookingStatus(), BookingStatus.GUESTS_ADDED);
        int maxGuests = booking.getRoomsCount() * booking.getRoom().getCapacity();
        if (guestIdSet.size() > maxGuests) {
            throw new IllegalArgumentException("Too many guests for " + booking.getRoomsCount() + " rooms");
        }
        if(hasBookingExpired(booking))
            throw new IllegalStateException("Booking has expired");
        AppUser user = appUserService.getCurrentUserFromSecurityContext();
        if(!user.equals(booking.getUser()))
            throw new UnauthorizedException("Booking does not belong to user with id: " + user.getId());
        for (Long guestId: guestIdSet) {
            Guest guest = guestRepository.findById(guestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + guestId));
            if (!guest.getUser().equals(user)) {
                throw new UnauthorizedException("Guest " + guestId + " doesn't belong to you");
            }
            booking.getGuests().add(guest);
        }
        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        return modelMapper.map(bookingRepository.save(booking), BookingDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public String initiatePayment(Long bookingId) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found with id : " + bookingId));
        validateStatusTransition(booking.getBookingStatus(), BookingStatus.PAYMENT_PENDING);
        if(hasBookingExpired(booking))
            throw new IllegalStateException("Booking has expired");
        AppUser user = appUserService.getCurrentUserFromSecurityContext();
        if(!user.equals(booking.getUser()))
            throw new UnauthorizedException("Booking does not belong to user with id: " + user.getId());
        booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);
        return checkoutService.getCheckoutSession(booking, frontendUrl + "/payments/success", frontendUrl + "/payments/failure");
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public void capturePayment(Event event) {
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) return;

            String sessionId = session.getId();
            Booking booking =
                    bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(() ->
                            new ResourceNotFoundException("Booking not found for session ID: "+sessionId));
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                log.info("Booking {} already confirmed, skipping duplicate webhook", booking.getId());
                return;
            }
           validateStatusTransition(booking.getBookingStatus(), BookingStatus.CONFIRMED);
            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
            int modifiedRowsCount = inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
            validateCountOfRows(booking.getCheckInDate(), booking.getCheckOutDate(), modifiedRowsCount);
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("Successfully confirmed the booking for Booking ID: {}", booking.getId());
        } else {
            log.warn("Unhandled event type: {}", event.getType());
        }
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public void cancelBooking(Long bookingId, boolean isCancelledByUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        BookingStatus targetStatus = isCancelledByUser
                ? BookingStatus.CANCELLED_BY_USER
                : BookingStatus.CANCELLED_BY_HOTEL_MANAGER;

        // Authorization check ONLY if cancelled by user
        if (isCancelledByUser) {
            AppUser user = appUserService.getCurrentUserFromSecurityContext();
            if (!user.equals(booking.getUser())) {
                throw new UnauthorizedException("Booking does not belong to this user with id: " + user.getId());
            }
        }

        validateStatusTransition(booking.getBookingStatus(), targetStatus);

        // Release inventory
        inventoryRepository.findAndLockBookedInventory(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount()
        );

        int modifiedRowsCount = inventoryRepository.cancelBooking(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount()
        );

        validateCountOfRows(booking.getCheckInDate(), booking.getCheckOutDate(), modifiedRowsCount);

        // Update status
        booking.setBookingStatus(targetStatus);
        bookingRepository.save(booking);

        // Calculate and process refund
        processRefund(booking, isCancelledByUser);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public BookingStatus getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if (!appUser.equals(booking.getUser())) {
            throw new UnauthorizedException("Booking does not belong to this user with id: " + appUser.getId());
        }
        return booking.getBookingStatus();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public List<BookingDto> getAllBookingsByHotelId(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
                "found with ID: "+hotelId));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(hotel.getOwner()))
            throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);
        log.info("Getting all booking for the hotel with ID: {}", hotelId);
        List<Booking> bookings = bookingRepository.findByHotel(hotel);
        return bookings.stream()
                .map((element) -> modelMapper.map(element, BookingDto.class))
                .toList();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Scheduled(fixedRate = 300000)
    @Transactional
    @Override
    public void cleanupExpiredBookings() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(30);
        List<Booking> expiredBookings = bookingRepository
                .findByBookingStatusInAndCreatedAtBefore(
                        List.of(BookingStatus.RESERVED, BookingStatus.GUESTS_ADDED, BookingStatus.PAYMENT_PENDING),
                        expiryThreshold
                );

        for (Booking booking : expiredBookings) {
            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
            int modifiedRows = inventoryRepository.cancelReservation(booking.getRoom().getId(),
                    booking.getCheckInDate(), booking.getCheckOutDate(), booking.getRoomsCount());
            try {
                validateCountOfRows(booking.getCheckInDate(), booking.getCheckOutDate(), modifiedRows);
                booking.setBookingStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);
            } catch (IllegalStateException e) {
                log.error("Failed to release inventory for expired booking {}. Expected rows mismatch.", booking.getId(), e);
            } catch (Exception e) {
                log.error("Unexpected error cleaning up booking {}", booking.getId(), e);
            }
        }
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not " +
                "found with ID: "+hotelId));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(hotel.getOwner()))
            throw new AccessDeniedException("You are not the owner of hotel with id: "+hotelId);
        log.info("Generating report for hotel with id: {}", hotelId);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);
        long totalConfirmedBookings = 0L;
        BigDecimal totalRevenueOfConfirmedBookings = BigDecimal.ZERO;
        for(Booking booking: bookings){
            if(booking.getBookingStatus() != BookingStatus.CONFIRMED) continue;
            totalConfirmedBookings++;
            totalRevenueOfConfirmedBookings = totalRevenueOfConfirmedBookings.add(booking.getAmount());
        }
        BigDecimal avgRevenue = totalConfirmedBookings == 0
                ? BigDecimal.ZERO :
                totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), RoundingMode.HALF_UP);
        return new HotelReportDto(totalConfirmedBookings, totalRevenueOfConfirmedBookings, avgRevenue);

    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public List<BookingDto> getMyBookings() {
        AppUser currentUser = appUserService.getCurrentUserFromSecurityContext();
        return bookingRepository.getByUser(currentUser)
                .stream()
                .map(booking -> modelMapper.map(booking, BookingDto.class))
                .toList();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public void removeGuests(Long bookingId, Set<Long> guestIdList) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("booking not found with id: " + bookingId));
        AppUser currentUser = appUserService.getCurrentUserFromSecurityContext();
        if(!currentUser.equals(booking.getUser()))
            throw new UnauthorizedException("You are not authorized to remove a guest from a booking that you have not created");
        BookingStatus currentStatus = booking.getBookingStatus();
        if (currentStatus != BookingStatus.RESERVED &&
                currentStatus != BookingStatus.GUESTS_ADDED) {
            throw new IllegalStateException("Can only modify guests in RESERVED or GUESTS_ADDED status");
        }
        Set<Guest> guestsToBeRemoved = new HashSet<>(guestRepository.findAllById(guestIdList));
        if (guestsToBeRemoved.size() != guestIdList.size()) {
            throw new EntityNotFoundException("Some guests were not found");
        }
        Set<Guest> bookingGuests = booking.getGuests();
        for(Guest guestToBeRemoved: guestsToBeRemoved){
            if (!guestToBeRemoved.getUser().equals(currentUser)) {
                throw new UnauthorizedException("Guest " + guestToBeRemoved.getId() + " doesn't belong to you");
            }
            if(!bookingGuests.contains(guestToBeRemoved))
                throw new IllegalStateException("Guest with id : " + guestToBeRemoved.getId() + " is not a part of this booking");
            bookingGuests.remove(guestToBeRemoved);
        }
    }

    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(30).isBefore(LocalDateTime.now());
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    private void processRefund(Booking booking, boolean isCancelledByUser) {
        try {
            Session session = Session.retrieve(booking.getPaymentSessionId());
            if (session.getPaymentIntent() == null) {
                log.warn("No payment intent for booking {}. Skipping refund.", booking.getId());
                return;
            }
            BigDecimal refundAmount = calculateRefundAmount(booking, isCancelledByUser);
            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("No refund due for booking {}. Amount: {}", booking.getId(), refundAmount);
                return;
            }
            // Stripe requires amount in paise
            long refundAmountInCents = refundAmount.multiply(BigDecimal.valueOf(100)).longValue();
            RefundCreateParams.Builder refundBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .setAmount(refundAmountInCents);

            // Set reason and metadata
            if (isCancelledByUser) {
                refundBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
                refundBuilder.putMetadata("cancelled_by", "user");
            }
            else {
                refundBuilder.putMetadata("cancelled_by", "hotel_manager");
                refundBuilder.putMetadata("reason", "hotel_closed_inventory");
            }
            Refund refund = Refund.create(refundBuilder.build());
            log.info("Refund created for booking {}. Refund ID: {}, Amount: {} INR, Cancelled by: {}",
                    booking.getId(),
                    refund.getId(),
                    refundAmount,
                    isCancelledByUser ? "USER" : "HOTEL_MANAGER");

        } catch (StripeException e) {
            log.error("Failed to process refund for booking {}. Manual intervention required.",
                    booking.getId(), e);
            // Don't throw - booking is already cancelled
        }
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    private BigDecimal calculateRefundAmount(Booking booking, boolean isCancelledByUser) {
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate checkOutDate = booking.getCheckOutDate();
        BigDecimal totalAmount = booking.getAmount();
        // Calculate total nights
        long totalNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (totalNights <= 0) {
            log.warn("Invalid booking dates for booking {}", booking.getId());
            return BigDecimal.ZERO;
        }
        BigDecimal costPerDay = totalAmount.divide(BigDecimal.valueOf(totalNights), 2, RoundingMode.HALF_UP);

        // Case 1: Cancellation BEFORE check-in
        if (today.isBefore(checkInDate)) {
            if (isCancelledByUser) {
                // User cancellation before check-in
                long daysUntilCheckIn = ChronoUnit.DAYS.between(today, checkInDate);

                if (daysUntilCheckIn >= 7) {
                    // Full refund if cancelled 7+ days before
                    log.info("Full refund: Cancelled {} days before check-in", daysUntilCheckIn);
                    return totalAmount;
                } else if (daysUntilCheckIn >= 3) {
                    // 50% refund if cancelled 3-6 days before
                    BigDecimal refund = totalAmount.multiply(BigDecimal.valueOf(0.5));
                    log.info("50% refund: Cancelled {} days before check-in", daysUntilCheckIn);
                    return refund;
                } else {
                    // No refund if cancelled less than 3 days before
                    log.info("No refund: Cancelled {} days before check-in", daysUntilCheckIn);
                    return BigDecimal.ZERO;
                }
            } else {
                // Hotel manager cancellation - always full refund
                log.info("Full refund: Cancelled by hotel manager before check-in");
                return totalAmount;
            }
        }
        // Case 2: Cancellation DURING the stay
        if (!today.isBefore(checkInDate) && today.isBefore(checkOutDate)) {
            // Calculate days passed (including today as a passed day)
            long nightsPassed = ChronoUnit.DAYS.between(checkInDate, today) + 1; // charging intentionally for the next night as well, to make things consistent, and so that new user can check in the next morning
            long remainingNights = totalNights - nightsPassed;
            if (isCancelledByUser) {
                // User cancellation during stay - NO REFUND
                log.info("No refund: User cancelled during stay. Days passed: {}, Remaining: {}",
                        nightsPassed, remainingNights);
                return BigDecimal.ZERO;
            } else {
                // Hotel manager cancellation - refund for remaining days
                BigDecimal refund = costPerDay.multiply(BigDecimal.valueOf(remainingNights));
                log.info("Partial refund: HM cancelled during stay. Days passed: {}, Remaining: {}, Refund: {}",
                        nightsPassed, remainingNights, refund);
                return refund;
            }
        }

        // Case 3: Cancellation AFTER checkout
        if (!today.isBefore(checkOutDate)) {
            // Stay completed - no refund for anyone
            log.info("No refund: Cancellation after checkout");
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    private void validateStatusTransition(BookingStatus from, BookingStatus to) {
        boolean valid = switch (from) {
            case RESERVED -> to == BookingStatus.GUESTS_ADDED
                    || to == BookingStatus.EXPIRED
                    || to == BookingStatus.CANCELLED_BY_HOTEL_MANAGER;

            case GUESTS_ADDED -> to == BookingStatus.PAYMENT_PENDING
                    || to == BookingStatus.EXPIRED
                    || to == BookingStatus.CANCELLED_BY_HOTEL_MANAGER;

            case PAYMENT_PENDING -> to == BookingStatus.CONFIRMED
                    || to == BookingStatus.EXPIRED
                    || to == BookingStatus.CANCELLED_BY_HOTEL_MANAGER;

            case CONFIRMED -> to == BookingStatus.CANCELLED_BY_USER
                    || to == BookingStatus.CANCELLED_BY_HOTEL_MANAGER;

            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Invalid status transition from " + from + " to " + to);
        }
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    private void validateCountOfRows(LocalDate checkInDate, LocalDate checkOutDate, int modifiedRows) {
        long expectedNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (expectedNights < 1) {
            throw new IllegalArgumentException(
                    "Check-out date must be at least 1 day after check-in date"
            );
        }
        // Validate row count matches
        if (expectedNights != modifiedRows) {
            throw new IllegalStateException(
                    "Inventory update failed. Expected: " + expectedNights + " nights, " +
                            "but updated: " + modifiedRows + " rows. " +
                            "Check-in: " + checkInDate + ", Check-out: " + checkOutDate
            );
        }
    }

    // =====================================================================================================================
}
