package com.nikhil.airbnb.dto;

import com.nikhil.airbnb.entity.Hotel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class HotelPriceDto {
    HotelDto hotel;
    double price;
    // Constructor for JPQL - accepts entity, converts internally
    public HotelPriceDto(Hotel hotel, double price) {
        this.hotel = HotelDto.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .city(hotel.getCity())
                .photos(hotel.getPhotos())
                .photos(new HashSet<>(hotel.getPhotos()))
                .amenities(new HashSet<>(hotel.getAmenities()))
                .active(hotel.getActive())
                .build();
        this.price = price;
    }
}
