package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.HotelDto;
import com.nikhil.airbnb.dto.HotelInfoDto;
import com.nikhil.airbnb.dto.RoomDto;
import com.nikhil.airbnb.entity.Hotel;
import com.nikhil.airbnb.entity.Room;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.repository.HotelRepository;
import com.nikhil.airbnb.repository.RoomRepository;
import com.nikhil.airbnb.service.serviceInterfaces.HotelService;
import com.nikhil.airbnb.service.serviceInterfaces.InventoryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HotelServiceImpl implements HotelService {
    // =====================================================================================================================
    HotelRepository hotelRepository;
    InventoryService inventoryService;
    RoomRepository roomRepository;
    ModelMapper modelMapper;
    // =====================================================================================================================

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
        hotel.setActive(false);
        hotel.setId(null);
        hotel = hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public HotelDto getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Hotel not found"));
        return modelMapper.map(hotel, HotelDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public List<HotelDto> getAllHotels() {
        return hotelRepository.findAll().stream().map(hotel -> modelMapper.map(hotel, HotelDto.class)).toList();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id"));
        modelMapper.map(hotelDto, hotel);
        hotel.setId(id);
        return modelMapper.map(hotelRepository.save(hotel), HotelDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id"));
        for(Room room: hotel.getRooms()){
            inventoryService.deleteByRoom(room);
            roomRepository.delete(room);
        }
        hotelRepository.delete(hotel);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
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
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public HotelInfoDto getHotelInfoById(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id: " + hotelId));
        List<RoomDto> roomDtos = hotel.getRooms().stream()
                .map(room -> modelMapper.map(room, RoomDto.class))
                .toList();
        return new HotelInfoDto(modelMapper.map(hotel, HotelDto.class), roomDtos);
    }

    // =====================================================================================================================
}
