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
public class OccupancyPricingStrategy implements PricingStrategy {
    // =====================================================================================================================
    PricingStrategy wrapped;
    // =====================================================================================================================

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);
        double occupancyRate = inventory.getBookedCount() / (double) inventory.getTotalCount();
        if(occupancyRate > 0.8) {
            price = price.multiply(BigDecimal.valueOf(1.2));
        }
        return price;
    }

    // =====================================================================================================================
}