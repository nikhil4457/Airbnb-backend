package com.nikhil.airbnb.repository;

import com.nikhil.airbnb.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}