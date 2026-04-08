package com.cms.service;

import com.cms.model.*;
import com.cms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DataInitializerService {

    private final UserRepository userRepository;
    private final ConferenceRepository conferenceRepository;
    private final PaperRepository paperRepository;
    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final RegistrationRepository registrationRepository;
    private final AbstractRepository abstractRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void initializeData() {
        // Prevent duplicate initialization
        if (userRepository.count() > 0) return;

        // ── Users ──────────────────────────────────────────────────────────────
        User admin = userRepository.save(User.builder()
                .username("admin").email("admin@cms.com").fullName("Admin User")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ADMIN).institution("CMS HQ").enabled(true).build());

        User author1 = userRepository.save(User.builder()
                .username("alice").email("alice@university.edu").fullName("Alice Johnson")
                .password(passwordEncoder.encode("alice123"))
                .role(User.Role.AUTHOR).institution("MIT")
                .bio("Researcher in distributed systems.").enabled(true).build());

        User author2 = userRepository.save(User.builder()
                .username("bob").email("bob@university.edu").fullName("Bob Smith")
                .password(passwordEncoder.encode("bob123"))
                .role(User.Role.AUTHOR).institution("Stanford")
                .bio("AI and ML researcher.").enabled(true).build());

        User reviewer1 = userRepository.save(User.builder()
                .username("carol").email("carol@review.org").fullName("Carol Williams")
                .password(passwordEncoder.encode("carol123"))
                .role(User.Role.REVIEWER).institution("Cambridge")
                .bio("Expert in cloud computing.").enabled(true).build());

        User reviewer2 = userRepository.save(User.builder()
                .username("dave").email("dave@review.org").fullName("Dave Brown")
                .password(passwordEncoder.encode("dave123"))
                .role(User.Role.REVIEWER).institution("Oxford")
                .bio("Expert in cybersecurity.").enabled(true).build());

        User speaker1 = userRepository.save(User.builder()
                .username("eve").email("eve@speaker.com").fullName("Eve Davis")
                .password(passwordEncoder.encode("eve123"))
                .role(User.Role.SPEAKER).institution("Google Research")
                .bio("Industry expert in AI applications.").enabled(true).build());

        User speaker2 = userRepository.save(User.builder()
                .username("frank").email("frank@speaker.com").fullName("Frank Miller")
                .password(passwordEncoder.encode("frank123"))
                .role(User.Role.SPEAKER).institution("Microsoft Research")
                .bio("Specializes in quantum computing.").enabled(true).build());

        User attendee1 = userRepository.save(User.builder()
                .username("grace").email("grace@attendee.com").fullName("Grace Lee")
                .password(passwordEncoder.encode("grace123"))
                .role(User.Role.ATTENDEE).institution("IIT Bombay").enabled(true).build());

        User attendee2 = userRepository.save(User.builder()
                .username("henry").email("henry@attendee.com").fullName("Henry Wilson")
                .password(passwordEncoder.encode("henry123"))
                .role(User.Role.ATTENDEE).institution("IISc Bangalore").enabled(true).build());

        // ── Conference ─────────────────────────────────────────────────────────
        Conference conf = conferenceRepository.save(Conference.builder()
                .name("International Conference on Computer Science 2026")
                .description("A premier venue for researchers and practitioners to present latest findings in computer science.")
                .venue("Bengaluru International Convention Centre, India")
                .startDate(LocalDate.of(2026, 6, 15))
                .endDate(LocalDate.of(2026, 6, 17))
                .submissionDeadline(LocalDate.of(2026, 4, 1))
                .reviewDeadline(LocalDate.of(2026, 5, 1))
                .notificationDate(LocalDate.of(2026, 5, 15))
                .maxAttendees(300)
                .contactEmail("iccs2026@cms.com")
                .status(Conference.ConferenceStatus.OPEN_FOR_SUBMISSIONS)
                .organizer(admin)
                .build());

        // ── Papers ─────────────────────────────────────────────────────────────
        Paper paper1 = paperRepository.save(Paper.builder()
                .title("Scalable Microservices Architecture for Cloud-Native Applications")
                .abstractText("This paper presents a novel approach to designing scalable microservices architectures.")
                .keywords("microservices, kubernetes, cloud, scalability")
                .author(author1)
                .status(Paper.Status.UNDER_REVIEW)
                .build());

        Paper paper2 = paperRepository.save(Paper.builder()
                .title("Deep Learning Approaches for Natural Language Understanding")
                .abstractText("We explore transformer-based architectures for NLU tasks.")
                .keywords("deep learning, NLP, transformers, BERT")
                .author(author2)
                .status(Paper.Status.ACCEPTED)
                .build());

        Paper paper3 = paperRepository.save(Paper.builder()
                .title("Zero-Knowledge Proofs in Blockchain Security")
                .abstractText("A comprehensive survey of zero-knowledge proof systems.")
                .keywords("blockchain, zero-knowledge, cryptography, security")
                .author(author1)
                .status(Paper.Status.SUBMITTED)
                .build());

        // ── Reviews ────────────────────────────────────────────────────────────
        reviewRepository.save(Review.builder()
                .paper(paper1).reviewer(reviewer1)
                .comments("Well-structured paper with practical contributions.")
                .strengthsComments("Clear architecture diagrams.")
                .weaknessComments("Related work section could be expanded.")
                .score(8).recommendation(Review.Recommendation.ACCEPT)
                .status(Review.ReviewStatus.SUBMITTED)
                .build());

        reviewRepository.save(Review.builder()
                .paper(paper1).reviewer(reviewer2)
                .comments("Good work overall but needs more performance analysis.")
                .score(7).recommendation(Review.Recommendation.MINOR_REVISION)
                .status(Review.ReviewStatus.SUBMITTED)
                .build());

        reviewRepository.save(Review.builder()
                .paper(paper2).reviewer(reviewer1)
                .comments("Excellent contribution to NLP research.")
                .score(9).recommendation(Review.Recommendation.ACCEPT)
                .status(Review.ReviewStatus.SUBMITTED)
                .build());

        // ── Sessions ───────────────────────────────────────────────────────────
        sessionRepository.save(Session.builder()
                .title("Keynote: The Future of Cloud Computing")
                .description("An inspiring keynote covering emerging trends.")
                .track("Keynote").room("Main Hall")
                .sessionDate(LocalDate.of(2026, 6, 15))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .type(Session.SessionType.KEYNOTE).speaker(speaker1)
                .maxCapacity(300).build());

        sessionRepository.save(Session.builder()
                .title("Deep Learning for NLP - Paper Presentation")
                .description("Presentation of accepted paper.")
                .track("AI & ML").room("Room A")
                .sessionDate(LocalDate.of(2026, 6, 15))
                .startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 15))
                .type(Session.SessionType.PAPER_PRESENTATION)
                .speaker(speaker1).paper(paper2)
                .maxCapacity(80).build());

        // ── Abstracts ──────────────────────────────────────────────────────────
        abstractRepository.save(Abstract.builder()
                .title("Federated Learning in Edge Computing")
                .content("We propose a privacy-preserving federated learning framework.")
                .keywords("federated learning, edge computing, privacy")
                .author(author1)
                .status(Abstract.AbstractStatus.APPROVED)
                .build());

        // ── Registrations ──────────────────────────────────────────────────────
        registrationRepository.save(Registration.builder()
                .attendee(attendee1).conference(conf)
                .type(Registration.RegistrationType.STUDENT)
                .status(Registration.RegistrationStatus.CONFIRMED)
                .dietaryPreferences("Vegetarian") // Your preference!
                .build());

        registrationRepository.save(Registration.builder()
                .attendee(author1).conference(conf)
                .type(Registration.RegistrationType.ACADEMIC)
                .status(Registration.RegistrationStatus.CONFIRMED)
                .build());
    }
}