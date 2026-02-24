package com.nikhil.airbnb.security;

import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JWTAuthFilter extends OncePerRequestFilter {
    // =====================================================================================================================
    JWTService jwtService;
    AppUserService appUserService;
    HandlerExceptionResolver handlerExceptionResolver;
    // =====================================================================================================================

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try{
            final String requestTokenHeader = request.getHeader("Authorization");
            if(requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = requestTokenHeader.substring(7);
            Long userId = jwtService.getUserIdFromToken(token);

            if(userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                AppUser user = appUserService.getUserById(userId);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        }
        catch (Exception e){
            handlerExceptionResolver.resolveException(request, response, null, e);
        }

    }
    // =====================================================================================================================
}
