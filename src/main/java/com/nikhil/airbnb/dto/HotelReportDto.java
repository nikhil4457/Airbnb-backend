package com.nikhil.airbnb.dto;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class HotelReportDto {
    Long bookingCount;
    BigDecimal totalRevenue;
    BigDecimal avgRevenue;
}
