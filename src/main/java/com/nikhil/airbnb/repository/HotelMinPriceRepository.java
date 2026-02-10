package com.nikhil.airbnb.repository;

import com.nikhil.airbnb.dto.HotelPriceDto;
import com.nikhil.airbnb.entity.Hotel;
import com.nikhil.airbnb.entity.HotelMinPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice, Long> {

    @Query(
            """
            SELECT new com.nikhil.airbnb.dto.HotelPriceDto(i.hotel, AVG(i.minPrice))
            FROM HotelMinPrice i
            WHERE i.hotel.city = :city
                AND i.date BETWEEN :startDate AND :endDate
                AND i.hotel.active = true
            GROUP BY i.hotel
            """
    )
    Page<HotelPriceDto> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
    List<HotelMinPrice> findByHotelAndDateIn(Hotel hotel, ArrayList<LocalDate> localDates);
}