package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.entity.AppUser;

public interface AppUserService {
    AppUser getUserById(Long userId);
}
