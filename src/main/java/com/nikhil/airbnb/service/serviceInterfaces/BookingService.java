package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.dto.BookingDto;
import com.nikhil.airbnb.dto.BookingRequest;
import com.nikhil.airbnb.dto.GuestDto;

import java.util.List;

public interface BookingService {

    BookingDto initialiseBooking(BookingRequest bookingRequest);
    BookingDto addGuests(Long bookingId, List<GuestDto> guestDtos);
}
