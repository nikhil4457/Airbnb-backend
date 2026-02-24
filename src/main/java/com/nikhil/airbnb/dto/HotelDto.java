package com.nikhil.airbnb.dto;

import com.nikhil.airbnb.entity.HotelContactInfo;
import jakarta.validation.constraints.Null;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class HotelDto {
    @Null(message = "ID must not be provided")
    private Long id;
    private String name;
    private String city;
    private Set<String> photos = new HashSet<>();
    private Set<String> amenities = new HashSet<>();
    private HotelContactInfo contactInfo;
    private Boolean active;
}
