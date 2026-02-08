package com.nikhil.airbnb.repository;

import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.Booking;
import com.nikhil.airbnb.entity.Hotel;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPaymentSessionId(String sessionId);
    List<Booking> findByHotel(Hotel hotel);
    List<Booking> findByBookingStatusInAndCreatedAtBefore(List<BookingStatus> reserved, LocalDateTime expiryThreshold);

    // This finds all bookings that have at least one date in the given range and at least on of the statuses
    @Query("""
        SELECT DISTINCT b
        FROM Booking b
        WHERE b.room.id = :roomId
          AND NOT(b.checkInDate > :endDate OR b.checkOutDate <= :startDate)
          AND b.bookingStatus IN :statuses
    """)
    List<Booking> findAllIntersectingWithDateRangeAndStatusIn(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<BookingStatus> statuses
    );

    List<Booking> findByHotelAndCreatedAtBetween(Hotel hotel, LocalDateTime startDateTime, LocalDateTime endDateTime);
    List<Booking> getByUser(AppUser currentUser);
}