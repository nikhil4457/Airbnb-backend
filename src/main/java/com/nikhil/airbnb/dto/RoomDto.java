package com.nikhil.airbnb.dto;

import com.nikhil.airbnb.entity.Hotel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Null;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class RoomDto {
    @Null(message = "ID must not be provided")
    private Long id;
    private Long hotelId;
    private String type;
    private BigDecimal basePrice;
    private Set<String> imageUrls = new HashSet<>();
    private Set<String> amenities = new HashSet<>();
    private Integer totalCount;
    private Integer capacity;
}
