package com.cms.pattern.chain;

import com.cms.model.Review;
import org.springframework.stereotype.Component;

// ═══════════════════════════════════════════════════════
// ConcreteHandler 1 — Deadline Validation
// ═══════════════════════════════════════════════════════
@Component
class DeadlineValidationHandler extends ReviewValidationHandler {
    @Override
    protected ValidationResult doValidate(ReviewContext context) {
        if (context.isPastDeadline()) {
            return ValidationResult.fail(
                "Review deadline has passed. Please contact the admin to submit a late review.");
        }
        return ValidationResult.ok();
    }
}

// ═══════════════════════════════════════════════════════
// ConcreteHandler 2 — Score Range Validation
// ═══════════════════════════════════════════════════════
@Component
class ScoreRangeValidationHandler extends ReviewValidationHandler {
    @Override
    protected ValidationResult doValidate(ReviewContext context) {
        Integer score = context.review().getScore();
        if (score == null) {
            return ValidationResult.fail("A numeric score (1–10) is required.");
        }
        if (score < 1 || score > 10) {
            return ValidationResult.fail(
                "Score must be between 1 and 10. Provided value: " + score);
        }
        return ValidationResult.ok();
    }
}

// ═══════════════════════════════════════════════════════
// ConcreteHandler 3 — Comments Validation
// ═══════════════════════════════════════════════════════
@Component
class CommentsValidationHandler extends ReviewValidationHandler {
    private static final int MIN_COMMENT_LENGTH = 20;

    @Override
    protected ValidationResult doValidate(ReviewContext context) {
        Review review = context.review();
        String comments = review.getComments();
        if (comments == null || comments.trim().length() < MIN_COMMENT_LENGTH) {
            return ValidationResult.fail(
                "General comments must be at least " + MIN_COMMENT_LENGTH
                + " characters. Reviewers are expected to provide constructive feedback.");
        }
        return ValidationResult.ok();
    }
}

// ═══════════════════════════════════════════════════════
// ConcreteHandler 4 — Recommendation Validation
// ═══════════════════════════════════════════════════════
@Component
class RecommendationValidationHandler extends ReviewValidationHandler {
    @Override
    protected ValidationResult doValidate(ReviewContext context) {
        Review.Recommendation rec = context.review().getRecommendation();
        if (rec == null || rec == Review.Recommendation.PENDING) {
            return ValidationResult.fail(
                "A recommendation (Accept / Reject / Minor Revision / Major Revision) is required.");
        }
        return ValidationResult.ok();
    }
}

// ═══════════════════════════════════════════════════════
// Chain Builder — assembles the chain in order
// ═══════════════════════════════════════════════════════
/**
 * Builds and returns the complete validation chain.
 * Inject this bean and call validate() to run all checks.
 *
 * Chain order:
 *   Deadline → ScoreRange → Comments → Recommendation
 */
@Component
public class ReviewValidationChain {

    private final ReviewValidationHandler chainHead;

    public ReviewValidationChain(
            DeadlineValidationHandler deadline,
            ScoreRangeValidationHandler scoreRange,
            CommentsValidationHandler comments,
            RecommendationValidationHandler recommendation) {

        // Wire the chain: each handler points to the next
        deadline.setNext(scoreRange)
                .setNext(comments)
                .setNext(recommendation);

        this.chainHead = deadline;   // entry point
    }

    /**
     * Runs the review through the full validation chain.
     *
     * @param context  review + metadata
     * @return ValidationResult — ok if all pass, fail with reason otherwise
     */
    public ReviewValidationHandler.ValidationResult validate(
            ReviewValidationHandler.ReviewContext context) {
        return chainHead.validate(context);
    }
}
