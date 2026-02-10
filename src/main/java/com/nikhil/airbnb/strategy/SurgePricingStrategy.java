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
public class SurgePricingStrategy implements PricingStrategy {
    // =====================================================================================================================
    PricingStrategy wrapped;
    // =====================================================================================================================

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        return wrapped.calculatePrice(inventory).multiply(inventory.getSurgeFactor());
    }

    // =====================================================================================================================
}
