package com.cms.controller;

import com.cms.model.*;
import com.cms.service.*;
import com.cms.service.impl.FeedbackServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/speaker")
@PreAuthorize("hasRole('SPEAKER')")
@RequiredArgsConstructor
public class SpeakerController {

    private final UserService userService;
    private final SessionService sessionService;
    // FIX: Inject the Interface (FeedbackService), not the Impl
    private final FeedbackServiceImpl feedbackService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User speaker = userService.getCurrentUser();
        if (speaker == null) return "redirect:/login";

        List<Session> sessions = sessionService.findBySpeaker(speaker);
        model.addAttribute("speaker", speaker);
        // PASSING A LIST CALLED 'sessions' IS OK (it's plural)
        model.addAttribute("sessions", sessions != null ? sessions : new ArrayList<>());
        return "speaker/dashboard";
    }

    @GetMapping("/sessions")
    public String mySessions(Model model) {
        User speaker = userService.getCurrentUser();
        model.addAttribute("sessions", sessionService.findBySpeaker(speaker));
        return "speaker/my-sessions";
    }

    @GetMapping("/sessions/{id}")
    public String viewSession(@PathVariable Long id, Model model) {
        Session sessionObject = sessionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        // FIX: Rename attribute to 'sess' to avoid collision with 'session' reserved word
        model.addAttribute("sess", sessionObject); 
        model.addAttribute("feedbackStats", feedbackService.getSessionStats(sessionObject));
        model.addAttribute("feedbacks", feedbackService.getBySession(sessionObject));
        return "speaker/session-detail";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("user", userService.getCurrentUser());
        return "speaker/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User updated, RedirectAttributes ra) {
        User current = userService.getCurrentUser();
        if (current != null) {
            current.setFullName(updated.getFullName());
            current.setInstitution(updated.getInstitution());
            current.setBio(updated.getBio());
            current.setPhone(updated.getPhone());
            userService.updateUser(current);
            ra.addFlashAttribute("success", "Profile updated.");
        }
        return "redirect:/speaker/profile";
    }
}