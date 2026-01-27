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
public class HotelSearchRequest {
    String city;
    LocalDate startDate;
    LocalDate endDate;
    Integer roomsCount;
    Integer page = 0;
    Integer size = 10;
}
