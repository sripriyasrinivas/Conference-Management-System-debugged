package com.cms.controller;

import com.cms.model.*;
import com.cms.service.*;
import com.cms.service.impl.CertificateServiceImpl;
import com.cms.service.impl.FeedbackServiceImpl;
import com.cms.service.impl.MessageServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/attendee")
@PreAuthorize("hasRole('ATTENDEE')")
@RequiredArgsConstructor
public class AttendeeController {

    private final UserService userService;
    private final ConferenceService conferenceService;
    private final RegistrationService registrationService;
    private final SessionService sessionService;
    private final FeedbackServiceImpl feedbackService;    // FIXED: Use Interface
    private final CertificateServiceImpl certificateService; // FIXED: Use Interface
    private final MessageServiceImpl messageService;     // FIXED: Use Interface

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        List<Registration> myRegistrations = registrationService.findByAttendee(attendee);
        
        model.addAttribute("attendee", attendee);
        model.addAttribute("myRegistrations", myRegistrations);
        model.addAttribute("conferences", conferenceService.findAll());
        model.addAttribute("myCertificates", certificateService.findByRecipient(attendee));
        model.addAttribute("unreadMessages", messageService.countUnread(attendee));
        
        // Pass IDs for easy registration checks in dashboard
        model.addAttribute("registeredConfIds", myRegistrations.stream()
                .map(r -> r.getConference().getId()).collect(Collectors.toList()));
        
        return "attendee/dashboard";
    }

    @GetMapping("/conferences")
    public String browsConferences(Model model) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        List<Conference> allConferences = conferenceService.findAll();
        List<Registration> myRegistrations = registrationService.findByAttendee(attendee);

        List<Long> registeredConfIds = myRegistrations.stream()
                .map(r -> r.getConference().getId())
                .collect(Collectors.toList());

        model.addAttribute("conferences", allConferences);
        model.addAttribute("registeredConfIds", registeredConfIds);
        return "attendee/conferences";
    }

    @GetMapping("/conferences/{id}/register")
    public String registerForm(@PathVariable Long id, Model model) {
        Conference conf = conferenceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Conference not found"));
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        if (registrationService.isRegistered(attendee, conf)) {
            return "redirect:/attendee/conferences";
        }
        model.addAttribute("conference", conf);
        model.addAttribute("types", Registration.RegistrationType.values());
        return "attendee/register-conference";
    }

    @PostMapping("/conferences/{id}/register")
    public String doRegister(@PathVariable Long id,
                              @RequestParam Registration.RegistrationType type,
                              @RequestParam(required = false) String dietary,
                              @RequestParam(required = false) String special,
                              RedirectAttributes ra) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        Conference conf = conferenceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Conference not found"));
        try {
            Registration reg = registrationService.register(attendee, conf, type, dietary, special);
            if (reg.getStatus() == Registration.RegistrationStatus.WAITLISTED) {
                ra.addFlashAttribute("warning", "Added to waitlist. Conference is at capacity.");
            } else {
                ra.addFlashAttribute("success", "Successfully registered for " + conf.getName() + "!");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/attendee/conferences";
    }

    @PostMapping("/registrations/{id}/cancel")
    public String cancelRegistration(@PathVariable Long id, RedirectAttributes ra) {
        registrationService.cancelRegistration(id);
        ra.addFlashAttribute("success", "Registration cancelled.");
        return "redirect:/attendee/dashboard";
    }
    // Add this to your AttendeeController
// --- Session/Event Enrollment --------------------------------------
    
    @GetMapping("/conferences/{id}/events")
    public String viewConferenceEvents(@PathVariable Long id, Model model) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        Conference conf = conferenceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        // Guard: Only allow registered attendees to see and join specific sessions
        if (!registrationService.isRegistered(attendee, conf)) {
            return "redirect:/attendee/conferences";
        }

        // Get all sessions (Filtering out general paper presentations if you want only workshops)
        List<Session> availableEvents = sessionService.findAll();

        model.addAttribute("conference", conf);
        model.addAttribute("events", availableEvents);
        model.addAttribute("attendee", attendee);
        
        // Pass IDs of sessions the user is already attending
        List<Long> attendingSessionIds = availableEvents.stream()
                .filter(s -> s.getAttendees().contains(attendee))
                .map(Session::getId)
                .collect(Collectors.toList());
        model.addAttribute("attendingSessionIds", attendingSessionIds);

        return "attendee/events";
    }

    @PostMapping("/events/{sessionId}/join")
    public String joinEvent(@PathVariable Long sessionId, 
                            @RequestParam Long conferenceId, 
                            RedirectAttributes ra) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        try {
            // Logic to add attendee to session (must be handled in SessionService)
            sessionService.enrollAttendee(sessionId, attendee);
            ra.addFlashAttribute("success", "Successfully enrolled in the event!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/attendee/conferences/" + conferenceId + "/events";
    }
    // ── Schedule: FIXED FOR NULL SAFETY ──────────────────────────────────────
    @GetMapping("/schedule")
    public String viewSchedule(Model model) {
        Map<LocalDate, List<Session>> schedule = sessionService.getScheduleGroupedByDate();
        List<String> tracks = sessionService.getAllTracks();

        model.addAttribute("schedule", schedule);
        model.addAttribute("tracks", tracks);
        return "attendee/schedule";
    }

    @GetMapping("/feedback")
    public String feedbackPage(Model model) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        model.addAttribute("sessions", sessionService.findAll());
        model.addAttribute("attendee", attendee);
        return "attendee/feedback";
    }

    @PostMapping("/feedback/{sessionId}")
    public String submitFeedback(@PathVariable Long sessionId,
                                  @RequestParam int contentRating,
                                  @RequestParam int speakerRating,
                                  @RequestParam int overallRating,
                                  @RequestParam(required = false) String comments,
                                  RedirectAttributes ra) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        try {
            feedbackService.submitFeedback(attendee, sessionId, contentRating,
                    speakerRating, overallRating, comments);
            ra.addFlashAttribute("success", "Thank you for your feedback!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/attendee/feedback";
    }

    @GetMapping("/certificates")
    public String myCertificates(Model model) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        model.addAttribute("certificates", certificateService.findByRecipient(attendee));
        return "attendee/certificates";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) return "redirect:/login";
        
        model.addAttribute("user", user);
        return "attendee/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User updated, RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        if (current == null) return "redirect:/login";

        current.setFullName(updated.getFullName());
        current.setInstitution(updated.getInstitution());
        current.setBio(updated.getBio());
        current.setPhone(updated.getPhone());
        userService.updateUser(current);
        ra.addFlashAttribute("success", "Profile updated.");
        return "redirect:/attendee/profile";
    }
}