package com.nikhil.airbnb.dto;


import com.nikhil.airbnb.entity.enums.Gender;
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
public class ProfileUpdateRequestDto {
    String name;
    LocalDate dateOfBirth;
    Gender gender;
}
