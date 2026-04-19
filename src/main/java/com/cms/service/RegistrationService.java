package com.cms.service;

import com.cms.model.Registration;
import com.cms.model.User;
import com.cms.model.Conference;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RegistrationService {
    // Core Registration
    Registration register(User attendee, Conference conference, Registration.RegistrationType type,
                          String dietary, String special);
    Registration registerWithWorkshops(User attendee, Conference conference, Registration.RegistrationType type,
                                       String dietary, String special, String selectedWorkshops);
    Registration updateStatus(Long id, Registration.RegistrationStatus status);
    Optional<Registration> findById(Long id);
    List<Registration> findByAttendee(User attendee);
    List<Registration> findByConference(Conference conference);
    Optional<Registration> findByAttendeeAndConference(User attendee, Conference conference);
    boolean isRegistered(User attendee, Conference conference);
    void cancelRegistration(Long id);
    long countConfirmedByConference(Conference conference);
    
    // Form Validation (Following Activity Diagram)
    Map<String, String> validateRegistrationForm(String fullName, String email, String registrationType);
    Registration processValidatedRegistration(User attendee, Conference conference, Registration.RegistrationType type,
                                             String dietary, String special, String selectedWorkshops, boolean vipPurchased);
    
    // Role Assignment
    void assignRoleBasedOnPackage(Long registrationId, boolean vipPurchased, boolean workshopSelected);
    Registration.AssignedRole determineRole(boolean vipPurchased, boolean workshopSelected);
    
    // Access Control & Validation
    boolean isConferenceAvailable(Conference conference);
    boolean hasAvailableSpace(Conference conference);
    List<Registration> getWaitlistedByConference(Conference conference);
    boolean canRegister(User attendee, Conference conference);
    void promoteFromWaitlist(Conference conference);
    long countWaitlistedByConference(Conference conference);
    
    // Certificate & Attendance Tracking
    void markAttendanceConfirmed(Long registrationId);
    void issueCertificate(Long registrationId);
    Registration updateSessionAttendance(Long registrationId, int sessionsAttended);
    List<Registration> findEligibleForCertificate(Conference conference);
    long countCertificatesIssuedByConference(Conference conference);
}
