package com.nikhil.airbnb.controller;


import com.nikhil.airbnb.dto.InventoryDto;
import com.nikhil.airbnb.dto.UpdateInventoryRequestDto;
import com.nikhil.airbnb.service.serviceInterfaces.InventoryService;
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
@RequestMapping("/admin/inventory")
public class InventoryController {
    // =====================================================================================================================
    InventoryService inventoryService;
    // =====================================================================================================================

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<List<InventoryDto>> getAllInventoryByRoom(@PathVariable Long roomId){
        return ResponseEntity.ok(inventoryService.getAllInventoryByRoom(roomId));
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @PatchMapping("/rooms/{roomId}")
    public ResponseEntity<Void> updateInventory(@PathVariable Long roomId,
                                                @RequestBody UpdateInventoryRequestDto updateInventoryRequestDto){
        inventoryService.updateInventory(roomId, updateInventoryRequestDto);
        return ResponseEntity.noContent().build();
    }

    // =====================================================================================================================
}
