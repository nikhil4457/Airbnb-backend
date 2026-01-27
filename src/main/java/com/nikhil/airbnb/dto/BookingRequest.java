package com.nikhil.airbnb.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class BookingRequest{
    Long hotelId;
    Long roomId;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer roomsCount;
}
