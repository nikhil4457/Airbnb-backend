package com.nikhil.airbnb.controller;


import com.nikhil.airbnb.dto.HotelInfoDto;
import com.nikhil.airbnb.dto.HotelPriceDto;
import com.nikhil.airbnb.dto.HotelSearchRequest;
import com.nikhil.airbnb.service.serviceInterfaces.HotelService;
import com.nikhil.airbnb.service.serviceInterfaces.InventoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequestMapping("/hotels")
public class HotelBrowseController {
    // =====================================================================================================================
    InventoryService inventoryService;
    HotelService hotelService;
    // =====================================================================================================================

    @GetMapping("/search")
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@RequestBody HotelSearchRequest request){
        Page<HotelPriceDto> page = inventoryService.searchHotels(request);
        return ResponseEntity.ok(page);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId){
        return ResponseEntity.ok(hotelService.getHotelInfoById(hotelId));
    }

    // =====================================================================================================================


}
