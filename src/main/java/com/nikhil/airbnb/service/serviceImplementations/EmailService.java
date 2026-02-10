package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.entity.Booking;
import com.nikhil.airbnb.entity.enums.BookingStatus;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    // =====================================================================================================================
    private final JavaMailSender mailSender;

    @Value("${spring.application.email.from}")
    private String fromEmail;
    @Value("${spring.application.email.from-name}")
    private String fromName;
    // =====================================================================================================================

    public void sendBookingCancellationEmail(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("Booking Cancelled - " + booking.getHotel().getName());

            String content = buildCancellationEmail(booking);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Sent cancellation email to {}", booking.getUser().getEmail());

        } catch (Exception e) {
            log.error("Failed to send email to {}", booking.getUser().getEmail(), e);
        }
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    private String buildCancellationEmail(Booking booking) {

        String refundLine =
                booking.getBookingStatus() == BookingStatus.CONFIRMED
                        ? "<p>You will receive a full refund within 5–7 business days.</p>"
                        : "";

        return """
        <html>
        <body style="font-family: Arial, sans-serif;">
            <h2>Booking Cancellation Notice</h2>
            <p>Dear %s,</p>
            <p>We regret to inform you that your booking has been cancelled by the hotel due to some internal reasons.</p>

            <div style="background: #f5f5f5; padding: 15px; margin: 20px 0;">
                <h3>Booking Details:</h3>
                <p><strong>Booking ID:</strong> %s</p>
                <p><strong>Hotel:</strong> %s</p>
                <p><strong>Check-in:</strong> %s</p>
                <p><strong>Check-out:</strong> %s</p>
                <p><strong>Amount:</strong> ₹%s</p>
            </div>

            %s

            <p>We apologize for any inconvenience caused.</p>

            <p>Best regards,<br/>AirBnB Clone Team</p>
        </body>
        </html>
        """.formatted(
                booking.getUser().getName(),
                booking.getId(),
                booking.getHotel().getName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getAmount(),
                refundLine
        );
    }

    // =====================================================================================================================
}