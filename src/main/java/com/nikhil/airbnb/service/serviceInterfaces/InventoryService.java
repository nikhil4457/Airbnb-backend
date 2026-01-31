package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.dto.HotelPriceDto;
import com.nikhil.airbnb.dto.HotelSearchRequest;
import com.nikhil.airbnb.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {

    void initializeRoomsForAYear(Room room);
    void deleteByRoom(Room room);
    Page<HotelPriceDto> searchHotels(HotelSearchRequest request);
}
