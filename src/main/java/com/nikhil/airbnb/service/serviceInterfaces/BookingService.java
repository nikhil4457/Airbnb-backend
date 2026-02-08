package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.dto.BookingDto;
import com.nikhil.airbnb.dto.BookingRequest;
import com.nikhil.airbnb.dto.HotelReportDto;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import com.stripe.model.Event;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface BookingService {

    BookingDto initialiseBooking(BookingRequest bookingRequest);
    BookingDto addGuests(Long bookingId, Set<Long> guestIdList);
    String initiatePayment(Long bookingId);
    void capturePayment(Event event);
    void cancelBooking(Long bookingId, boolean isCancelledByUser);
    BookingStatus getBookingStatus(Long bookingId);
    List<BookingDto> getAllBookingsByHotelId(Long hotelId);

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    void cleanupExpiredBookings();

    HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate);
    List<BookingDto> getMyBookings();
}
