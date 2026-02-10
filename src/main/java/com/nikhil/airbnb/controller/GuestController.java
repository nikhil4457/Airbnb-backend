package com.nikhil.airbnb.controller;


import com.nikhil.airbnb.dto.GuestDto;
import com.nikhil.airbnb.service.serviceInterfaces.GuestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequestMapping("/guests")
public class GuestController {
    // =====================================================================================================================
    GuestService guestService;
    // =====================================================================================================================

    @PostMapping("/createGuest")
    public ResponseEntity<GuestDto> createGuest(@RequestBody GuestDto guestDto){
        return ResponseEntity.ok(guestService.createGuest(guestDto));
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @DeleteMapping("/deleteGuest/{guestId}")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long guestId){
        guestService.deleteGuest(guestId);
        return ResponseEntity.noContent().build();
    }

    // =====================================================================================================================
}
