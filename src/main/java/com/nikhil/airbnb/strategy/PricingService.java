package com.nikhil.airbnb.strategy;


import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.service.serviceInterfaces.AbstractHolidayService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PricingService {
    // =====================================================================================================================
    AbstractHolidayService holidayService;
    // =====================================================================================================================

    public BigDecimal calculateDynamicPricing(Inventory inventory){
        log.info("Calculating dynamic price ...");
        PricingStrategy pricingStrategy = new BasePricingStrategy();
        pricingStrategy = new SurgePricingStrategy(pricingStrategy);
        pricingStrategy = new UrgencyPricingStrategy(pricingStrategy);
        pricingStrategy = new HolidayPricingStrategy(pricingStrategy, holidayService);
        pricingStrategy = new OccupancyPricingStrategy(pricingStrategy);
        return pricingStrategy.calculatePrice(inventory);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    //    Return the sum of prices of individual rooms for these inventories
    public BigDecimal calculateTotalPrice(List<Inventory> inventoryList) {
        return inventoryList.stream()
                .map(this::calculateDynamicPricing)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =====================================================================================================================
}


