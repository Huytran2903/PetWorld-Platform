package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.dto.GeneralFeedbackDTO;
import vn.edu.fpt.petworldplatform.dto.ServiceReviewDTO;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentServiceLineRepository;
import vn.edu.fpt.petworldplatform.repository.FeedbackRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceLineRepository appointmentServiceLineRepository;

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

    /**
     * Validate and return the appointment for service review.
     */
    public Appointment getAppointmentForReview(Integer appointmentId, Integer customerId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));

        if (!appointment.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("This appointment does not belong to you.");
        }

        if (!"done".equalsIgnoreCase(appointment.getStatus())) {
            throw new IllegalArgumentException("You can only review completed appointments.");
        }

        return appointment;
    }

    /**
     * Get service lines for a given appointment.
     */
    public List<AppointmentServiceLine> getServiceLinesForAppointment(Integer appointmentId) {
        return appointmentServiceLineRepository.findByAppointment_Id(appointmentId);
    }

    /**
     * Check if a review already exists for a given appointment + service + customer.
     */
    public boolean hasAlreadyReviewed(Integer appointmentId, Integer serviceId, Integer customerId) {
        return feedbackRepository.existsByAppointmentIdAndServiceIdAndCustomer_CustomerId(
                appointmentId, serviceId, customerId);
    }

    /**
     * Submit a service review for a completed appointment.
     */
    public Feedback submitServiceReview(ServiceReviewDTO dto, Integer appointmentId, Customer customer) {
        Appointment appointment = getAppointmentForReview(appointmentId, customer.getCustomerId());

        // Verify the service belongs to this appointment
        List<AppointmentServiceLine> lines = appointmentServiceLineRepository.findByAppointment_Id(appointmentId);
        boolean serviceInAppointment = lines.stream()
                .anyMatch(line -> line.getService().getId().equals(dto.getServiceId()));
        if (!serviceInAppointment) {
            throw new IllegalArgumentException("Selected service is not part of this appointment.");
        }

        // Check for duplicate review
        if (hasAlreadyReviewed(appointmentId, dto.getServiceId(), customer.getCustomerId())) {
            throw new IllegalArgumentException("You have already submitted a review for this service in this appointment.");
        }

        Feedback feedback = Feedback.builder()
                .type("service")
                .customer(customer)
                .appointmentId(appointmentId)
                .serviceId(dto.getServiceId())
                .rating(dto.getRating())
                .subject(dto.getSubject())
                .comment(dto.getComment())
                .imageUrls(dto.getImageUrls())
                .status("pending")
                .build();

        return feedbackRepository.save(feedback);
    }

    // ──────────────── Feedback Manager (Staff) ────────────────

    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Feedback> getFeedbacksByFilter(String status, String type) {
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasType = type != null && !type.isBlank();

        if (hasStatus && hasType) {
            return feedbackRepository.findByStatusAndTypeOrderByCreatedAtDesc(status, type);
        } else if (hasStatus) {
            return feedbackRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (hasType) {
            return feedbackRepository.findByTypeOrderByCreatedAtDesc(type);
        }
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public Feedback getFeedbackById(Integer id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found."));
    }

    public void approveFeedback(Integer id) {
        Feedback feedback = getFeedbackById(id);
        feedback.setStatus("approved");
        feedbackRepository.save(feedback);
    }

    public void rejectFeedback(Integer id) {
        Feedback feedback = getFeedbackById(id);
        feedback.setStatus("rejected");
        feedbackRepository.save(feedback);
    }

    public void replyToFeedback(Integer id, String replyMessage) {
        Feedback feedback = getFeedbackById(id);
        feedback.setReplyMessage(replyMessage);
        feedback.setRepliedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }
}
