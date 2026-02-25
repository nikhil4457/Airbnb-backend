package com.nikhil.airbnb.security;

import com.nikhil.airbnb.dto.LoginDto;
import com.nikhil.airbnb.dto.SignupRequestDto;
import com.nikhil.airbnb.dto.UserDto;
import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.enums.Role;
import com.nikhil.airbnb.repository.AppUserRepository;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService {
    // =====================================================================================================================
    AppUserRepository appUserRepository;
    PasswordEncoder passwordEncoder;
    AuthenticationManager authenticationManager;
    AppUserService appUserService;
    JWTService jwtService;
    ModelMapper modelMapper;
    // =====================================================================================================================

    public UserDto signup(SignupRequestDto signupRequestDto) {
        if(appUserRepository.existsByEmail(signupRequestDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        AppUser appUser = modelMapper.map(signupRequestDto, AppUser.class);
        appUser.setRoles(Set.of(Role.GUEST));
        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
        AppUser savedUser = appUserRepository.save(appUser);
        return modelMapper.map(savedUser, UserDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    public String[] login(LoginDto loginDto){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
        );
        AppUser appUser = (AppUser) authentication.getPrincipal();
        return new String[]{jwtService.generateAccessToken(appUser), jwtService.generateRefreshToken(appUser)};
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    public String refreshToken(String refreshToken){
        Long id = jwtService.getUserIdFromToken(refreshToken);
        AppUser appUser = appUserService.getUserById(id);
        return jwtService.generateAccessToken(appUser);
    }

    // =====================================================================================================================
}
