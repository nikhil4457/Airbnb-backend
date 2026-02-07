package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.HotelPriceDto;
import com.nikhil.airbnb.dto.HotelSearchRequest;
import com.nikhil.airbnb.entity.Booking;
import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.entity.Room;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import com.nikhil.airbnb.repository.BookingRepository;
import com.nikhil.airbnb.repository.HotelMinPriceRepository;
import com.nikhil.airbnb.repository.InventoryRepository;
import com.nikhil.airbnb.service.serviceInterfaces.BookingService;
import com.nikhil.airbnb.service.serviceInterfaces.InventoryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    // =====================================================================================================================
    InventoryRepository inventoryRepository;
    HotelMinPriceRepository hotelMinPriceRepository;
    BookingRepository bookingRepository;
    BookingService bookingService;
    // =====================================================================================================================

    @Override
    @Transactional
    public void initializeRoomsForAYear(Room room) {
        log.info("Initializing inventory for room: {}", room.getId());
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        List<Inventory> inventoriesToBeSaved = new ArrayList<>();
        for(; !today.isAfter(endDate); today = today.plusDays(1)){
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoriesToBeSaved.add(inventory);
        }
        inventoryRepository.saveAll(inventoriesToBeSaved);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public void deleteByRoom(Room room) {
        log.info("Deleting inventory for room: {}", room.getId());
        inventoryRepository.deleteByRoom(room);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequest request) {
        log.info("Searching hotels for city: {}, start date: {}, end date: {}, rooms count: {}, page: {}, size: {}", request.getCity(), request.getStartDate(), request.getEndDate(), request.getRoomsCount(), request.getPage(), request.getSize());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return hotelMinPriceRepository.findHotelsWithAvailableInventory(
                request.getCity(),
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Transactional
    public void closeInventory(Long roomId, LocalDate startDate, LocalDate endDate, String reason) {
        List<Booking> affectedBookings = new ArrayList<>();
        List<Booking> reservedBookings = bookingRepository
                .findAllIntersectingWithDateRangeAndStatusIn(
                        roomId,
                        startDate,
                        endDate,
                        List.of(BookingStatus.RESERVED, BookingStatus.GUESTS_ADDED, BookingStatus.PAYMENT_PENDING)
                );
        List<Booking> confirmedBookings = bookingRepository
                .findAllIntersectingWithDateRangeAndStatusIn(
                        roomId,
                        startDate,
                        endDate,
                        List.of(BookingStatus.CONFIRMED)
                );
        // for all those bookings, we now decrement all inventories associated with those bookings
        // ( inventories that lie between the checkin and checkout date of this booking ) and decrement their reserved count
        for (Booking booking : reservedBookings) {
            inventoryRepository.findAndLockReservedInventory(
                    booking.getRoom().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getRoomsCount()
            );
            inventoryRepository.cancelReservation(
                    booking.getRoom().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getRoomsCount()
            );
            // for all these bookings we change their status
            booking.setBookingStatus(BookingStatus.CANCELLED_BY_HOTEL_MANAGER);
            // add these booking to the affected bookings list
            affectedBookings.add(booking);
            log.info("Cancelled reserved booking {} due to inventory closure", booking.getId());
        }


        // for each booking we cancel that booking using BookingService's cancel method ( which also internally calls stripe's refund mechanism )
        for (Booking booking : confirmedBookings) {
            bookingService.cancelBooking(booking.getId(), false);  // false = cancelled by HM
            affectedBookings.add(booking);
        }

        inventoryRepository.updateInventory(roomId, startDate, endDate, true, BigDecimal.ONE); // default);
        bookingRepository.saveAll(affectedBookings);

//        // TODO :  Notify affected customers
//        if (!affectedBookings.isEmpty()) {
//            notifyCustomersOfCancellation(affectedBookings, reason);
//        }

        log.info("Closed inventory for room {} from {} to {}. Affected {} bookings",
                roomId, startDate, endDate, affectedBookings.size());
    }

    // =====================================================================================================================
}
