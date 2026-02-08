package com.nikhil.airbnb.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class UpdateInventoryRequestDto {
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal SurgeFactor;
    Boolean closed;
}
