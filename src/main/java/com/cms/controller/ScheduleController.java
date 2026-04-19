package com.cms.controller;

import com.cms.pattern.builder.ConferenceSchedule;
import com.cms.pattern.builder.ScheduleDirector;
import com.cms.service.ConferenceService;
import com.cms.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Schedule Controller — uses the Builder pattern via ScheduleDirector
 * to construct ConferenceSchedule objects for different view types.
 */
@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final SessionService     sessionService;
    private final ConferenceService  conferenceService;
    private final ScheduleDirector   scheduleDirector;  // Builder Director

    @GetMapping
    public String viewSchedule(@RequestParam(required = false) String view,
                                Model model) {
        // ── Builder Pattern: Director builds the schedule ─────────
        var conferences = conferenceService.findAll();
        var sessions    = sessionService.findAll();

        if (!conferences.isEmpty() && !sessions.isEmpty()) {
            var conf = conferences.get(0);

            // Director chooses the right build steps for the view type
            ConferenceSchedule schedule = ("draft".equals(view))
                    ? scheduleDirector.buildDraftSchedule(conf.getName(), sessions)
                    : scheduleDirector.buildFullSchedule(
                            conf.getName(),
                            conf.getVenue() != null ? conf.getVenue() : "",
                            conf.getStartDate(), conf.getEndDate(),
                            sessions);

            model.addAttribute("builtSchedule", schedule);
        }
        // ─────────────────────────────────────────────────────────

        model.addAttribute("schedule", sessionService.getScheduleGroupedByDate());
        model.addAttribute("tracks",   sessionService.getAllTracks());
        model.addAttribute("allSessions", sessions);
        return "common/schedule";
    }
}
