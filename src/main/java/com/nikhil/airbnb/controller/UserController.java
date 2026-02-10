package com.nikhil.airbnb.controller;

import com.nikhil.airbnb.dto.BookingDto;
import com.nikhil.airbnb.dto.ProfileUpdateRequestDto;
import com.nikhil.airbnb.dto.UserDto;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import com.nikhil.airbnb.service.serviceInterfaces.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequestMapping("/users")
public class UserController {
    // =====================================================================================================================
    AppUserService appUserService;
    BookingService bookingService;
    // =====================================================================================================================

    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody ProfileUpdateRequestDto profileUpdateRequestDto){
        appUserService.updateProfile(profileUpdateRequestDto);
        return ResponseEntity.noContent().build();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @GetMapping("/myBooking")
    public ResponseEntity<List<BookingDto>> getMyBookings(){
        return ResponseEntity.ok(bookingService.getMyBookings());
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @GetMapping("/getMyProfile")
    public ResponseEntity<UserDto> getMyProfile(){
        return ResponseEntity.ok(appUserService.getMyProfile());
    }

    // =====================================================================================================================
}
