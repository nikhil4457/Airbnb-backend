package com.nikhil.airbnb.repository;

import com.nikhil.airbnb.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String mail);
    boolean existsByEmail(String email);
}