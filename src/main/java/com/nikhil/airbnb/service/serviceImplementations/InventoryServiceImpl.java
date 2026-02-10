package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.HotelPriceDto;
import com.nikhil.airbnb.dto.HotelSearchRequest;
import com.nikhil.airbnb.dto.InventoryDto;
import com.nikhil.airbnb.dto.UpdateInventoryRequestDto;
import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.Booking;
import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.entity.Room;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.exception.UnauthorizedException;
import com.nikhil.airbnb.repository.BookingRepository;
import com.nikhil.airbnb.repository.HotelMinPriceRepository;
import com.nikhil.airbnb.repository.InventoryRepository;
import com.nikhil.airbnb.repository.RoomRepository;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import com.nikhil.airbnb.service.serviceInterfaces.BookingService;
import com.nikhil.airbnb.service.serviceInterfaces.InventoryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
    RoomRepository roomRepository;
    AppUserService appUserService;
    EmailService emailService;
    ModelMapper modelMapper;
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
        log.info("Searching hotels for city: {}, start date: {}, end date: {}, rooms count: {}, page: {}, size: {}",
                request.getCity(), request.getStartDate(), request.getEndDate(), request.getRoomsCount(), request.getPage(), request.getSize());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return hotelMinPriceRepository.findHotelsWithAvailableInventory(
                request.getCity(),
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public List<InventoryDto> getAllInventoryByRoom(Long roomId) {
        log.info("Getting all inventories for room with id: {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(room.getHotel().getOwner()))
            throw new UnauthorizedException("You are not the owner of room with id : " + roomId);
        return inventoryRepository.findByRoomOrderByDate(room)
                .stream()
                .map(inventory -> modelMapper.map(inventory, InventoryDto.class))
                .toList();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto) {
        log.info("Updating all inventories for room with id: {} between date range : {} - {} ",
                roomId, updateInventoryRequestDto.getStartDate(), updateInventoryRequestDto.getEndDate());
        Boolean newClosedStatus = updateInventoryRequestDto.getClosed();
        BigDecimal newSurgeFactor = updateInventoryRequestDto.getSurgeFactor();
        boolean updateClosedStatus = newClosedStatus != null;
        boolean updateSurgeFactor = newSurgeFactor != null;
        if(!updateClosedStatus && !updateSurgeFactor){
            throw new IllegalArgumentException("SurgeFactor and closed cannot be null simultaneously in a update request");
        }
        LocalDate updateStartDate = updateInventoryRequestDto.getStartDate();
        LocalDate updateEndDate = updateInventoryRequestDto.getEndDate();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(room.getHotel().getOwner()))
            throw new UnauthorizedException("You are not the owner of room with id : " + roomId);
        inventoryRepository.lockInventoryBeforeUpdate(roomId,
                updateStartDate,
                updateEndDate);
        if(updateClosedStatus && newClosedStatus) // close inventories
            closeInventory(roomId, updateStartDate, updateEndDate, newSurgeFactor, updateSurgeFactor);
        else
             inventoryRepository.updateInventory(
                     roomId,
                     updateStartDate,
                     updateEndDate,
                     newClosedStatus,
                     newSurgeFactor,
                     updateClosedStatus,
                     updateSurgeFactor
             );

    }

    /*-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    -x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-*/
    private void closeInventory(Long roomId, LocalDate startDate, LocalDate endDate, BigDecimal newSurgeFactor, boolean updateSurgeFactor) {
        // assuming the inventory is already locked by updateInventory method
        // WE CAN SAFELY ASSUME this method will only be called by updateInventory()
//        inventoryRepository.lockInventoryBeforeUpdate(roomId, startDate, endDate); // not needed
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
        List<Booking> toSaveBookings = new ArrayList<>();
        List<Booking> allAffectedBookings = new ArrayList<>();
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
            toSaveBookings.add(booking);
            allAffectedBookings.add(booking);
            log.info("Cancelled reserved booking {} due to inventory closure", booking.getId());
        }


        // for each booking we cancel that booking using BookingService's cancel method ( which also internally calls stripe's refund mechanism )
        for (Booking booking : confirmedBookings) {
            bookingService.cancelBooking(booking.getId(), false);  // false = canceled by HM
            allAffectedBookings.add(booking);
        }

        inventoryRepository.updateInventory(roomId,
                startDate,
                endDate,
                true,
                newSurgeFactor,
                true,
                updateSurgeFactor
                );
        bookingRepository.saveAll(toSaveBookings);

        for (Booking booking : allAffectedBookings) {
            emailService.sendBookingCancellationEmail(booking);
        }

        int totalAffected = reservedBookings.size() + confirmedBookings.size();
        log.info("Closed inventory for room {} from {} to {}. Affected {} bookings ({} reserved, {} confirmed)",
                roomId, startDate, endDate, totalAffected, reservedBookings.size(), confirmedBookings.size());
    }

    // =====================================================================================================================
}
