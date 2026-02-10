package com.nikhil.airbnb.repository;

import com.nikhil.airbnb.entity.Holiday;
import com.nikhil.airbnb.entity.HolidayId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, HolidayId> {
}