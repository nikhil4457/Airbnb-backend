package com.nikhil.airbnb.entity.enums;


import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

public enum PaymentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
