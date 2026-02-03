package com.nikhil.airbnb.dto;

import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class GuestDto {
    private Long id;
    private AppUser user;
    private String name;
    private Gender gender;
    private Integer age;
}
