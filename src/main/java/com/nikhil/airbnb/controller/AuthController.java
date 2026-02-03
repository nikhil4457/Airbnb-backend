package com.nikhil.airbnb.controller;


import com.nikhil.airbnb.dto.LoginDto;
import com.nikhil.airbnb.dto.LoginResponseDto;
import com.nikhil.airbnb.dto.SignupRequestDto;
import com.nikhil.airbnb.dto.UserDto;
import com.nikhil.airbnb.security.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequestMapping("/auth")
public class AuthController {

    AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@RequestBody SignupRequestDto signupRequestDto) {
        return new ResponseEntity<>(authService.signup(signupRequestDto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto loginDto, HttpServletResponse response) {
        String[] tokens = authService.login(loginDto);
        Cookie cookie = new Cookie("refreshToken", tokens[1]);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ResponseEntity.ok().body(new LoginResponseDto(tokens[0])  );
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if(refreshToken == null)
            throw new AuthenticationServiceException("Refresh token is missing");
        String accessToken = authService.refreshToken(refreshToken);
        return ResponseEntity.ok().body(new LoginResponseDto(accessToken));
    }



}
