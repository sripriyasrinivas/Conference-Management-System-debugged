package com.cms.service.impl;

import com.cms.model.User;
import com.cms.model.Paper;
import com.cms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * Notification Service Implementation
 * Sends notifications to authors about paper decisions.
 * 
 * Currently implements a CONSOLE/LOG-based notification system.
 * Can be extended to support:
 *   - Email notifications
 *   - In-app notifications (notification table)
 *   - SMS notifications
 *   - Push notifications
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notifyAuthorAccepted(Paper paper) {
        if (paper == null || paper.getAuthor() == null) {
            return;
        }
        
        String subject = "Paper Accepted: " + paper.getTitle();
        String message = String.format(
            "Congratulations! Your paper '%s' has been ACCEPTED for the conference. " +
            "Average review score: %.1f/10. " +
            "Please check the system for reviewer feedback.",
            paper.getTitle(),
            calculateAverageScore(paper)
        );
        
        sendNotification(paper.getAuthor(), subject, message);
    }

    @Override
    public void notifyAuthorRejected(Paper paper) {
        if (paper == null || paper.getAuthor() == null) {
            return;
        }
        
        String subject = "Paper Rejected: " + paper.getTitle();
        String message = String.format(
            "Unfortunately, your paper '%s' has been REJECTED. " +
            "Average review score: %.1f/10. " +
            "Please review the feedback from reviewers in the system and consider submitting to another venue.",
            paper.getTitle(),
            calculateAverageScore(paper)
        );
        
        sendNotification(paper.getAuthor(), subject, message);
    }

    @Override
    public void notifyAuthorRevisionRequired(Paper paper, String adminComments) {
        if (paper == null || paper.getAuthor() == null) {
            return;
        }
        
        String subject = "Revision Required: " + paper.getTitle();
        String message = String.format(
            "Your paper '%s' requires REVISION. " +
            "Average review score: %.1f/10. " +
            "Admin Comments: %s " +
            "Please revise and resubmit through the system.",
            paper.getTitle(),
            calculateAverageScore(paper),
            (adminComments != null ? adminComments : "See reviewer feedback for details")
        );
        
        sendNotification(paper.getAuthor(), subject, message);
    }

    @Override
    public void sendNotification(User user, String subject, String message) {
        if (user == null || user.getEmail() == null) {
            log.warn("Cannot send notification: User or email is null");
            return;
        }
        
        // LOG-BASED NOTIFICATION (Console output for development)
        log.info("════════════════════════════════════════════════════════════");
        log.info("NOTIFICATION SENT TO: {} ({})", user.getUsername(), user.getEmail());
        log.info("SUBJECT: {}", subject);
        log.info("MESSAGE: {}", message);
        log.info("SENT AT: {}", LocalDateTime.now());
        log.info("════════════════════════════════════════════════════════════");
        
        // TODO: Implement actual email sending
        // Example with Spring Mail:
        // SimpleMailMessage mailMessage = new SimpleMailMessage();
        // mailMessage.setTo(user.getEmail());
        // mailMessage.setSubject(subject);
        // mailMessage.setText(message);
        // mailSender.send(mailMessage);
    }

    /**
     * Calculate average score from all reviews
     */
    private double calculateAverageScore(Paper paper) {
        if (paper.getReviews() == null || paper.getReviews().isEmpty()) {
            return 0.0;
        }
        
        return paper.getReviews().stream()
                .filter(r -> r.getScore() != null)
                .mapToInt(r -> r.getScore())
                .average()
                .orElse(0.0);
    }
}
