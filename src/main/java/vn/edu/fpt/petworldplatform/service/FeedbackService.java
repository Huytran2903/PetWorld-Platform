package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.dto.GeneralFeedbackDTO;
import vn.edu.fpt.petworldplatform.entity.Feedback;
import vn.edu.fpt.petworldplatform.repository.FeedbackRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public Feedback submitGeneralFeedback(GeneralFeedbackDTO feedbackDTO, boolean isLoggedIn) {
        Feedback feedback = Feedback.builder()
                .type("general")
                .subject(feedbackDTO.getSubject())
                .comment(feedbackDTO.getComment())
                .imageUrls(feedbackDTO.getImageUrls())
                .status("pending")
                .createdAt(LocalDateTime.now())
                .build();

        if (isLoggedIn) {
            // User is logged in - try to get customer from session
            try {
                // For now, treat as guest user since we can't easily get customer from session
                // But we need to ensure at least email or phone is provided for database constraint
                if (feedbackDTO.getEmail() == null && feedbackDTO.getPhoneNumber() == null) {
                    throw new RuntimeException("For logged-in users, please provide either email or phone number for feedback submission.");
                }
                feedback.setEmail(feedbackDTO.getEmail());
                feedback.setPhoneNumber(feedbackDTO.getPhoneNumber());
            } catch (Exception e) {
                // If any error occurs, treat as guest user
                if (feedbackDTO.getEmail() == null && feedbackDTO.getPhoneNumber() == null) {
                    throw new RuntimeException("Please provide either email or phone number for feedback submission.");
                }
                feedback.setEmail(feedbackDTO.getEmail());
                feedback.setPhoneNumber(feedbackDTO.getPhoneNumber());
            }
        } else {
            // Guest user - must provide email or phone
            if (feedbackDTO.getEmail() == null && feedbackDTO.getPhoneNumber() == null) {
                throw new RuntimeException("Guest users must provide either email or phone number for feedback submission.");
            }
            feedback.setEmail(feedbackDTO.getEmail());
            feedback.setPhoneNumber(feedbackDTO.getPhoneNumber());
        }

        return feedbackRepository.save(feedback);
    }
}
