package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.dto.HotelDto;
import com.nikhil.airbnb.dto.HotelInfoDto;

import java.util.List;

public interface HotelService {

    HotelDto createNewHotel(HotelDto hotelDto);
    HotelDto getHotelById(Long id);
    List<HotelDto> getAllHotels();
    HotelDto updateHotelById(Long id,HotelDto hotelDto);
    void deleteHotelById(Long id);
    void activateHotel(Long id);
    HotelInfoDto getHotelInfoById(Long hotelId);
}
