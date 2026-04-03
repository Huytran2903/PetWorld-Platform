package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.dto.GeneralFeedbackDTO;
import vn.edu.fpt.petworldplatform.dto.ServiceReviewDTO;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentServiceLineRepository;
import vn.edu.fpt.petworldplatform.repository.FeedbackRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceItemRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final int GENERAL_FEEDBACK_COOLDOWN_MINUTES = 10;

    private final FeedbackRepository feedbackRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceLineRepository appointmentServiceLineRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final NotificationService notificationService;

    public void validateGeneralFeedbackCooldown(Customer loggedInCustomer) {
        if (loggedInCustomer == null || loggedInCustomer.getCustomerId() == null) {
            throw new RuntimeException("Please login to submit feedback.");
        }

        LocalDateTime now = LocalDateTime.now();
        feedbackRepository.findTopByCustomer_CustomerIdAndTypeOrderByCreatedAtDesc(
                        loggedInCustomer.getCustomerId(), "general")
                .ifPresent(lastFeedback -> {
                    if (lastFeedback.getCreatedAt() == null) {
                        return;
                    }

                    LocalDateTime nextAllowedAt = lastFeedback.getCreatedAt().plusMinutes(GENERAL_FEEDBACK_COOLDOWN_MINUTES);
                    if (now.isBefore(nextAllowedAt)) {
                        long remainingSeconds = Duration.between(now, nextAllowedAt).getSeconds();
                        long remainingMinutes = Math.max(1, (remainingSeconds + 59) / 60);
                        throw new IllegalStateException(
                            "You just submitted feedback. Please wait " + remainingMinutes + " more minute(s) before submitting again.");
                    }
                });
    }

    public Feedback submitGeneralFeedback(GeneralFeedbackDTO feedbackDTO, Customer loggedInCustomer) {
        validateGeneralFeedbackCooldown(loggedInCustomer);

        Feedback feedback = Feedback.builder()
                .type("general")
                .subject(feedbackDTO.getSubject())
                .comment(feedbackDTO.getComment())
                .imageUrls(feedbackDTO.getImageUrls())
                .status("pending")
                .createdAt(LocalDateTime.now())
                .build();

        if (loggedInCustomer == null) {
            throw new RuntimeException("Please login to submit feedback.");
        }

        feedback.setCustomer(loggedInCustomer);

        String inputEmail = feedbackDTO.getEmail() != null ? feedbackDTO.getEmail().trim() : "";
        String inputPhone = feedbackDTO.getPhoneNumber() != null ? feedbackDTO.getPhoneNumber().trim() : "";

        String resolvedEmail = !inputEmail.isBlank() ? inputEmail : loggedInCustomer.getEmail();
        String resolvedPhone = !inputPhone.isBlank() ? inputPhone : loggedInCustomer.getPhone();

        feedback.setEmail(resolvedEmail);
        feedback.setPhoneNumber(resolvedPhone);

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
     * Get the service review feedback for a given appointment + service + customer.
     */
    public java.util.Optional<Feedback> getServiceReview(Integer appointmentId, Integer serviceId, Integer customerId) {
        return feedbackRepository.findTopByAppointmentIdAndServiceIdAndCustomer_CustomerIdOrderByCreatedAtDesc(
                appointmentId, serviceId, customerId);
    }

    /**
     * Update customer's own service review (comment, rating, subject only).
     */
    public void updateCustomerServiceReview(Integer feedbackId, Integer customerId, String comment, Integer rating, String subject) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found."));
        if (feedback.getCustomer() == null || !feedback.getCustomer().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only update your own feedback.");
        }
        if (!"service".equalsIgnoreCase(feedback.getType())) {
            throw new IllegalArgumentException("This feedback is not a service review.");
        }
        if (comment != null) {
            feedback.setComment(comment.trim());
        }
        if (rating != null && rating >= 1 && rating <= 5) {
            feedback.setRating(rating);
        }
        if (subject != null) {
            feedback.setSubject(subject.trim());
        }
        feedbackRepository.save(feedback);
    }

    /**
     * Submit a service review for a completed appointment.
     */
    public Feedback submitServiceReview(ServiceReviewDTO dto, Integer appointmentId, Customer customer) {
        Appointment appointment = getAppointmentForReview(appointmentId, customer.getCustomerId());

        // Verify the service belongs to this appointment
        List<AppointmentServiceLine> lines = appointmentServiceLineRepository.findByAppointment_Id(appointmentId);
        AppointmentServiceLine selectedLine = lines.stream()
            .filter(line -> line.getService() != null && line.getService().getId().equals(dto.getServiceId()))
            .findFirst()
            .orElse(null);
        if (selectedLine == null) {
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
                .serviceName(selectedLine.getService().getName())
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

    public Page<Feedback> getFeedbacksByFilter(String status, String type, int page, int size) {
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasType = type != null && !type.isBlank();

        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (hasStatus && hasType) {
            return feedbackRepository.findByStatusAndType(status, type, pageable);
        } else if (hasStatus) {
            return feedbackRepository.findByStatus(status, pageable);
        } else if (hasType) {
            return feedbackRepository.findByType(type, pageable);
        }

        return feedbackRepository.findAll(pageable);
    }

    public Feedback getFeedbackById(Integer id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found."));
    }

    public void approveFeedback(Integer id) {
        Feedback feedback = getFeedbackById(id);
        feedback.setStatus("approved");
        feedbackRepository.save(feedback);
        notifyFeedbackStatus(feedback, "approved");
    }

    public void rejectFeedback(Integer id) {
        Feedback feedback = getFeedbackById(id);
        feedback.setStatus("rejected");
        feedbackRepository.save(feedback);
        notifyFeedbackStatus(feedback, "rejected");
    }

    public void replyToFeedback(Integer id, String replyMessage) {
        Feedback feedback = getFeedbackById(id);
        feedback.setReplyMessage(replyMessage);
        feedback.setRepliedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
        notifyFeedbackReply(feedback);
    }

    private void notifyFeedbackStatus(Feedback feedback, String status) {
        if (feedback.getCustomer() == null) {
            return;
        }

        String feedbackLabel = resolveFeedbackLabel(feedback);
        String serviceInfo = resolveServiceInfoLine(feedback);
        String title;
        String message;

        if ("approved".equalsIgnoreCase(status)) {
            title = "Feedback approved";
            message = "Feedback: " + feedbackLabel
                + serviceInfo
                    + "\nStatus: Approved"
                    + "\n\nThank you for sharing your experience with PetWorld.";
        } else {
            title = "Feedback rejected";
            message = "Feedback: " + feedbackLabel
                + serviceInfo
                    + "\nStatus: Rejected"
                    + "\n\nPlease contact support if you need more details.";
        }

        notificationService.createForCustomer(feedback.getCustomer(), null, title, message, "feedback_status");
    }

    private void notifyFeedbackReply(Feedback feedback) {
        if (feedback.getCustomer() == null || feedback.getReplyMessage() == null || feedback.getReplyMessage().isBlank()) {
            return;
        }

        String feedbackLabel = resolveFeedbackLabel(feedback);
        String serviceInfo = resolveServiceInfoLine(feedback);
        String title = "Admin replied to your feedback";
        String message = "Feedback: " + feedbackLabel
            + serviceInfo
                + "\n\nAdmin reply:\n" + feedback.getReplyMessage().trim();

        notificationService.createForCustomer(feedback.getCustomer(), null, title, message, "feedback_reply");
    }

    private String resolveFeedbackLabel(Feedback feedback) {
        String serviceName = resolveServiceName(feedback);

        if (feedback.getSubject() != null && !feedback.getSubject().isBlank()) {
            String subject = feedback.getSubject().trim();
            if (serviceName == null || !subject.equalsIgnoreCase(serviceName)) {
                return subject;
            }
        }

        if (feedback.getComment() != null && !feedback.getComment().isBlank()) {
            String comment = feedback.getComment().trim();
            return comment.length() > 60 ? comment.substring(0, 60) + "..." : comment;
        }

        return "Your feedback";
    }

    private String resolveServiceInfoLine(Feedback feedback) {
        String serviceName = resolveServiceName(feedback);
        if (serviceName != null && !serviceName.isBlank()) {
            return "\nService: " + serviceName;
        }

        return "";
    }

    private String resolveServiceName(Feedback feedback) {
        if (feedback.getServiceName() != null && !feedback.getServiceName().isBlank()) {
            return feedback.getServiceName().trim();
        }

        if (feedback.getServiceId() != null) {
            return serviceItemRepository.findById(feedback.getServiceId())
                    .map(ServiceItem::getName)
                    .orElse(null);
        }

        return null;
    }
}
