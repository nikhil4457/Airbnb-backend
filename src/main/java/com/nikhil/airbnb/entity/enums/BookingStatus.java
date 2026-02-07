package com.nikhil.airbnb.entity.enums;

public enum BookingStatus {
    RESERVED,                      // Initial reservation
    GUESTS_ADDED,                  // Guest details added
    PAYMENT_PENDING,               // Waiting for payment
    CONFIRMED,                     // Payment successful, booking active
    CANCELLED_BY_USER,             // User cancelled
    CANCELLED_BY_HOTEL_MANAGER,    // Hotel closed inventory
    EXPIRED                        // Reservation timed out
}
