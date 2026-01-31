package com.nikhil.airbnb.dto;

import com.nikhil.airbnb.entity.Hotel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class HotelPriceDto {
    Hotel hotel;
    double price;
}
