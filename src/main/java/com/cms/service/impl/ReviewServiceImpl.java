package com.cms.service.impl;

import com.cms.model.Paper;
import com.cms.model.Review;
import com.cms.model.User;
import com.cms.pattern.chain.ReviewValidationChain;
import com.cms.pattern.singleton.ConferenceAppConfig;
import com.cms.pattern.chain.ReviewValidationHandler;
import com.cms.repository.PaperRepository;
import com.cms.repository.ReviewRepository;
import com.cms.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Review service — integrates the Chain of Responsibility pattern
 * for review submission validation.
 *
 * Before a review is saved, it passes through a chain of validators:
 *   DeadlineValidationHandler
 *     → ScoreRangeValidationHandler
 *       → CommentsValidationHandler
 *         → RecommendationValidationHandler
 *
 * Each handler either rejects with a reason or passes to the next.
 * If all pass, the review is persisted.
 *
 * DESIGN PRINCIPLE — Open/Closed:
 *   New validation rules = new handler class only.
 *   This service and existing handlers never change.
 *
 * DESIGN PRINCIPLE — Single Responsibility:
 *   Each handler has exactly one validation responsibility.
 *   This class orchestrates; it does not validate.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository     reviewRepository;
    private final PaperRepository      paperRepository;
    private final ReviewValidationChain validationChain;   // ← Chain injected

    @Override
    public Review assignReview(Paper paper, User reviewer) {
        if (reviewRepository.existsByPaperAndReviewer(paper, reviewer)) {
            throw new RuntimeException("Reviewer already assigned to this paper.");
        }
        if (paper.getAuthor().getId().equals(reviewer.getId())) {
            throw new RuntimeException("Author cannot review their own paper.");
        }

        Review review = Review.builder()
                .paper(paper)
                .reviewer(reviewer)
                .status(Review.ReviewStatus.ASSIGNED)
                .deadline(LocalDateTime.now()
                        .plusDays(ConferenceAppConfig.getInstance().getReviewDeadlineDays())
                        .toLocalDate().atStartOfDay())
                .build();

        paper.setStatus(Paper.Status.UNDER_REVIEW);
        paperRepository.save(paper);
        return reviewRepository.save(review);
    }

    @Override
    public Review submitReview(Long reviewId, Review reviewData) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

        // Populate data before validation so handlers can inspect it
        review.setComments(reviewData.getComments());
        review.setStrengthsComments(reviewData.getStrengthsComments());
        review.setWeaknessComments(reviewData.getWeaknessComments());
        review.setScore(reviewData.getScore());
        review.setRecommendation(reviewData.getRecommendation());

        // ── Singleton Pattern: read max score from global config ───
        int maxScore = ConferenceAppConfig.getInstance().getMaxReviewScore();
        int minScore = ConferenceAppConfig.getInstance().getMinReviewScore();
        // (Chain handlers also use these values)
        // ── Chain of Responsibility Pattern ───────────────────────
        boolean pastDeadline = review.getDeadline() != null
                && LocalDateTime.now().isAfter(review.getDeadline());

        ReviewValidationHandler.ReviewContext context =
                new ReviewValidationHandler.ReviewContext(review, pastDeadline);

        ReviewValidationHandler.ValidationResult result =
                validationChain.validate(context);

        if (!result.isValid()) {
            throw new RuntimeException(result.getReason());
        }
        // ─────────────────────────────────────────────────────────

        review.setStatus(Review.ReviewStatus.SUBMITTED);
        review.setSubmittedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        // Auto-decide paper status when all reviews are in
        autoDecidePaperStatus(review.getPaper());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByReviewer(User reviewer) {
        return reviewRepository.findByReviewer(reviewer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByPaper(Paper paper) {
        return reviewRepository.findByPaper(paper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findPendingByReviewer(User reviewer) {
        return reviewRepository.findPendingReviewsByReviewer(reviewer);
    }

    @Override
    public void autoDecidePaperStatus(Paper paper) {
        List<Review> reviews = reviewRepository.findByPaper(paper);
        long submitted = reviews.stream()
                .filter(r -> r.getStatus() == Review.ReviewStatus.SUBMITTED)
                .count();

        if (submitted == reviews.size() && !reviews.isEmpty()) {
            long accepts = reviews.stream()
                    .filter(r -> r.getRecommendation() == Review.Recommendation.ACCEPT).count();
            long rejects = reviews.stream()
                    .filter(r -> r.getRecommendation() == Review.Recommendation.REJECT).count();
            long minors  = reviews.stream()
                    .filter(r -> r.getRecommendation() == Review.Recommendation.MINOR_REVISION).count();

            Paper.Status newStatus;
            if (accepts > rejects + minors)    newStatus = Paper.Status.ACCEPTED;
            else if (rejects > accepts)         newStatus = Paper.Status.REJECTED;
            else                                newStatus = Paper.Status.REVISION_REQUIRED;

            paper.setStatus(newStatus);
            paperRepository.save(paper);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReviewed(Paper paper, User reviewer) {
        return reviewRepository.existsByPaperAndReviewer(paper, reviewer);
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageScore(Paper paper) {
        Double avg = reviewRepository.getAverageScoreForPaper(paper);
        return avg != null ? avg : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(Review.ReviewStatus status) {
        return reviewRepository.countByStatus(status);
    }
}
