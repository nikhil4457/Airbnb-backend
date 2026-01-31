package com.nikhil.airbnb.strategy;

import com.nikhil.airbnb.entity.Inventory;

import java.math.BigDecimal;

public interface PricingStrategy {

    BigDecimal calculatePrice(Inventory inventory);
}
