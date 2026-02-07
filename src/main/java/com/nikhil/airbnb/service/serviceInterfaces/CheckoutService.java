package com.nikhil.airbnb.service.serviceInterfaces;

import com.nikhil.airbnb.entity.Booking;

public interface CheckoutService {
    String getCheckoutSession(Booking booking, String successUrl, String failureUrl);
}
