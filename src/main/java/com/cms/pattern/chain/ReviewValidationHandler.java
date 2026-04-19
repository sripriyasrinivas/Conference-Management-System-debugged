package com.cms.pattern.chain;

import com.cms.model.Review;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  DESIGN PATTERN 3: Chain of Responsibility (Behavioral)     ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Intent:                                                     ║
 * ║    Avoid coupling the sender of a request to its receiver    ║
 * ║    by giving more than one object a chance to handle the     ║
 * ║    request. Chain the receiving objects and pass the request ║
 * ║    along the chain until an object handles it.               ║
 * ║                                                              ║
 * ║  Why used here:                                              ║
 * ║    When a reviewer submits a review, it must pass through    ║
 * ║    multiple validation checks before being accepted:         ║
 * ║      1. DeadlineValidationHandler  — is submission past due? ║
 * ║      2. ScoreRangeValidationHandler — is score 1–10?        ║
 * ║      3. CommentsValidationHandler  — is feedback provided?  ║
 * ║      4. RecommendationHandler      — is recommendation set?  ║
 * ║    Each handler either rejects the review (with a reason)    ║
 * ║    or passes it to the next handler in the chain.            ║
 * ║                                                              ║
 * ║  Participants:                                               ║
 * ║    Handler         → ReviewValidationHandler (this)         ║
 * ║    ConcreteHandler → DeadlineValidationHandler,             ║
 * ║                      ScoreRangeValidationHandler,           ║
 * ║                      CommentsValidationHandler,             ║
 * ║                      RecommendationValidationHandler        ║
 * ║    Client          → ReviewServiceImpl                      ║
 * ║                                                              ║
 * ║  DESIGN PRINCIPLE demonstrated:                              ║
 * ║    Open/Closed — new validation rules are added by creating  ║
 * ║    a new handler and inserting it in the chain. Existing     ║
 * ║    handlers and ReviewServiceImpl never change.              ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public abstract class ReviewValidationHandler {

    /** Reference to the next handler in the chain */
    private ReviewValidationHandler next;

    /** Sets the next handler; returns it to allow fluent chaining */
    public ReviewValidationHandler setNext(ReviewValidationHandler next) {
        this.next = next;
        return next;
    }

    /**
     * Validates the review context. If this handler's check passes,
     * it delegates to the next handler (if any). If a check fails,
     * it returns a ValidationResult with the failure reason.
     *
     * @param context the review being submitted
     * @return ValidationResult — ok() if all checks pass, fail(reason) otherwise
     */
    public final ValidationResult validate(ReviewContext context) {
        ValidationResult result = doValidate(context);
        if (!result.isValid()) {
            return result;                    // stop chain on first failure
        }
        if (next != null) {
            return next.validate(context);    // pass along the chain
        }
        return ValidationResult.ok();         // end of chain — all passed
    }

    /** Concrete handlers implement their specific check here */
    protected abstract ValidationResult doValidate(ReviewContext context);

    // ── Value objects ─────────────────────────────────────────────

    /** Carries the review data through the chain */
    public record ReviewContext(
            Review review,
            boolean isPastDeadline
    ) {}

    /** Result returned by the chain */
    public static class ValidationResult {
        private final boolean valid;
        private final String  reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid  = valid;
            this.reason = reason;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid()  { return valid; }
        public String  getReason() { return reason; }
    }
}
