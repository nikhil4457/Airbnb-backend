package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.GuestDto;
import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.Guest;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.exception.UnauthorizedException;
import com.nikhil.airbnb.repository.BookingRepository;
import com.nikhil.airbnb.repository.GuestRepository;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import com.nikhil.airbnb.service.serviceInterfaces.GuestService;
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
public class GuestServiceImpl implements GuestService {
    // =====================================================================================================================
    GuestRepository guestRepository;
    AppUserService appUserService;
    BookingRepository bookingRepository;
    ModelMapper modelMapper;
    // =====================================================================================================================

    @Override
    public GuestDto createGuest(GuestDto guestDto) {
        AppUser currentUser = appUserService.getCurrentUserFromSecurityContext();
        Guest guest = modelMapper.map(guestDto, Guest.class);
        guest.setId(null);
        guest.setUser(currentUser);
        return modelMapper.map(guestRepository.save(guest), GuestDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public void deleteGuest(Long guestId) {
        Guest guest = guestRepository.findById(guestId).orElseThrow(() -> new ResourceNotFoundException("Guest not found with id : " + guestId));
        AppUser currentUser = appUserService.getCurrentUserFromSecurityContext();
        if(!currentUser.equals(guest.getUser()))
            throw new UnauthorizedException("Your are not authorized to delete guests that you dont own");
        if(bookingRepository.existsByBookingStatusInAndGuestsContaining(
                List.of(BookingStatus.RESERVED,
                BookingStatus.GUESTS_ADDED,
                BookingStatus.PAYMENT_PENDING,
                BookingStatus.CONFIRMED), guest))
            throw new IllegalStateException("Cannot delete guests that are already part of a booking for which the payment procedure has started");
        guestRepository.delete(guest);
    }

    // =====================================================================================================================
}
