package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.RoomDto;
import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.Hotel;
import com.nikhil.airbnb.entity.Room;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.exception.UnauthorizedException;
import com.nikhil.airbnb.repository.HotelRepository;
import com.nikhil.airbnb.repository.RoomRepository;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import com.nikhil.airbnb.service.serviceInterfaces.InventoryService;
import com.nikhil.airbnb.service.serviceInterfaces.RoomService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoomServiceImpl implements RoomService {
    // =====================================================================================================================
    RoomRepository roomRepository;
    HotelRepository hotelRepository;
    InventoryService inventoryService;
    AppUserService appUserService;
    PricingUpdateService pricingUpdateService;
    ModelMapper modelMapper;
    // =====================================================================================================================

    @Override
    @Transactional
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id : " + hotelId));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(hotel.getOwner()))
            throw new UnauthorizedException("Hotel does not belong to user with id: " + appUser.getId());
        Room room = modelMapper.map(roomDto, Room.class);
        room.setHotel(hotel);
        room = roomRepository.save(room);
        if(hotel.getActive()){
            inventoryService.initializeRoomsForAYear(room);
        }

        return modelMapper.map(room, RoomDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id : " + hotelId));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(hotel.getOwner()))
            throw new UnauthorizedException("Hotel does not belong to user with id: " + appUser.getId());
        return hotel.getRooms()
                .stream()
                .map(room -> modelMapper.map(room, RoomDto.class))
                .toList();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public RoomDto getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found with id:  " + roomId));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(room.getHotel().getOwner()))
            throw new UnauthorizedException("Room does not belong to user with id: " + appUser.getId());
        return modelMapper.map(room, RoomDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    @Transactional
    public void deleteRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found with id:  " + roomId));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(room.getHotel().getOwner()))
            throw new UnauthorizedException("Room does not belong to user with id: " + appUser.getId());
        inventoryService.deleteByRoom(room);
        roomRepository.delete(room);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Transactional
    @Override
    public RoomDto updateRoomById(Long hotelId, Long roomId, RoomDto roomDto) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with this id"));
        AppUser appUser = appUserService.getCurrentUserFromSecurityContext();
        if(!appUser.equals(hotel.getOwner()))
            throw new UnauthorizedException("Hotel does not belong to user with id: " + appUser.getId());
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id : " + roomId));
        BigDecimal oldPrice = room.getBasePrice();
        modelMapper.map(roomDto, room);
        room.setId(roomId);
        // not relying on cron job because Hotel manager would want to see the change immediately
        if(!room.getBasePrice().equals(oldPrice))
            pricingUpdateService.updateRoomPrice(room);
        return modelMapper.map(roomRepository.save(room), RoomDto.class);
    }

    // =====================================================================================================================
}
