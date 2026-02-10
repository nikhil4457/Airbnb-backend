package com.nikhil.airbnb.entity;

import com.nikhil.airbnb.entity.enums.CountryCode;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HolidayId implements Serializable {

    @NotNull
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CountryCode countryCode;
}