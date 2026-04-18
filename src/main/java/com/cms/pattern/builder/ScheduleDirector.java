package com.cms.pattern.builder;

import com.cms.model.Session;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Director — knows how to construct common schedule configurations
 * using ConferenceSchedule.Builder.
 *
 * The Director encapsulates construction logic so callers don't
 * need to know the exact build steps for each schedule type.
 * They just call the right Director method.
 *
 * This is the "Director" participant in the Builder pattern.
 */
@Component
public class ScheduleDirector {

    /**
     * Builds a full published conference schedule with all sessions
     * and tracks grouped automatically from the session list.
     */
    public ConferenceSchedule buildFullSchedule(String conferenceName,
                                                 String venue,
                                                 LocalDate start,
                                                 LocalDate end,
                                                 List<Session> sessions) {

        ConferenceSchedule.Builder builder =
                new ConferenceSchedule.Builder(conferenceName + " — Schedule")
                        .venue(venue)
                        .dateRange(start, end)
                        .addSessions(sessions)
                        .notes("Official published schedule. Subject to change.");

        // Auto-extract distinct tracks from sessions
        sessions.stream()
                .map(Session::getTrack)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .forEach(builder::addTrack);

        return builder.publish().build();
    }

    /**
     * Builds a draft (unpublished) schedule for admin preview.
     */
    public ConferenceSchedule buildDraftSchedule(String conferenceName,
                                                  List<Session> sessions) {
        return new ConferenceSchedule.Builder(conferenceName + " — Draft Schedule")
                .addSessions(sessions)
                .notes("DRAFT — Not published to attendees.")
                .build();   // note: no .publish()
    }

    /**
     * Builds a single-day schedule for a specific date.
     */
    public ConferenceSchedule buildDaySchedule(String conferenceName,
                                                String venue,
                                                LocalDate day,
                                                List<Session> daySessions) {
        return new ConferenceSchedule.Builder(
                conferenceName + " — Day Schedule (" + day + ")")
                .venue(venue)
                .dateRange(day, day)
                .addSessions(daySessions)
                .publish()
                .build();
    }
}
