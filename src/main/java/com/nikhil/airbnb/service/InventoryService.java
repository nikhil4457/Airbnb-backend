package com.nikhil.airbnb.service;

import com.nikhil.airbnb.dto.HotelDto;
import com.nikhil.airbnb.dto.HotelSearchRequest;
import com.nikhil.airbnb.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {

    void initializeRoomsForAYear(Room room);
    void deleteByRoom(Room room);
    Page<HotelDto> searchHotels(HotelSearchRequest request);
}
