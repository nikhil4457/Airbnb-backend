package com.nikhil.airbnb.service;


import com.nikhil.airbnb.dto.BookingDto;
import com.nikhil.airbnb.dto.BookingRequest;
import com.nikhil.airbnb.dto.GuestDto;
import com.nikhil.airbnb.entity.*;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import com.nikhil.airbnb.entity.enums.Role;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingServiceImpl implements BookingService {
    BookingRepository bookingRepository;
    HotelRepository hotelRepository;
    RoomRepository roomRepository;
    InventoryRepository inventoryRepository;
    ModelMapper modelMapper;
    UserRepository userRepository;


    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {
        log.info("Initialising booking for hotel: {}, room: {}, check-in: {}, check-out: {}, rooms: {}", bookingRequest.getHotelId(), bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id: " + bookingRequest.getHotelId()));
        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with this id: " + bookingRequest.getRoomId()));
        List<Inventory> inventories = inventoryRepository.findAndLockAvailableInventories(
                room.getId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount()
        );
        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate()) + 1;
        if(inventories.size() != daysCount){
            throw new IllegalStateException("Not enough rooms available for the selected dates");
        }
        // Reserve the rooms by updating the booked count
        for(Inventory inventory : inventories){
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventories);
        // create the booking :
        // dummy user since we havent implemented spring security yet


        // TODO : Calculate Dynamic pricing based on surge factor and other parameters

        Booking booking = Booking.builder()
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN)
                .bookingStatus(BookingStatus.RESERVED)
                .build();
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    @Override
    public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtos) {
        log.info("Adding guests to booking: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with this id: " + bookingId));
        if(booking.getBookingStatus() != BookingStatus.RESERVED)
            throw new IllegalStateException("Cannot add guests to booking with status: " + booking.getBookingStatus());
        if(hasBookingExpired(booking))
            throw new IllegalStateException("Booking has expired");
        for(GuestDto guestDto : guestDtos){
            Guest guest = modelMapper.map(guestDto, Guest.class);
            guest.setUser(getCurrentUser());
            booking.getGuests().add(guest);
        }
        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(30).isBefore(LocalDateTime.now());
    }
    private User getCurrentUser() {
        return userRepository.findByEmail("dummy@test.com")
                .orElseGet(() -> {
                    User dummyUser = new User();
                    dummyUser.setEmail("dummy@test.com");
                    dummyUser.setPassword("password123"); // In production, hash this!
                    dummyUser.setName("Dummy User");
                    dummyUser.setRoles(Set.of(Role.GUEST));
                    return userRepository.save(dummyUser);
                });
    }

}
