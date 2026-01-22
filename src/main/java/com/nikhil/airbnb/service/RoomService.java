package com.nikhil.airbnb.service;

import com.nikhil.airbnb.dto.HotelDto;
import com.nikhil.airbnb.dto.RoomDto;
import com.nikhil.airbnb.entity.Room;

import java.util.List;

public interface RoomService {

    RoomDto createNewRoom(Long hotelId, RoomDto roomDto);
    List<RoomDto> getAllRoomsInHotel(Long hotelId);
    RoomDto getRoomById(Long roomId);
    void deleteRoomById(Long roomId);
}
