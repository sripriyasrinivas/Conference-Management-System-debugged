package com.cms.service.impl;

import com.cms.model.Payment;
import com.cms.model.User;
import com.cms.model.Conference;
import com.cms.repository.PaymentRepository;
import com.cms.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment processPayment(User attendee, Conference conference, Payment.PackageType packageType) {
        if (attendee == null || conference == null || packageType == null) {
            throw new RuntimeException("Invalid payment parameters");
        }

        // Check if payment already exists for this attendee and conference
        Optional<Payment> existing = paymentRepository.findByAttendeeAndConference(attendee, conference);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                throw new RuntimeException("Payment already completed for this conference");
            }
        }

        // Create new payment record
        Payment payment = Payment.builder()
                .attendee(attendee)
                .conference(conference)
                .packageType(packageType)
                .amount(packageType.getPrice())
                .status(Payment.PaymentStatus.PENDING)
                .build();

        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByAttendeeAndConference(User attendee, Conference conference) {
        return paymentRepository.findByAttendeeAndConference(attendee, conference)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByAttendee(User attendee) {
        return paymentRepository.findByAttendee(attendee);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasVipPackage(User attendee, Conference conference) {
        return paymentRepository.existsByAttendeeAndConferenceAndStatus(attendee, conference, Payment.PaymentStatus.COMPLETED)
                && paymentRepository.findByAttendeeAndConference(attendee, conference)
                        .map(p -> p.getPackageType() == Payment.PackageType.VIP)
                        .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasWorkshopOnlyPackage(User attendee, Conference conference) {
        return paymentRepository.existsByAttendeeAndConferenceAndStatus(attendee, conference, Payment.PaymentStatus.COMPLETED)
                && paymentRepository.findByAttendeeAndConference(attendee, conference)
                        .map(p -> p.getPackageType() == Payment.PackageType.WORKSHOP_ONLY)
                        .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCompletedPayment(User attendee, Conference conference) {
        return paymentRepository.existsByAttendeeAndConferenceAndStatus(attendee, conference, Payment.PaymentStatus.COMPLETED);
    }

    @Override
    public Payment completePayment(Long paymentId, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionId(transactionId != null ? transactionId : generateTransactionId());
        payment.setCompletedAt(LocalDateTime.now());
        
        return paymentRepository.save(payment);
    }

    @Override
    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Only completed payments can be refunded");
        }
        
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        return paymentRepository.save(payment);
    }

    /**
     * Generate a dummy transaction ID for testing purposes
     */
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
