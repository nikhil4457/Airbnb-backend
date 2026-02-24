package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.dto.*;
import com.nikhil.airbnb.entity.Room;

import java.util.List;

public interface InventoryService {

    void initializeRoomsForAYear(Room room);
    void deleteByRoom(Room room);
    CachedPageDto<HotelPriceDto> searchHotels(HotelSearchRequest request);
    List<InventoryDto> getAllInventoryByRoom(Long roomId);
    void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto);
}
