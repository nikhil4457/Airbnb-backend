package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.dto.ProfileUpdateRequestDto;
import com.nikhil.airbnb.dto.UserDto;
import com.nikhil.airbnb.entity.AppUser;

public interface AppUserService {
    AppUser getUserById(Long userId);
    AppUser getCurrentUserFromSecurityContext();
    void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);
    UserDto getMyProfile();
}
