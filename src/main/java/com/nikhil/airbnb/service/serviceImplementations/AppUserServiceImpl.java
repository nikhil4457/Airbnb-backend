package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.repository.AppUserRepository;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppUserServiceImpl implements AppUserService, UserDetailsService {
    // =====================================================================================================================
    AppUserRepository appUserRepository;
    // =====================================================================================================================

    @Override
    public AppUser getUserById(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser not found with this id: " + userId));
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return appUserRepository.findByEmail(email).orElse(null);
    }

    // =====================================================================================================================
}
