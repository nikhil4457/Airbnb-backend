package com.nikhil.airbnb.dto;

import com.nikhil.airbnb.entity.HotelContactInfo;
import jakarta.persistence.*;
import jakarta.validation.constraints.Null;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
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
