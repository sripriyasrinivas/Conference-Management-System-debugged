package com.cms.service;

import com.cms.model.User;
import com.cms.model.Paper;

/**
 * Notification Service Interface
 * Handles author notifications for paper decisions and status updates.
 * Follows the Activity Diagram: "Notify Author" step.
 */
public interface NotificationService {
    
    /**
     * Notify author when paper is ACCEPTED
     */
    void notifyAuthorAccepted(Paper paper);
    
    /**
     * Notify author when paper is REJECTED
     */
    void notifyAuthorRejected(Paper paper);
    
    /**
     * Notify author when paper REVISION_REQUIRED
     */
    void notifyAuthorRevisionRequired(Paper paper, String adminComments);
    
    /**
     * Send general notification to a user
     */
    void sendNotification(User user, String subject, String message);
}
