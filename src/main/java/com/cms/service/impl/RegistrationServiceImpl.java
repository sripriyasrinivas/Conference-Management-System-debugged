package com.cms.service.impl;

import com.cms.model.*;
import com.cms.repository.RegistrationRepository;
import com.cms.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;

    // ==================== Form Validation (Activity Diagram Step 1) ====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> validateRegistrationForm(String fullName, String email, String registrationType) {
        Map<String, String> errors = new HashMap<>();

        // Validate Full Name
        if (fullName == null || fullName.trim().isEmpty()) {
            errors.put("fullName", "Full name is required");
        } else if (fullName.trim().length() < 2) {
            errors.put("fullName", "Full name must be at least 2 characters");
        }

        // Validate Email
        if (email == null || email.trim().isEmpty()) {
            errors.put("email", "Email is required");
        } else if (!isValidEmail(email)) {
            errors.put("email", "Invalid email format");
        }

        // Validate Registration Type
        if (registrationType == null || registrationType.trim().isEmpty()) {
            errors.put("type", "Registration type is required");
        } else {
            try {
                Registration.RegistrationType.valueOf(registrationType);
            } catch (IllegalArgumentException e) {
                errors.put("type", "Invalid registration type");
            }
        }

        return errors;
    }

    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    // ==================== Core Registration with Full Workflow ====================

    @Override
    public Registration register(User attendee, Conference conference,
                                  Registration.RegistrationType type,
                                  String dietary, String special) {
        return registerWithWorkshops(attendee, conference, type, dietary, special, null);
    }

    @Override
    public Registration registerWithWorkshops(User attendee, Conference conference,
                                             Registration.RegistrationType type,
                                             String dietary, String special,
                                             String selectedWorkshops) {
        if (registrationRepository.existsByAttendeeAndConference(attendee, conference)) {
            throw new RuntimeException("Already registered for this conference.");
        }

        if (!isConferenceAvailable(conference)) {
            throw new RuntimeException("This conference is no longer accepting registrations.");
        }

        long confirmed = registrationRepository.countByConferenceAndStatus(
                conference, Registration.RegistrationStatus.CONFIRMED);
        Registration.RegistrationStatus status = (conference.getMaxAttendees() > 0
                && confirmed >= conference.getMaxAttendees())
                ? Registration.RegistrationStatus.WAITLISTED
                : Registration.RegistrationStatus.CONFIRMED;

        Registration reg = Registration.builder()
                .attendee(attendee)
                .conference(conference)
                .type(type)
                .status(status)
                .dietaryPreferences(dietary)
                .specialRequirements(special)
                .selectedWorkshops(selectedWorkshops)
                .formValidated(true)
                .confirmedAt(status == Registration.RegistrationStatus.CONFIRMED ? LocalDateTime.now() : null)
                .build();

        return registrationRepository.save(reg);
    }

    @Override
    public Registration processValidatedRegistration(User attendee, Conference conference,
                                                    Registration.RegistrationType type,
                                                    String dietary, String special,
                                                    String selectedWorkshops, boolean vipPurchased) {
        // Register the attendee
        Registration reg = registerWithWorkshops(attendee, conference, type, dietary, special, selectedWorkshops);

        // Assign role based on VIP package and workshop selection
        assignRoleBasedOnPackage(reg.getId(), vipPurchased, selectedWorkshops != null && !selectedWorkshops.isEmpty());

        return registrationRepository.findById(reg.getId()).orElse(reg);
    }

    @Override
    public Registration updateStatus(Long id, Registration.RegistrationStatus status) {
        Registration reg = registrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found: " + id));
        reg.setStatus(status);
        if (status == Registration.RegistrationStatus.CONFIRMED) {
            reg.setConfirmedAt(LocalDateTime.now());
        }
        return registrationRepository.save(reg);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Registration> findById(Long id) {
        return registrationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Registration> findByAttendee(User attendee) {
        return registrationRepository.findByAttendee(attendee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Registration> findByConference(Conference conference) {
        return registrationRepository.findByConference(conference);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Registration> findByAttendeeAndConference(User attendee, Conference conference) {
        return registrationRepository.findByAttendeeAndConference(attendee, conference);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRegistered(User attendee, Conference conference) {
        return registrationRepository.existsByAttendeeAndConference(attendee, conference);
    }

    @Override
    public void cancelRegistration(Long id) {
        Registration reg = registrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found: " + id));
        reg.setStatus(Registration.RegistrationStatus.CANCELLED);
        registrationRepository.save(reg);
        
        // Promote from waitlist if space available
        promoteFromWaitlist(reg.getConference());
    }

    @Override
    @Transactional(readOnly = true)
    public long countConfirmedByConference(Conference conference) {
        return registrationRepository.countByConferenceAndStatus(conference,
                Registration.RegistrationStatus.CONFIRMED);
    }

    // ==================== Role Assignment (Activity Diagram Step 2) ====================

    @Override
    public void assignRoleBasedOnPackage(Long registrationId, boolean vipPurchased, boolean workshopSelected) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found: " + registrationId));

        Registration.AssignedRole role = determineRole(vipPurchased, workshopSelected);
        reg.setAssignedRole(role);

        registrationRepository.save(reg);
    }

    @Override
    public Registration.AssignedRole determineRole(boolean vipPurchased, boolean workshopSelected) {
        // Activity Diagram Logic:
        // - If VIP package purchased → VIP Role
        // - Else if workshops selected → WORKSHOP_ONLY Role
        // - Else → ATTENDEE Role
        if (vipPurchased) {
            return Registration.AssignedRole.VIP;
        } else if (workshopSelected) {
            return Registration.AssignedRole.WORKSHOP_ONLY;
        } else {
            return Registration.AssignedRole.ATTENDEE;
        }
    }

    // ==================== Access Control & Validation ====================

    @Override
    @Transactional(readOnly = true)
    public boolean isConferenceAvailable(Conference conference) {
        if (conference == null) {
            return false;
        }

        Conference.ConferenceStatus status = conference.getStatus();
        return status == Conference.ConferenceStatus.UPCOMING
                || status == Conference.ConferenceStatus.OPEN_FOR_SUBMISSIONS
                || status == Conference.ConferenceStatus.SCHEDULE_PUBLISHED;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAvailableSpace(Conference conference) {
        if (conference == null || conference.getMaxAttendees() <= 0) {
            return true;
        }

        long confirmed = countConfirmedByConference(conference);
        return confirmed < conference.getMaxAttendees();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Registration> getWaitlistedByConference(Conference conference) {
        return registrationRepository.findByConferenceAndStatus(conference,
                Registration.RegistrationStatus.WAITLISTED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRegister(User attendee, Conference conference) {
        if (attendee == null || conference == null) {
            return false;
        }

        if (!attendee.isEnabled()) {
            return false;
        }

        if (attendee.getRole() != User.Role.ATTENDEE) {
            return false;
        }

        return isConferenceAvailable(conference);
    }

    @Override
    public void promoteFromWaitlist(Conference conference) {
        if (conference == null || !hasAvailableSpace(conference)) {
            return;
        }

        List<Registration> waitlisted = getWaitlistedByConference(conference);
        if (!waitlisted.isEmpty()) {
            Registration toPromote = waitlisted.get(0);
            toPromote.setStatus(Registration.RegistrationStatus.CONFIRMED);
            toPromote.setConfirmedAt(LocalDateTime.now());
            registrationRepository.save(toPromote);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countWaitlistedByConference(Conference conference) {
        return registrationRepository.countByConferenceAndStatus(conference,
                Registration.RegistrationStatus.WAITLISTED);
    }

    // ==================== Certificate & Attendance Tracking ====================

    @Override
    public void markAttendanceConfirmed(Long registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found: " + registrationId));
        reg.setAttendanceConfirmed(true);
        registrationRepository.save(reg);
    }

    @Override
    public void issueCertificate(Long registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found: " + registrationId));

        if (!reg.isAttendanceConfirmed()) {
            throw new RuntimeException("Attendance must be confirmed before issuing certificate.");
        }

        reg.setCertificateIssued(true);
        reg.setCertificateIssuedAt(LocalDateTime.now());
        registrationRepository.save(reg);
    }

    @Override
    public Registration updateSessionAttendance(Long registrationId, int sessionsAttended) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found: " + registrationId));
        reg.setSessionsAttended(sessionsAttended);
        return registrationRepository.save(reg);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Registration> findEligibleForCertificate(Conference conference) {
        return registrationRepository.findEligibleForCertificate(conference);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCertificatesIssuedByConference(Conference conference) {
        return registrationRepository.countByConferenceAndCertificateIssuedTrue(conference);
    }
}
