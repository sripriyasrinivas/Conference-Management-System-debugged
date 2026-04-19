package com.cms.service.impl;

import com.cms.model.Conference;
import com.cms.repository.ConferenceRepository;
import com.cms.service.ConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConferenceServiceImpl implements ConferenceService {

    private final ConferenceRepository conferenceRepository;

    @Override
    public Conference create(Conference conference) {
        // Validate start date is before end date
        if (conference.getStartDate() != null && conference.getEndDate() != null) {
            if (conference.getStartDate().isAfter(conference.getEndDate()) || conference.getStartDate().equals(conference.getEndDate())) {
                throw new RuntimeException("Conference start date must be before end date.");
            }
        }
        
        // Validate deadline sequence
        if (conference.getSubmissionDeadline() != null && conference.getStartDate() != null) {
            if (conference.getSubmissionDeadline().isAfter(conference.getStartDate()) || conference.getSubmissionDeadline().equals(conference.getStartDate())) {
                throw new RuntimeException("Submission deadline must be before conference start date.");
            }
        }
        
        if (conference.getReviewDeadline() != null && conference.getSubmissionDeadline() != null) {
            if (conference.getReviewDeadline().isBefore(conference.getSubmissionDeadline()) || conference.getReviewDeadline().equals(conference.getSubmissionDeadline())) {
                throw new RuntimeException("Review deadline must be after submission deadline.");
            }
        }
        
        if (conference.getReviewDeadline() != null && conference.getStartDate() != null) {
            if (conference.getReviewDeadline().isAfter(conference.getStartDate()) || conference.getReviewDeadline().equals(conference.getStartDate())) {
                throw new RuntimeException("Review deadline must be before conference start date.");
            }
        }
        
        if (conference.getNotificationDate() != null && conference.getReviewDeadline() != null) {
            if (conference.getNotificationDate().isBefore(conference.getReviewDeadline()) || conference.getNotificationDate().equals(conference.getReviewDeadline())) {
                throw new RuntimeException("Notification date must be after review deadline.");
            }
        }
        
        if (conference.getNotificationDate() != null && conference.getStartDate() != null) {
            if (conference.getNotificationDate().isAfter(conference.getStartDate()) || conference.getNotificationDate().equals(conference.getStartDate())) {
                throw new RuntimeException("Notification date must be before conference start date.");
            }
        }
        
        return conferenceRepository.save(conference);
    }

    @Override
    public Conference update(Long id, Conference updated) {
        // Validate start date is before end date
        if (updated.getStartDate() != null && updated.getEndDate() != null) {
            if (updated.getStartDate().isAfter(updated.getEndDate()) || updated.getStartDate().equals(updated.getEndDate())) {
                throw new RuntimeException("Conference start date must be before end date.");
            }
        }
        
        // Validate deadline sequence
        if (updated.getSubmissionDeadline() != null && updated.getStartDate() != null) {
            if (updated.getSubmissionDeadline().isAfter(updated.getStartDate()) || updated.getSubmissionDeadline().equals(updated.getStartDate())) {
                throw new RuntimeException("Submission deadline must be before conference start date.");
            }
        }
        
        if (updated.getReviewDeadline() != null && updated.getSubmissionDeadline() != null) {
            if (updated.getReviewDeadline().isBefore(updated.getSubmissionDeadline()) || updated.getReviewDeadline().equals(updated.getSubmissionDeadline())) {
                throw new RuntimeException("Review deadline must be after submission deadline.");
            }
        }
        
        if (updated.getReviewDeadline() != null && updated.getStartDate() != null) {
            if (updated.getReviewDeadline().isAfter(updated.getStartDate()) || updated.getReviewDeadline().equals(updated.getStartDate())) {
                throw new RuntimeException("Review deadline must be before conference start date.");
            }
        }
        
        if (updated.getNotificationDate() != null && updated.getReviewDeadline() != null) {
            if (updated.getNotificationDate().isBefore(updated.getReviewDeadline()) || updated.getNotificationDate().equals(updated.getReviewDeadline())) {
                throw new RuntimeException("Notification date must be after review deadline.");
            }
        }
        
        if (updated.getNotificationDate() != null && updated.getStartDate() != null) {
            if (updated.getNotificationDate().isAfter(updated.getStartDate()) || updated.getNotificationDate().equals(updated.getStartDate())) {
                throw new RuntimeException("Notification date must be before conference start date.");
            }
        }
        
        Conference existing = conferenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conference not found: " + id));
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setVenue(updated.getVenue());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setSubmissionDeadline(updated.getSubmissionDeadline());
        existing.setReviewDeadline(updated.getReviewDeadline());
        existing.setNotificationDate(updated.getNotificationDate());
        existing.setMaxAttendees(updated.getMaxAttendees());
        existing.setWebsiteUrl(updated.getWebsiteUrl());
        existing.setContactEmail(updated.getContactEmail());
        return conferenceRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conference> findById(Long id) {
        return conferenceRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conference> findAll() {
        return conferenceRepository.findByOrderByStartDateDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conference> findByStatus(Conference.ConferenceStatus status) {
        return conferenceRepository.findByStatus(status);
    }

    @Override
    public Conference updateStatus(Long id, Conference.ConferenceStatus status) {
        Conference c = conferenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conference not found: " + id));
        c.setStatus(status);
        return conferenceRepository.save(c);
    }

    @Override
    public void delete(Long id) {
        conferenceRepository.deleteById(id);
    }
}
