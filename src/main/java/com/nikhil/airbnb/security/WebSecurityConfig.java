package com.nikhil.airbnb.security;

import com.nikhil.airbnb.handler.OAuth2SuccessHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSecurityConfig {
    // =====================================================================================================================
     JWTAuthFilter jwtAuthFilter;
     HandlerExceptionResolver handlerExceptionResolver;
     OAuth2SuccessHandler oAuth2SuccessHandler;
     // =====================================================================================================================

     @Bean
     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

         http
                 .csrf(AbstractHttpConfigurer::disable)
                 .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                 .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                 .authorizeHttpRequests(authorize -> authorize
                         .requestMatchers("/admin/**").hasRole("HOTEL_MANAGER")
                         .requestMatchers("/bookings/**").authenticated()
                         .requestMatchers("/users/**").authenticated()
                         .anyRequest().permitAll()
                 )
                 .oauth2Login(oauth2config -> oauth2config
                                 .failureUrl("/login?error=true")
                                 .successHandler(oAuth2SuccessHandler)
                 )
                 .exceptionHandling(configurer -> configurer.accessDeniedHandler(accessDeniedHandler()));


         return http.build();
     }
     //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
         return (request, response, accessDeniedException) -> {
             handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
         };
    }

    // =====================================================================================================================

}
