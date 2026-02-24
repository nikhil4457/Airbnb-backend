package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.CalendarificResponse;
import com.nikhil.airbnb.entity.enums.CountryCode;
import com.nikhil.airbnb.service.serviceInterfaces.AbstractHolidayService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class CalendarificHolidayServiceImpl extends AbstractHolidayService {
    // =====================================================================================================================
    final RestClient calendarificRestClient;
    @Value("${calendarific.secret-key-1}")
    String apiKey1;
    @Value("${calendarific.secret-key-2}")
    String apiKey2;
    @Value("${calendarific.secret-key-3}")
    String apiKey3;
    @Value("${calendarific.secret-key-4}")
    String apiKey4;
    @Value("${calendarific.secret-key-5}")
    String apiKey5;
    @Value("${calendarific.secret-key-6}")
    String apiKey6;
    // =====================================================================================================================

    @Override
    protected boolean isHolidayInternal(
            LocalDate date,
            CountryCode countryCode
    ) {
        List<String> apiKeys = List.of(apiKey1, apiKey2, apiKey3, apiKey4, apiKey5, apiKey6);
        for (String apiKey : apiKeys) {
            if (isHolidayWithKey(date, appCountryCodeToApiCountryCode(countryCode), apiKey))
                return true;
        }
        return false;
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    protected String appCountryCodeToApiCountryCode(CountryCode countryCode) {
        return countryCode.name();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    private boolean isHolidayWithKey(
            LocalDate date,
            String countryCode,
            String apiKey
    ) {

            try {
                CalendarificResponse response =
                            calendarificRestClient.get()
                                    .uri(uriBuilder -> uriBuilder
                                            .path("/holidays")
                                            .queryParam("api_key", apiKey)
                                            .queryParam("country", countryCode)
                                            .queryParam("year", date.getYear())
                                            .queryParam("month", date.getMonthValue())
                                            .queryParam("day", date.getDayOfMonth())
                                            .build()
                                    )
                                    .retrieve()
                                    .body(CalendarificResponse.class);

                return response != null
                        && response.getResponse() != null
                        && !response.getResponse().getHolidays().isEmpty();
            }
            catch(Exception e){
                log.warn(e.getLocalizedMessage());
                return false;
            }
    }

    // =====================================================================================================================
}
