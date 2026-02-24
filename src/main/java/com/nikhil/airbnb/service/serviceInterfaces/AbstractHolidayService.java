package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.entity.enums.CountryCode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;

import java.time.DayOfWeek;
import java.time.LocalDate;


@FieldDefaults(level = AccessLevel.PROTECTED)
@Slf4j
public abstract class AbstractHolidayService {
    // =====================================================================================================================
    // =====================================================================================================================

    @Cacheable(
            value = "holidays",
            key = "#countryCode + ':' + #date",
            unless = "#result == false"
    )
    public boolean isHoliday(LocalDate date, CountryCode countryCode) {
        if (isWeekend(date))
            return true;

        log.info("Cache MISS - Calling API: {} - {}", countryCode, date);
        boolean isHoliday = isHolidayInternal(date, countryCode);
        if (isHoliday) {
            log.info("Holiday found via API: {} - {}", countryCode, date);
        }
        return isHoliday;
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    protected boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    /*-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    -x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-*/
    protected abstract boolean isHolidayInternal(LocalDate date, CountryCode countryCode);
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    protected abstract String appCountryCodeToApiCountryCode(CountryCode countryCode);

    // =====================================================================================================================
}
