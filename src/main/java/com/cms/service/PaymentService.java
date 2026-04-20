package com.cms.service;

import com.cms.model.Payment;
import com.cms.model.User;
import com.cms.model.Conference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentService {
    // Payment Processing (Dummy System)
    Payment processPayment(User attendee, Conference conference, Payment.PackageType packageType);
    Payment getPaymentByAttendeeAndConference(User attendee, Conference conference);
    Optional<Payment> findById(Long id);
    List<Payment> findByAttendee(User attendee);
    
    // Package Checking
    boolean hasVipPackage(User attendee, Conference conference);
    boolean hasWorkshopOnlyPackage(User attendee, Conference conference);
    boolean hasCompletedPayment(User attendee, Conference conference);
    
    // Payment Status
    Payment completePayment(Long paymentId, String transactionId);
    Payment refundPayment(Long paymentId);
}
