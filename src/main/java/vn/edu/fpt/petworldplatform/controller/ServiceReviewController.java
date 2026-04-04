package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.ServiceReviewDTO;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.service.FeedbackService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/customer/appointments")
@RequiredArgsConstructor
public class ServiceReviewController {

    private final FeedbackService feedbackService;

    private static final String UPLOAD_DIR = "uploads/feedback-images/";
    private static final int MAX_IMAGES = 5;
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "webp", "gif"
    ));

    private void validateImageUploads(MultipartFile[] imageFiles) {
        long nonEmptyFileCount = Arrays.stream(imageFiles)
                .filter(file -> file != null && !file.isEmpty())
                .count();

        if (nonEmptyFileCount > MAX_IMAGES) {
            throw new IllegalArgumentException("You can upload up to 5 images only.");
        }

        for (MultipartFile file : imageFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            validateSingleImage(file);
        }
    }

    private void validateSingleImage(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        int dotIndex = originalFilename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("Only image files (JPG, JPEG, PNG, WEBP, GIF) are allowed.");
        }

        String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only image files (JPG, JPEG, PNG, WEBP, GIF) are allowed.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only image files (JPG, JPEG, PNG, WEBP, GIF) are allowed.");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Each image must be 5MB or smaller.");
        }
    }

    /**
     * GET /customer/appointments/{id}/review — Show the service review form.
     */
    @GetMapping("/{id}/review")
    public String showReviewForm(@PathVariable Integer id,
                                 @RequestParam(value = "serviceId", required = false) Integer serviceId,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }

        try {
            Appointment appointment = feedbackService.getAppointmentForReview(id, customer.getCustomerId());
            List<AppointmentServiceLine> serviceLines = feedbackService.getServiceLinesForAppointment(id);

            if (serviceLines.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No services found for this appointment.");
                return "redirect:/customer/appointments";
            }

            Integer selectedServiceId = serviceId;
            if (selectedServiceId == null) {
                if (serviceLines.size() == 1) {
                    selectedServiceId = serviceLines.get(0).getService().getId();
                } else {
                    redirectAttributes.addFlashAttribute("error", "Please select a specific service to review from your appointment list.");
                    return "redirect:/customer/appointments";
                }
            }

            final Integer fixedSelectedServiceId = selectedServiceId;

            AppointmentServiceLine selectedServiceLine = serviceLines.stream()
                    .filter(line -> line.getService() != null && line.getService().getId().equals(fixedSelectedServiceId))
                    .findFirst()
                    .orElse(null);
            if (selectedServiceLine == null) {
                redirectAttributes.addFlashAttribute("error", "Selected service is not part of this appointment.");
                return "redirect:/customer/appointments";
            }

            if (feedbackService.hasAlreadyReviewed(id, selectedServiceId, customer.getCustomerId())) {
                redirectAttributes.addFlashAttribute("message", "You have already submitted a review for this service.");
                return "redirect:/customer/appointments";
            }

            model.addAttribute("appointment", appointment);
            model.addAttribute("serviceLines", serviceLines);
            ServiceReviewDTO reviewDTO = new ServiceReviewDTO();
            reviewDTO.setServiceId(selectedServiceId);
            model.addAttribute("reviewDTO", reviewDTO);
            model.addAttribute("activePage", "appointments");
            model.addAttribute("selectedServiceLine", selectedServiceLine);

            // Mark which services have already been reviewed
            List<Integer> reviewedServiceIds = new ArrayList<>();
            for (AppointmentServiceLine line : serviceLines) {
                if (feedbackService.hasAlreadyReviewed(id, line.getService().getId(), customer.getCustomerId())) {
                    reviewedServiceIds.add(line.getService().getId());
                }
            }
            model.addAttribute("reviewedServiceIds", reviewedServiceIds);
            model.addAttribute("selectedServiceReviewed", reviewedServiceIds.contains(selectedServiceId));

            return "feedback/service-review";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/appointments";
        }
    }

    /**
     * POST /customer/appointments/{id}/review — Submit the service review.
     */
    @PostMapping("/{id}/review")
    public String submitReview(@PathVariable Integer id,
                               @RequestParam(value = "serviceId", required = false) Integer serviceId,
                               @Valid @ModelAttribute("reviewDTO") ServiceReviewDTO reviewDTO,
                               BindingResult bindingResult,
                               @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }

        try {
            Appointment appointment = feedbackService.getAppointmentForReview(id, customer.getCustomerId());
            List<AppointmentServiceLine> serviceLines = feedbackService.getServiceLinesForAppointment(id);

            Integer selectedServiceId = serviceId;
            if (selectedServiceId == null) {
                redirectAttributes.addFlashAttribute("error", "Invalid review request: missing service.");
                return "redirect:/customer/appointments";
            }

                final Integer fixedSelectedServiceId = selectedServiceId;

            AppointmentServiceLine selectedServiceLine = serviceLines.stream()
                    .filter(line -> line.getService() != null && line.getService().getId().equals(fixedSelectedServiceId))
                    .findFirst()
                    .orElse(null);
            if (selectedServiceLine == null) {
                redirectAttributes.addFlashAttribute("error", "Selected service is not part of this appointment.");
                return "redirect:/customer/appointments";
            }

            if (feedbackService.hasAlreadyReviewed(id, selectedServiceId, customer.getCustomerId())) {
                redirectAttributes.addFlashAttribute("message", "You have already submitted a review for this service.");
                return "redirect:/customer/appointments";
            }

            // Keep service fixed from outside button to avoid mismatched selections.
            reviewDTO.setServiceId(selectedServiceId);

            List<Integer> reviewedServiceIds = new ArrayList<>();
            for (AppointmentServiceLine line : serviceLines) {
                if (feedbackService.hasAlreadyReviewed(id, line.getService().getId(), customer.getCustomerId())) {
                    reviewedServiceIds.add(line.getService().getId());
                }
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("appointment", appointment);
                model.addAttribute("serviceLines", serviceLines);
                model.addAttribute("activePage", "appointments");
                model.addAttribute("reviewedServiceIds", reviewedServiceIds);
                model.addAttribute("selectedServiceLine", selectedServiceLine);
                model.addAttribute("selectedServiceReviewed", reviewedServiceIds.contains(selectedServiceId));
                return "feedback/service-review";
            }

            // Process image uploads
            if (imageFiles != null && imageFiles.length > 0) {
                String imageUrls = processImageUploads(imageFiles);
                reviewDTO.setImageUrls(imageUrls);
            }

            feedbackService.submitServiceReview(reviewDTO, id, customer);

            String successMessage = "Your review has been submitted successfully. Thank you!";
            if (reviewDTO.getRating() != null && reviewDTO.getRating() >= 1 && reviewDTO.getRating() <= 3) {
                successMessage = "Your review has been submitted successfully. Thank you! We are truly sorry that your experience did not fully meet expectations. Your feedback is valuable, and we will work harder to improve our service.";
            }

            redirectAttributes.addFlashAttribute("message", successMessage);
            return "redirect:/customer/appointments";

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("already submitted")) {
                redirectAttributes.addFlashAttribute("message", "You have already submitted a review for this service.");
                return "redirect:/customer/appointments";
            }
            redirectAttributes.addFlashAttribute("error", msg);
            if (serviceId == null) {
                return "redirect:/customer/appointments";
            }
            return "redirect:/customer/appointments/" + id + "/review?serviceId=" + serviceId;
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload images. Please try again.");
            if (serviceId == null) {
                return "redirect:/customer/appointments";
            }
            return "redirect:/customer/appointments/" + id + "/review?serviceId=" + serviceId;
        }
    }

    private String processImageUploads(MultipartFile[] imageFiles) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        validateImageUploads(imageFiles);
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : imageFiles) {
            if (file != null && !file.isEmpty()) {
                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                if (originalFilename == null || !originalFilename.contains(".")) {
                    continue;
                }
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFilename = UUID.randomUUID().toString() + fileExtension;
                Path filePath = uploadPath.resolve(newFilename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                imageUrls.add("/uploads/feedback-images/" + newFilename);
            }
        }

        return String.join(",", imageUrls);
    }
}
