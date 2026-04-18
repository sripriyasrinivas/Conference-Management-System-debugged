package com.cms.pattern.facade;

import com.cms.model.*;
import com.cms.pattern.builder.ConferenceSchedule;
import com.cms.pattern.builder.ScheduleDirector;
import com.cms.service.*;
import com.cms.service.impl.CertificateServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  DESIGN PATTERN 3 : Facade  (Structural)                    ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Intent :                                                    ║
 * ║    Provide a unified, simplified interface to a set of       ║
 * ║    interfaces in a subsystem. The Facade defines a           ║
 * ║    higher-level interface that makes the subsystem easier    ║
 * ║    to use.                                                   ║
 * ║                                                              ║
 * ║  Why used here :                                             ║
 * ║    The Admin dashboard needs to perform complex operations   ║
 * ║    that coordinate multiple services:                        ║
 * ║      • closeConference() calls ConferenceService,            ║
 * ║        RegistrationService, CertificateServiceImpl,         ║
 * ║        and SessionService in a specific sequence.           ║
 * ║      • getConferenceSummary() aggregates data from 4         ║
 * ║        different services into one result map.              ║
 * ║      • publishSchedule() calls SessionService +             ║
 * ║        ConferenceService + ScheduleDirector together.       ║
 * ║    Without the Facade the AdminController would be tightly   ║
 * ║    coupled to 5+ services and contain orchestration logic.  ║
 * ║                                                              ║
 * ║  Participants :                                              ║
 * ║    Facade     → ConferenceFacade (this class)               ║
 * ║    Subsystems → ConferenceService, RegistrationService,     ║
 * ║                 CertificateServiceImpl, SessionService,     ║
 * ║                 PaperService, ScheduleDirector              ║
 * ║    Client     → AdminController                             ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Component
@RequiredArgsConstructor
public class ConferenceFacade {

    // ── Subsystem services ────────────────────────────────────────
    private final ConferenceService       conferenceService;
    private final RegistrationService     registrationService;
    private final CertificateServiceImpl  certificateService;
    private final SessionService          sessionService;
    private final PaperService            paperService;
    private final ScheduleDirector        scheduleDirector;

    /**
     * FACADE OPERATION 1: Close a conference.
     *
     * Coordinates across 3 subsystems in one call:
     *   1. Issues participation certificates to all confirmed attendees
     *   2. Updates conference status to COMPLETED
     *
     * AdminController calls ONE method instead of 3 service calls.
     */
    public String closeConference(Long conferenceId) {
        Conference conference = conferenceService.findById(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found: " + conferenceId));

        // Step 1: Bulk-issue participation certificates
        certificateService.bulkIssueParticipationCertificates(conference);

        // Step 2: Mark conference as completed
        conferenceService.updateStatus(conferenceId, Conference.ConferenceStatus.COMPLETED);

        long certCount = certificateService.findByConference(conference).size();
        return "Conference closed. " + certCount + " certificates issued.";
    }

    /**
     * FACADE OPERATION 2: Get a unified conference summary.
     *
     * Aggregates data from 4 subsystems into one map.
     * AdminController gets everything in one call.
     */
    public Map<String, Object> getConferenceSummary(Long conferenceId) {
        Conference conference = conferenceService.findById(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found: " + conferenceId));

        Map<String, Object> summary = new HashMap<>();

        // From ConferenceService
        summary.put("conference", conference);
        summary.put("status", conference.getStatus());

        // From RegistrationService
        List<Registration> registrations = registrationService.findByConference(conference);
        long confirmed = registrationService.countConfirmedByConference(conference);
        summary.put("totalRegistrations", registrations.size());
        summary.put("confirmedCount", confirmed);
        summary.put("availableSeats",
                Math.max(0, conference.getMaxAttendees() - (int) confirmed));

        // From SessionService
        List<Session> sessions = sessionService.findAll();
        summary.put("totalSessions", sessions.size());
        summary.put("tracks", sessionService.getAllTracks());

        // From PaperService
        summary.put("acceptedPapers",
                paperService.countByStatus(Paper.Status.ACCEPTED));
        summary.put("totalPapers", paperService.findAll().size());

        // From CertificateService
        summary.put("certificatesIssued",
                certificateService.findByConference(conference).size());

        return summary;
    }

    /**
     * FACADE OPERATION 3: Publish the conference schedule.
     *
     * Uses ScheduleDirector (Builder pattern) + ConferenceService
     * to build and activate the schedule in one step.
     */
    public ConferenceSchedule publishSchedule(Long conferenceId) {
        Conference conference = conferenceService.findById(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found: " + conferenceId));

        List<Session> sessions = sessionService.findAll();

        // Delegate to Builder Director to construct the schedule
        ConferenceSchedule schedule = scheduleDirector.buildFullSchedule(
                conference.getName(),
                conference.getVenue() != null ? conference.getVenue() : "",
                conference.getStartDate(),
                conference.getEndDate(),
                sessions
        );

        // Update conference status to reflect published schedule
        conferenceService.updateStatus(conferenceId,
                Conference.ConferenceStatus.SCHEDULE_PUBLISHED);

        return schedule;
    }

    /**
     * FACADE OPERATION 4: Register an attendee for a conference.
     *
     * Hides the multi-step check: already registered? capacity?
     * Returns a user-friendly result message.
     */
    public String registerAttendee(User attendee,
                                    Long conferenceId,
                                    Registration.RegistrationType type) {
        Conference conference = conferenceService.findById(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        if (registrationService.isRegistered(attendee, conference)) {
            return "You are already registered for this conference.";
        }

        Registration reg = registrationService.register(
                attendee, conference, type, null, null);

        return reg.getStatus() == Registration.RegistrationStatus.CONFIRMED
                ? "Successfully registered for " + conference.getName() + "!"
                : "Added to waitlist for " + conference.getName() + ".";
    }
}
