package com.nikhil.airbnb.service;

import com.nikhil.airbnb.dto.HotelDto;

import java.util.List;

public interface HotelService {

    HotelDto createNewHotel(HotelDto hotelDto);
    HotelDto getHotelById(Long id);
    List<HotelDto> getAllHotels();
    HotelDto updateHotelById(Long id,HotelDto hotelDto);
    void deleteHotelById(Long id);
    void activateHotel(Long id);
}
