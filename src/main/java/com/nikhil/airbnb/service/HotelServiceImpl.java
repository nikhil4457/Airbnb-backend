package com.nikhil.airbnb.service;

import com.nikhil.airbnb.dto.HotelDto;
import com.nikhil.airbnb.entity.Hotel;
import com.nikhil.airbnb.entity.Room;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.repository.HotelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService{

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
        hotel.setActive(false);
        hotel.setId(null);
        hotel = hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    @Transactional
    public HotelDto getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Hotel not found"));
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public List<HotelDto> getAllHotels() {
        return hotelRepository.findAll().stream().map(hotel -> modelMapper.map(hotel, HotelDto.class)).toList();
    }


    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id"));
        modelMapper.map(hotelDto, hotel);
        hotel.setId(id);
        return modelMapper.map(hotelRepository.save(hotel), HotelDto.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id"));
        for(Room room: hotel.getRooms()){
            inventoryService.deleteFutureInventories(room);
        }
        hotelRepository.delete(hotel);

    }

    @Override
    @Transactional
    public void activateHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id: " + id));
        hotel.setActive(true);
         // assuming you only do it once:
        for(Room room: hotel.getRooms()){
            inventoryService.initializeRoomsForAYear(room);
        }
        hotelRepository.save(hotel);
    }
}
