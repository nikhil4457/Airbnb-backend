package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.entity.AppUser;
import com.nikhil.airbnb.entity.Booking;
import com.nikhil.airbnb.repository.BookingRepository;
import com.nikhil.airbnb.service.serviceInterfaces.CheckoutService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {
    // =====================================================================================================================
    BookingRepository bookingRepository;
    // =====================================================================================================================

    @Override
    @Transactional
    public String getCheckoutSession(Booking booking, String successUrl, String failureUrl) {
        log.info("Creating session for booking with id: {} .....",booking.getId());
        AppUser appUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            CustomerCreateParams customerParams = CustomerCreateParams
                    .builder()
                    .setName(appUser.getName())
                    .setEmail(appUser.getEmail())
                    .build();
            Customer customer = Customer.create(customerParams);
            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                    .setCustomer(customer.getId())
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(failureUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem
                                    .builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData
                                                    .builder()
                                                    .setCurrency("inr")
                                                    .setUnitAmount(booking
                                                            .getAmount()
                                                            .multiply(BigDecimal.valueOf(100))
                                                            .longValue()
                                                    )
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData
                                                                    .builder()
                                                                    .setName(booking.getHotel().getName() +
                                                                            " : " +
                                                                            booking.getRoom().getType())
                                                                    .setDescription("Booking ID: " + booking.getId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();
            Session session = Session.create(sessionParams);
            booking.setPaymentSessionId(session.getId());
            bookingRepository.save(booking);
            log.info("Created a session for booking with id: {}", booking.getId());
            return session.getUrl();

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    // =====================================================================================================================
}
