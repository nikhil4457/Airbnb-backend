package com.nikhil.airbnb.strategy;

import com.nikhil.airbnb.entity.Inventory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HolidayPricingStrategy implements PricingStrategy {
    PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        boolean isTodayHoliday = true; // call some external service to check if today is holiday or check in local data
        BigDecimal price = wrapped.calculatePrice(inventory);
        if(isTodayHoliday)
            price = price.multiply(BigDecimal.valueOf(1.2));
        return price;
    }
}