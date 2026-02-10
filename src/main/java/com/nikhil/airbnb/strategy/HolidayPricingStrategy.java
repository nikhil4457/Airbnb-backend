package com.nikhil.airbnb.strategy;

import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.entity.enums.CountryCode;
import com.nikhil.airbnb.service.serviceInterfaces.AbstractHolidayService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HolidayPricingStrategy implements PricingStrategy {
    // =====================================================================================================================
    PricingStrategy wrapped;
    AbstractHolidayService holidayService;
    // =====================================================================================================================

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        boolean isHoliday = holidayService.isHoliday(inventory.getDate(), CountryCode.IN); // call some external service to check if today is holiday or check in local data
        log.info("is " + inventory.getDate() + "a holiday ? : " + isHoliday);
        BigDecimal price = wrapped.calculatePrice(inventory);
        if(isHoliday)
            price = price.multiply(BigDecimal.valueOf(1.2));
        return price;
    }

    // =====================================================================================================================
}