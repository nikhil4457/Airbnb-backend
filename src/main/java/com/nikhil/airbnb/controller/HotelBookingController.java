package com.nikhil.airbnb.controller;

import com.nikhil.airbnb.dto.BookingDto;
import com.nikhil.airbnb.dto.BookingRequest;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import com.nikhil.airbnb.service.serviceInterfaces.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequestMapping("/bookings")
public class HotelBookingController {
    // =====================================================================================================================
    BookingService bookingService;
    // =====================================================================================================================

    @PostMapping("/init")
    public ResponseEntity<BookingDto> initialiseBooking(@RequestBody BookingRequest bookingRequest){
        return ResponseEntity.ok(bookingService.initialiseBooking(bookingRequest));
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @PostMapping("/{bookingId}/addGuests")
    public ResponseEntity<BookingDto> addGuests(@PathVariable Long bookingId,
                                                @RequestBody Set<Long> guestIdList) {
        return ResponseEntity.ok(bookingService.addGuests(bookingId, guestIdList));
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @DeleteMapping("/{bookingId}/removeGuests")
    public ResponseEntity<Void> removeGuests(@PathVariable Long bookingId,
                                             @RequestBody Set<Long> guestLists){
        bookingService.removeGuests(bookingId, guestLists);
        return ResponseEntity.noContent().build();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable Long bookingId){
        String sessionUrl = bookingService.initiatePayment(bookingId);
        return ResponseEntity.ok(Map.of("sessionUrl", sessionUrl));
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @GetMapping("/{bookingId}/status")
    public ResponseEntity<Map<String, String>> getBookingStatus(@PathVariable Long bookingId){
        BookingStatus bookingStatus = bookingService.getBookingStatus(bookingId);
        return ResponseEntity.ok(Map.of("bookingStatus", bookingStatus.name()));
    }

    // =====================================================================================================================
}
