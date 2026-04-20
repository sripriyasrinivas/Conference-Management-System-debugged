package com.cms.repository;

import com.cms.model.Payment;
import com.cms.model.User;
import com.cms.model.Conference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByAttendee(User attendee);
    List<Payment> findByConference(Conference conference);
    Optional<Payment> findByAttendeeAndConference(User attendee, Conference conference);
    List<Payment> findByAttendeeAndConferenceAndStatus(User attendee, Conference conference, Payment.PaymentStatus status);
    boolean existsByAttendeeAndConferenceAndStatus(User attendee, Conference conference, Payment.PaymentStatus status);
}
