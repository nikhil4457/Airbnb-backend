package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.entity.Holiday;
import com.nikhil.airbnb.entity.HolidayId;
import com.nikhil.airbnb.entity.enums.CountryCode;
import com.nikhil.airbnb.repository.HolidayRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;


@FieldDefaults(level = AccessLevel.PROTECTED)
@Slf4j
public abstract class AbstractHolidayService {
    // =====================================================================================================================
    protected final HolidayRepository holidayRepository;
    // =====================================================================================================================

    protected AbstractHolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    public boolean isHoliday(LocalDate date, CountryCode countryCode) {
        if (isWeekend(date))
            return true;

        HolidayId id = new HolidayId(date, countryCode);
        if (holidayRepository.existsById(id)) {
            log.info("Found in Holiday repository");
            return true;
        }

        boolean isHoliday = isHolidayInternal(date, countryCode);
        if (isHoliday) {
            log.info("Attempting to save holiday: {}", id);

            try {
                Holiday saved = holidayRepository.save(new Holiday(id));
                log.info("Save returned: {}", saved);

                // Force flush to DB
                holidayRepository.flush();
                log.info("Flushed to DB");

                // Verify it's there
                boolean exists = holidayRepository.existsById(id);
                log.info("Exists after save: {}", exists);

            } catch (Exception e) {
                log.error("Save failed!", e);
                throw e;
            }
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
