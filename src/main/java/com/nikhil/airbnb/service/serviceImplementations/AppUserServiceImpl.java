package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.ProfileUpdateRequestDto;
import com.nikhil.airbnb.dto.UserDto;
import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.enums.Role;
import com.nikhil.airbnb.exception.ResourceNotFoundException;
import com.nikhil.airbnb.exception.UnauthorizedException;
import com.nikhil.airbnb.repository.AppUserRepository;
import com.nikhil.airbnb.service.serviceInterfaces.AppUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
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
    ModelMapper modelMapper;
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
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public AppUser getCurrentUserFromSecurityContext(){
        if(SecurityContextHolder.getContext().getAuthentication() == null ||
                SecurityContextHolder.getContext().getAuthentication().getPrincipal() == null)
            throw new UnauthorizedException("You are not logged in ! There is not current user !");
        return (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto) {
        AppUser currentUser = getCurrentUserFromSecurityContext();
        if(profileUpdateRequestDto.getName()!=null)
            currentUser.setName(profileUpdateRequestDto.getName());
        if(profileUpdateRequestDto.getGender()!=null)
            currentUser.setGender(profileUpdateRequestDto.getGender());
        if(profileUpdateRequestDto.getDateOfBirth()!=null)
            currentUser.setDateOfBirth(profileUpdateRequestDto.getDateOfBirth());
        appUserRepository.save(currentUser);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public UserDto getMyProfile() {
        log.info("Fetching profile of logged in user ...");
        AppUser currentUser = getCurrentUserFromSecurityContext();
        return modelMapper.map(currentUser, UserDto.class);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public void processHotelMangerRequest() {
        AppUser user = getCurrentUserFromSecurityContext();
        if (user.getRoles().contains(Role.HOTEL_MANAGER)) {
            throw new IllegalStateException("Already a hotel manager");
        }

        /*-----------------------------------------------------------------------
         * PRODUCTION VERIFICATION WORKFLOW:
         *
         * In a production system, this would include:
         * 1. Email verification (confirm business email ownership)
         * 2. Phone OTP verification (SMS/WhatsApp)
         * 3. Document upload & validation:
         *    - Government-issued ID (passport/driver's license)
         *    - Business registration certificate
         *    - Tax identification number
         * 4. Background check via third-party API
         * 5. Manual admin review for high-risk applications
         * 6. Bank account verification for payout setup
         *
         * For this demo, instant approval is enabled to showcase the hotel
         * management features (room creation, inventory management, booking
         * reports) without adding verification infrastructure.
         ----------------------------------------------------------------------*/

        user.getRoles().add(Role.HOTEL_MANAGER);
        appUserRepository.save(user);
        log.info("User {} became hotel manager", user.getId());
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email).orElse(null);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public AppUser saveUser(AppUser appUser) {
        return appUserRepository.save(appUser);
    }

    // =====================================================================================================================
}
