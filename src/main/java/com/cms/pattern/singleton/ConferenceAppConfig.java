package com.cms.pattern.singleton;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  DESIGN PATTERN 1 : Singleton  (Creational)                 ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Intent :                                                    ║
 * ║    Ensure a class has only ONE instance and provide a        ║
 * ║    global access point to it.                               ║
 * ║                                                              ║
 * ║  Why used here :                                             ║
 * ║    ConferenceAppConfig holds application-wide settings       ║
 * ║    (submission limits, review deadlines, max reviewers)      ║
 * ║    that must be consistent across the entire system.         ║
 * ║    Multiple instances would cause inconsistent behaviour.    ║
 * ║                                                              ║
 * ║  Implementation :                                            ║
 * ║    Thread-safe Bill Pugh / Initialization-on-demand holder.  ║
 * ║    The inner static class is loaded only when getInstance()  ║
 * ║    is first called — lazy, thread-safe, no synchronisation   ║
 * ║    overhead.                                                 ║
 * ║                                                              ║
 * ║  Participants :                                              ║
 * ║    Singleton → ConferenceAppConfig (this class)             ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public final class ConferenceAppConfig {

    // ── Configuration fields ──────────────────────────────────────
    private int    maxReviewersPerPaper   = 3;
    private int    minReviewScore         = 1;
    private int    maxReviewScore         = 10;
    private int    reviewDeadlineDays     = 14;
    private int    maxAbstractWords       = 500;
    private int    maxFileSizeMb          = 10;
    private String appName                = "Conference Management System";
    private String supportEmail           = "support@cms.com";

    // ── Private constructor prevents external instantiation ───────
    private ConferenceAppConfig() { }

    // ── Initialization-on-demand holder (thread-safe lazy init) ───
    private static final class Holder {
        private static final ConferenceAppConfig INSTANCE =
                new ConferenceAppConfig();
    }

    /**
     * Global access point to the single instance.
     * Thread-safe without synchronisation overhead.
     */
    public static ConferenceAppConfig getInstance() {
        return Holder.INSTANCE;
    }

    // ── Getters ───────────────────────────────────────────────────

    public int    getMaxReviewersPerPaper()  { return maxReviewersPerPaper; }
    public int    getMinReviewScore()        { return minReviewScore; }
    public int    getMaxReviewScore()        { return maxReviewScore; }
    public int    getReviewDeadlineDays()    { return reviewDeadlineDays; }
    public int    getMaxAbstractWords()      { return maxAbstractWords; }
    public int    getMaxFileSizeMb()         { return maxFileSizeMb; }
    public String getAppName()               { return appName; }
    public String getSupportEmail()          { return supportEmail; }

    // ── Setters (admin-configurable at runtime) ───────────────────

    public void setMaxReviewersPerPaper(int n)  { this.maxReviewersPerPaper = n; }
    public void setReviewDeadlineDays(int days) { this.reviewDeadlineDays = days; }
    public void setMaxAbstractWords(int words)  { this.maxAbstractWords = words; }
    public void setSupportEmail(String email)   { this.supportEmail = email; }

    @Override
    public String toString() {
        return "ConferenceAppConfig{" +
               "maxReviewersPerPaper=" + maxReviewersPerPaper +
               ", reviewDeadlineDays=" + reviewDeadlineDays  +
               ", maxReviewScore="     + maxReviewScore       +
               ", appName='"          + appName + '\''        +
               '}';
    }
}
