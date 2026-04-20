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
    private final PaymentService paymentService;
    private final SessionService sessionService;
    private final FeedbackServiceImpl feedbackService;
    private final CertificateServiceImpl certificateService;
    private final MessageServiceImpl messageService;

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

    // ==================== Registration Form Display ====================
    @GetMapping("/conferences/{id}/register")
    public String registerForm(@PathVariable Long id, Model model) {
        Conference conf = conferenceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Conference not found"));
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        if (registrationService.isRegistered(attendee, conf)) {
            return "redirect:/attendee/conferences";
        }

        // Get available workshops (sessions) for this conference
        List<Session> workshops = sessionService.findByConference(conf).stream()
                .filter(s -> s.getType() == Session.SessionType.WORKSHOP)
                .collect(Collectors.toList());

        // Check if user has VIP package
        boolean hasVip = paymentService.hasVipPackage(attendee, conf);
        
        // Get available payment packages
        List<Payment.PackageType> packages = List.of(
                Payment.PackageType.BASIC,
                Payment.PackageType.WORKSHOP_ONLY,
                Payment.PackageType.VIP
        );

        model.addAttribute("conference", conf);
        model.addAttribute("types", Registration.RegistrationType.values());
        model.addAttribute("workshops", workshops);
        model.addAttribute("packages", packages);
        model.addAttribute("hasVipPackage", hasVip);
        model.addAttribute("hasAvailableSpace", registrationService.hasAvailableSpace(conf));
        model.addAttribute("confirmedCount", registrationService.countConfirmedByConference(conf));
        model.addAttribute("waitlistedCount", registrationService.countWaitlistedByConference(conf));
        
        return "attendee/register-conference";
    }

    // ==================== Registration Form Processing (Activity Diagram) ====================
    @PostMapping("/conferences/{id}/register")
    public String doRegister(@PathVariable Long id,
                              @RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam Registration.RegistrationType type,
                              @RequestParam(required = false) String dietary,
                              @RequestParam(required = false) String special,
                              @RequestParam(required = false) String selectedWorkshops,
                              @RequestParam(required = false) String packageType,
                              @RequestParam(defaultValue = "false") String paymentConfirmed,
                              RedirectAttributes ra) {
        User attendee = userService.getCurrentUser();
        if (attendee == null) return "redirect:/login";

        Conference conf = conferenceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        // Step 1: VALIDATE INPUT
        Map<String, String> validationErrors = registrationService.validateRegistrationForm(fullName, email, type.toString());
        
        if (!validationErrors.isEmpty()) {
            // Show error messages
            String errorMsg = String.join("; ", validationErrors.values());
            ra.addFlashAttribute("error", "Form validation failed: " + errorMsg);
            ra.addFlashAttribute("validationErrors", validationErrors);
            return "redirect:/attendee/conferences/" + id + "/register";
        }

        try {
            // PAYMENT VALIDATION: Check payment confirmation for non-BASIC packages
            if (packageType != null && !packageType.equals("BASIC")) {
                if (!"true".equals(paymentConfirmed)) {
                    ra.addFlashAttribute("error", "Payment must be confirmed to complete registration. Please click 'Yes, Pay' to proceed.");
                    return "redirect:/attendee/conferences/" + id + "/register";
                }
            }

            // Step 2: CHECK VIP PACKAGE PURCHASED
            boolean vipPurchased = paymentService.hasVipPackage(attendee, conf);
            
            // Step 3: CHECK WORKSHOP SELECTION
            boolean workshopSelected = selectedWorkshops != null && !selectedWorkshops.trim().isEmpty();
            
            // Step 4: PROCESS PAYMENT (Dummy System)
            if (packageType != null && !packageType.equals("BASIC")) {
                Payment.PackageType pType = Payment.PackageType.valueOf(packageType);
                Payment payment = paymentService.processPayment(attendee, conf, pType);
                // Auto-complete payment in dummy system
                paymentService.completePayment(payment.getId(), null);
                
                vipPurchased = pType == Payment.PackageType.VIP;
                workshopSelected = workshopSelected || (pType == Payment.PackageType.WORKSHOP_ONLY);
            }

            // Step 5: PROCESS REGISTRATION WITH ROLE ASSIGNMENT
            Registration reg = registrationService.processValidatedRegistration(
                    attendee, conf, type, dietary, special, selectedWorkshops, vipPurchased
            );

            // Step 6: PROVIDE ACCESS (Access control via assigned role)
            if (reg.getStatus() == Registration.RegistrationStatus.WAITLISTED) {
                ra.addFlashAttribute("warning", "Added to waitlist (Position: " + 
                    registrationService.countWaitlistedByConference(conf) + 
                    "). Conference is at capacity. You'll be promoted when a spot opens.");
            } else {
                String roleMsg = "Registered as " + reg.getAssignedRole().getDisplayName();
                ra.addFlashAttribute("success", "Successfully registered for " + conf.getName() + "! " + roleMsg);
            }
            
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Registration failed: " + e.getMessage());
        }
        
        return "redirect:/attendee/conferences";
    }

    @PostMapping("/registrations/{id}/cancel")
    public String cancelRegistration(@PathVariable Long id, RedirectAttributes ra) {
        registrationService.cancelRegistration(id);
        ra.addFlashAttribute("success", "Registration cancelled.");
        return "redirect:/attendee/dashboard";
    }

    // ==================== Session/Event Enrollment ====================
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

        // Filter events to only those linked to this conference
        List<Session> availableEvents = sessionService.findByConference(conf);

        model.addAttribute("conference", conf);
        model.addAttribute("events", availableEvents);
        model.addAttribute("attendee", attendee);
        
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

        Conference conf = conferenceService.findById(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        // Access Control: Verify attendee is registered for the conference
        if (!registrationService.isRegistered(attendee, conf)) {
            ra.addFlashAttribute("error", "You must be registered for the conference to join events.");
            return "redirect:/attendee/conferences/" + conferenceId + "/events";
        }

        try {
            sessionService.enrollAttendee(sessionId, attendee);
            ra.addFlashAttribute("success", "Successfully enrolled in the event!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Cannot enroll: " + e.getMessage());
        }
        return "redirect:/attendee/conferences/" + conferenceId + "/events";
    }

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