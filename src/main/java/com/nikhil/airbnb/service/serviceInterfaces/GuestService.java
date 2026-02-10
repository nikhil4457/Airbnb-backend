package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.dto.GuestDto;

public interface GuestService {
    GuestDto createGuest(GuestDto guestDto);
    void deleteGuest(Long guestId);
}
