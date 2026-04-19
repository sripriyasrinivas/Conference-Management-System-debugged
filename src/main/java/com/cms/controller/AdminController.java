package com.cms.controller;

import com.cms.model.*;
import com.cms.pattern.facade.ConferenceFacade;
import com.cms.service.*;
import com.cms.service.impl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin MVC Controller.
 *
 * Complex multi-service operations are delegated to ConferenceFacade
 * (Facade pattern) — e.g. closeConference, publishSchedule, summary.
 * Simple single-service operations are called directly.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    // Individual services (for simple, single-service operations)
    private final UserService            userService;
    private final PaperService           paperService;
    private final ReviewService          reviewService;
    private final SessionService         sessionService;
    private final ConferenceService      conferenceService;
    private final RegistrationService    registrationService;
    private final CertificateServiceImpl certificateService;
    private final FeedbackServiceImpl    feedbackService;
    private final AbstractServiceImpl    abstractService;

    // Facade (for complex multi-service operations)
    private final ConferenceFacade       conferenceFacade;

    // ── Dashboard ─────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers",        userService.findAll().size());
        model.addAttribute("totalPapers",       paperService.findAll().size());
        model.addAttribute("totalSessions",     sessionService.findAll().size());
        model.addAttribute("totalConferences",  conferenceService.findAll().size());
        model.addAttribute("submittedPapers",   paperService.countByStatus(Paper.Status.SUBMITTED));
        model.addAttribute("underReviewPapers", paperService.countByStatus(Paper.Status.UNDER_REVIEW));
        model.addAttribute("acceptedPapers",    paperService.countByStatus(Paper.Status.ACCEPTED));
        model.addAttribute("rejectedPapers",    paperService.countByStatus(Paper.Status.REJECTED));
        model.addAttribute("pendingReviews",    reviewService.countByStatus(Review.ReviewStatus.ASSIGNED));
        
        // Safety check for stream limit
        List<Paper> all = paperService.findAll();
        model.addAttribute("recentPapers",      all.stream().limit(Math.min(all.size(), 5)).toList());
        
        model.addAttribute("conferences",       conferenceService.findAll());
        return "admin/dashboard";
    }

    // ── Conference Summary via Facade ─────────────────────────────
    @GetMapping("/conferences/{id}/summary")
    public String conferenceSummary(@PathVariable Long id, Model model) {
        // Facade aggregates data from 4 services in one call
        Map<String, Object> summary = conferenceFacade.getConferenceSummary(id);
        model.addAllAttributes(summary);
        return "admin/conference-summary";
    }

    // ── Close Conference via Facade ────────────────────────────────
    @PostMapping("/conferences/{id}/close")
    public String closeConference(@PathVariable Long id, RedirectAttributes ra) {
        try {
            // Facade: issues all certs + marks COMPLETED in one call
            String result = conferenceFacade.closeConference(id);
            ra.addFlashAttribute("success", result);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/conferences";
    }

    // ── Publish Schedule via Facade ────────────────────────────────
    @PostMapping("/conferences/{id}/publish-schedule")
    public String publishSchedule(@PathVariable Long id, RedirectAttributes ra) {
        try {
            // Facade: builds schedule (Builder pattern) + updates status
            var schedule = conferenceFacade.publishSchedule(id);
            ra.addFlashAttribute("success",
                "Schedule published! " + schedule.getTotalSessionCount() + " sessions included.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/conferences";
    }

    // ── User Management ───────────────────────────────────────────
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        // Pass the logged-in user's ID to the template
        model.addAttribute("currentUserId", userService.getCurrentUser().getId());
        return "admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        User currentUser = userService.getCurrentUser();
        
        // SERVER-SIDE GUARD: Prevent self-deletion
        if (currentUser != null && currentUser.getId().equals(id)) {
            ra.addFlashAttribute("error", "Security Violation: You cannot delete your own account.");
            return "redirect:/admin/users";
        }

        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "User deleted successfully.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ── Paper Management ──────────────────────────────────────────
    @GetMapping("/papers")
    public String listPapers(@RequestParam(required = false) Paper.Status status, Model model) {
        List<Paper> papers;
        
        // FIXED: Now checks if a status was requested in the URL
        if (status != null) {
            papers = paperService.findByStatus(status);
            model.addAttribute("currentStatus", status.name());
        } else {
            papers = paperService.findAll();
            model.addAttribute("currentStatus", "ALL");
        }
        
        model.addAttribute("papers", papers);
        return "admin/papers";
    }
    
    @GetMapping("/papers/{id}")
    public String viewPaper(@PathVariable Long id, Model model) {
        Paper paper = paperService.findById(id)
                .orElseThrow(() -> new RuntimeException("Paper not found"));
        model.addAttribute("paper",     paper);
        model.addAttribute("reviews",   reviewService.findByPaper(paper));
        model.addAttribute("reviewers", userService.findByRole(User.Role.REVIEWER));
        model.addAttribute("avgScore",  reviewService.getAverageScore(paper));
        return "admin/paper-detail";
    }

    @PostMapping("/papers/{id}/assign-reviewer")
    public String assignReviewer(@PathVariable Long id,
                                  @RequestParam Long reviewerId,
                                  RedirectAttributes ra) {
        Paper paper    = paperService.findById(id)
                .orElseThrow(() -> new RuntimeException("Paper not found"));
        User  reviewer = userService.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));
        try {
            reviewService.assignReview(paper, reviewer);
            ra.addFlashAttribute("success", "Reviewer assigned successfully.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/papers/" + id;
    }

    @PostMapping("/papers/{id}/status")
    public String updatePaperStatus(@PathVariable Long id,
                                     @RequestParam Paper.Status status,
                                     @RequestParam(required = false) String comments,
                                     RedirectAttributes ra) {
        paperService.updateStatus(id, status, comments);
        ra.addFlashAttribute("success", "Paper status updated to " + status.getDisplayName());
        return "redirect:/admin/papers/" + id;
    }

    // ── Conference Management ─────────────────────────────────────
    @GetMapping("/conferences")
    public String listConferences(Model model) {
        model.addAttribute("conferences",    conferenceService.findAll());
        model.addAttribute("newConference",  new Conference());
        return "admin/conferences";
    }

    @PostMapping("/conferences/create")
    public String createConference(@ModelAttribute Conference conference,
                                    RedirectAttributes ra) {
        conference.setOrganizer(userService.getCurrentUser());
        conferenceService.create(conference);
        ra.addFlashAttribute("success", "Conference created successfully.");
        return "redirect:/admin/conferences";
    }

    @GetMapping("/conferences/{id}/edit")
    public String editConference(@PathVariable Long id, Model model) {
        model.addAttribute("conference",
                conferenceService.findById(id)
                        .orElseThrow(() -> new RuntimeException("Conference not found")));
        return "admin/conference-edit";
    }

    @PostMapping("/conferences/{id}/edit")
    public String updateConference(@PathVariable Long id,
                                    @ModelAttribute Conference conference,
                                    RedirectAttributes ra) {
        conferenceService.update(id, conference);
        ra.addFlashAttribute("success", "Conference updated.");
        return "redirect:/admin/conferences";
    }

    @PostMapping("/conferences/{id}/status")
    public String updateConferenceStatus(@PathVariable Long id,
                                          @RequestParam Conference.ConferenceStatus status,
                                          RedirectAttributes ra) {
        conferenceService.updateStatus(id, status);
        ra.addFlashAttribute("success", "Conference status updated.");
        return "redirect:/admin/conferences";
    }

    // ── Session Management ────────────────────────────────────────
    @GetMapping("/sessions")
    public String listSessions(Model model) {
        model.addAttribute("sessions",       sessionService.findAll());
        model.addAttribute("newSession",     new Session());
        model.addAttribute("speakers",       userService.findByRole(User.Role.SPEAKER));
        model.addAttribute("acceptedPapers", paperService.findByStatus(Paper.Status.ACCEPTED));
        return "admin/sessions";
    }

    @PostMapping("/sessions/create")
    public String createSession(@ModelAttribute Session session,
                                 @RequestParam(required = false) Long speakerId,
                                 @RequestParam(required = false) Long paperId,
                                 RedirectAttributes ra) {
        if (speakerId != null) userService.findById(speakerId).ifPresent(session::setSpeaker);
        if (paperId   != null) paperService.findById(paperId).ifPresent(session::setPaper);
        try {
            sessionService.createSession(session);
            ra.addFlashAttribute("success", "Session created successfully.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/sessions";
    }

    @PostMapping("/sessions/{id}/delete")
    public String deleteSession(@PathVariable Long id, RedirectAttributes ra) {
        sessionService.deleteSession(id);
        ra.addFlashAttribute("success", "Session deleted.");
        return "redirect:/admin/sessions";
    }

    // ── Certificates ──────────────────────────────────────────────
    // ── Certificates ──────────────────────────────────────────────
    @GetMapping("/certificates")
    public String certificatesPage(Model model) {
        model.addAttribute("conferences",   conferenceService.findAll());
        model.addAttribute("certificates",  certificateService.findAll());
        
        // FIX: Add the users list so the "Individual Issue" dropdown actually has data
        model.addAttribute("users", userService.findAll()); 
        
        return "admin/certificates";
    }

    @PostMapping("/certificates/issue")
    public String issueCertificate(@RequestParam(required = false) Long userId,
                                   @RequestParam(required = false) Long conferenceId,
                                   @RequestParam(required = false) Certificate.CertificateType type,
                                   RedirectAttributes ra) {
        
        // FIX: Safety check to avoid "Something went wrong" page if user didn't select something
        if (userId == null || conferenceId == null || type == null) {
            ra.addFlashAttribute("error", "Please select a recipient, conference, and certificate type.");
            return "redirect:/admin/certificates";
        }

        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Conference conf = conferenceService.findById(conferenceId)
                    .orElseThrow(() -> new RuntimeException("Conference not found"));
            
            certificateService.issueCertificate(user, conf, type);
            ra.addFlashAttribute("success", type.getTitle() + " issued successfully to " + user.getFullName());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Error issuing certificate: " + e.getMessage());
        }
        return "redirect:/admin/certificates";
    }

    @PostMapping("/certificates/bulk-issue")
    public String bulkIssueCertificates(@RequestParam Long conferenceId,
                                         RedirectAttributes ra) {
        Conference conf = conferenceService.findById(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));
        certificateService.bulkIssueParticipationCertificates(conf);
        ra.addFlashAttribute("success",
                "Participation certificates issued to all confirmed attendees.");
        return "redirect:/admin/certificates";
    }

    // ── Abstract Management ───────────────────────────────────────
    @GetMapping("/abstracts")
    public String listAbstracts(Model model) {
        model.addAttribute("abstracts", abstractService.findAll());
        return "admin/abstracts";
    }

    @PostMapping("/abstracts/{id}/review")
    public String reviewAbstract(@PathVariable Long id,
                                  @RequestParam Abstract.AbstractStatus status,
                                  @RequestParam(required = false) String comments,
                                  RedirectAttributes ra) {
        abstractService.review(id, status, comments);
        ra.addFlashAttribute("success", "Abstract status updated.");
        return "redirect:/admin/abstracts";
    }

    // ── Reports ───────────────────────────────────────────────────
    @GetMapping("/reports")
    public String reportsPage(Model model) {
        model.addAttribute("sessionStats", feedbackService.getAllSessionStats());

        List<Paper> allPapers = paperService.findAll();
        model.addAttribute("papers", allPapers);

        Map<Long, Double> paperAverages = allPapers.stream()
                .collect(Collectors.toMap(
                        Paper::getId,
                        p -> p.getReviews().stream()
                                .filter(r -> r.getScore() != null)
                                .mapToInt(Review::getScore)
                                .average().orElse(0.0)
                ));
        model.addAttribute("paperAverages", paperAverages);

        List<Conference> allConferences = conferenceService.findAll();
        model.addAttribute("conferences", allConferences);

        if (!allConferences.isEmpty()) {
            Conference latest = allConferences.get(0);
            model.addAttribute("latestConf",      latest);
            model.addAttribute("registrations",   registrationService.findByConference(latest));
            model.addAttribute("confirmedCount",  registrationService.countConfirmedByConference(latest));
        }
        return "admin/reports";
    }
}
