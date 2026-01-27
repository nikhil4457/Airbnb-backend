package com.nikhil.airbnb.dto;


import com.nikhil.airbnb.entity.enums.BookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class BookingDto {
    Long id;
    Integer roomsCount;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    LocalDateTime createdAt;
    BookingStatus bookingStatus;
    Set<GuestDto> guests = new HashSet<>();
}
