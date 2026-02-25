package com.nikhil.airbnb.handler;

import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.enums.Role;
import com.nikhil.airbnb.security.JWTService;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;


@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    // =====================================================================================================================
    JWTService jwtService;
    AppUserService appUserService;

    @Value("${frontend.url}")
    @NonFinal String frontendUrl;
    // =====================================================================================================================

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        DefaultOAuth2User defaultOAuth2User = (DefaultOAuth2User) oAuth2AuthenticationToken.getPrincipal();
        log.info("OAuth2 user: {}", defaultOAuth2User);
        String email = defaultOAuth2User.getAttribute("email");
        String name = defaultOAuth2User.getAttribute("name");
        log.info("Email: {}", email);
        log.info("Name: {}", name);
        AppUser appUser = appUserService.getUserByEmail(email);
        if(appUser == null) {
            AppUser newUser = AppUser.builder()
                    .email(email)
                    .roles(Set.of(Role.GUEST))
                    .name(name == null ? email : name)
                    .build();
            appUser = appUserService.saveUser(newUser);
            log.info("New user created: {}", appUser);
        }

        String accessToken = jwtService.generateAccessToken(appUser);
        String refreshToken = jwtService.generateRefreshToken(appUser);

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "?accessToken=" + accessToken);
    }
}
