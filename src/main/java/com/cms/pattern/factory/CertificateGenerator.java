package com.cms.pattern.factory;

import com.cms.model.Certificate;
import com.cms.model.Conference;
import com.cms.model.User;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  DESIGN PATTERN 1: Factory Method (Creational)              ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Intent:                                                     ║
 * ║    Define an interface for creating an object, but let       ║
 * ║    subclasses decide which class to instantiate.             ║
 * ║                                                              ║
 * ║  Why used here:                                              ║
 * ║    Different certificate types (Participation, Presentation, ║
 * ║    Best Paper, Reviewer) each have different content and     ║
 * ║    eligibility rules. Each type is its own ConcreteCreator.  ║
 * ║    Adding a new type never changes existing code (OCP).      ║
 * ║                                                              ║
 * ║  Participants:                                               ║
 * ║    Creator         → CertificateGenerator (this class)      ║
 * ║    ConcreteCreator → ParticipationCertGenerator,            ║
 * ║                      PresentationCertGenerator,             ║
 * ║                      BestPaperCertGenerator,                ║
 * ║                      ReviewerCertGenerator                  ║
 * ║    Product         → CertificateContent (record)            ║
 * ║    Client          → CertificateGeneratorFactory            ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public abstract class CertificateGenerator {

    /**
     * Template method — orchestrates certificate creation.
     * Calls the factory method getCertificateType() internally.
     */
    public final CertificateContent generate(User recipient, Conference conference) {
        if (recipient == null)   throw new IllegalArgumentException("Recipient cannot be null");
        if (conference == null)  throw new IllegalArgumentException("Conference cannot be null");

        String title  = buildTitle();
        String body   = buildBody(recipient, conference);
        String footer = buildFooter(conference);
        return new CertificateContent(title, body, footer, getCertificateType());
    }

    // ── Factory Method — each subclass decides the type ──────────
    public abstract Certificate.CertificateType getCertificateType();

    protected abstract String buildTitle();
    protected abstract String buildBody(User recipient, Conference conference);

    protected String buildFooter(Conference conference) {
        return "Issued at: " + conference.getName()
                + (conference.getStartDate() != null ? " | " + conference.getStartDate() : "");
    }

    protected String institution(User user) {
        return (user.getInstitution() != null && !user.getInstitution().isBlank())
                ? user.getInstitution() : "their institution";
    }

    /** Product — the generated certificate content */
    public record CertificateContent(
            String title,
            String body,
            String footer,
            Certificate.CertificateType type) {
    }
}
