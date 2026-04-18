package com.cms.pattern.builder;

import com.cms.model.Session;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  DESIGN PATTERN 2 : Builder  (Creational)                   ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Intent :                                                    ║
 * ║    Separate the construction of a complex object from its    ║
 * ║    representation so the same construction process can       ║
 * ║    create different representations.                         ║
 * ║                                                              ║
 * ║  Why used here :                                             ║
 * ║    A ConferenceSchedule is complex — it has a title,         ║
 * ║    description, date range, multiple tracks, multiple        ║
 * ║    sessions per track, and venue. Using a constructor with   ║
 * ║    all these params is unreadable and error-prone.           ║
 * ║    The Builder lets clients compose a schedule fluently,     ║
 * ║    step-by-step, only supplying what they need.              ║
 * ║                                                              ║
 * ║  Participants :                                              ║
 * ║    Product   → ConferenceSchedule                            ║
 * ║    Builder   → ConferenceSchedule.Builder (inner class)      ║
 * ║    Director  → ScheduleDirector                              ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class ConferenceSchedule {

    // ── Product fields ────────────────────────────────────────────
    private final String          title;
    private final String          venue;
    private final LocalDate       startDate;
    private final LocalDate       endDate;
    private final List<String>    tracks;
    private final List<Session>   sessions;
    private final String          notes;
    private final boolean         published;

    /** Private constructor — only the Builder can create instances */
    private ConferenceSchedule(Builder builder) {
        this.title     = builder.title;
        this.venue     = builder.venue;
        this.startDate = builder.startDate;
        this.endDate   = builder.endDate;
        this.tracks    = Collections.unmodifiableList(builder.tracks);
        this.sessions  = Collections.unmodifiableList(builder.sessions);
        this.notes     = builder.notes;
        this.published = builder.published;
    }

    // ── Getters ───────────────────────────────────────────────────
    public String        getTitle()     { return title; }
    public String        getVenue()     { return venue; }
    public LocalDate     getStartDate() { return startDate; }
    public LocalDate     getEndDate()   { return endDate; }
    public List<String>  getTracks()    { return tracks; }
    public List<Session> getSessions()  { return sessions; }
    public String        getNotes()     { return notes; }
    public boolean       isPublished()  { return published; }

    public int getTotalSessionCount() { return sessions.size(); }

    @Override
    public String toString() {
        return "ConferenceSchedule{title='" + title + "', sessions=" +
               sessions.size() + ", tracks=" + tracks.size() +
               ", published=" + published + '}';
    }

    // ═══════════════════════════════════════════════════════════════
    // Builder — fluent API for step-by-step construction
    // ═══════════════════════════════════════════════════════════════
    public static class Builder {

        // Required fields
        private final String title;

        // Optional fields with defaults
        private String        venue     = "";
        private LocalDate     startDate = LocalDate.now();
        private LocalDate     endDate   = LocalDate.now();
        private List<String>  tracks    = new ArrayList<>();
        private List<Session> sessions  = new ArrayList<>();
        private String        notes     = "";
        private boolean       published = false;

        public Builder(String title) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Schedule title is required.");
            }
            this.title = title;
        }

        public Builder venue(String venue) {
            this.venue = venue;
            return this;
        }

        public Builder dateRange(LocalDate start, LocalDate end) {
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("End date cannot be before start date.");
            }
            this.startDate = start;
            this.endDate   = end;
            return this;
        }

        public Builder addTrack(String trackName) {
            this.tracks.add(trackName);
            return this;
        }

        public Builder addSession(Session session) {
            this.sessions.add(session);
            return this;
        }

        public Builder addSessions(List<Session> sessions) {
            this.sessions.addAll(sessions);
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder publish() {
            this.published = true;
            return this;
        }

        /** Terminal method — validates and constructs the product */
        public ConferenceSchedule build() {
            return new ConferenceSchedule(this);
        }
    }
}
