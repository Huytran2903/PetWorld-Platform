package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
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
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/customer/appointments")
@RequiredArgsConstructor
public class ServiceReviewController {

    private final FeedbackService feedbackService;

    private static final String UPLOAD_DIR = "uploads/feedback-images/";

    /**
     * GET /customer/appointments/{id}/review — Show the service review form.
     */
    @GetMapping("/{id}/review")
    public String showReviewForm(@PathVariable Integer id,
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

            model.addAttribute("appointment", appointment);
            model.addAttribute("serviceLines", serviceLines);
            model.addAttribute("reviewDTO", new ServiceReviewDTO());
            model.addAttribute("activePage", "appointments");

            // Mark which services have already been reviewed
            List<Integer> reviewedServiceIds = new ArrayList<>();
            for (AppointmentServiceLine line : serviceLines) {
                if (feedbackService.hasAlreadyReviewed(id, line.getService().getId(), customer.getCustomerId())) {
                    reviewedServiceIds.add(line.getService().getId());
                }
            }
            model.addAttribute("reviewedServiceIds", reviewedServiceIds);

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

            if (bindingResult.hasErrors()) {
                model.addAttribute("appointment", appointment);
                model.addAttribute("serviceLines", serviceLines);
                model.addAttribute("activePage", "appointments");

                List<Integer> reviewedServiceIds = new ArrayList<>();
                for (AppointmentServiceLine line : serviceLines) {
                    if (feedbackService.hasAlreadyReviewed(id, line.getService().getId(), customer.getCustomerId())) {
                        reviewedServiceIds.add(line.getService().getId());
                    }
                }
                model.addAttribute("reviewedServiceIds", reviewedServiceIds);
                return "feedback/service-review";
            }

            // Process image uploads
            if (imageFiles != null && imageFiles.length > 0) {
                String imageUrls = processImageUploads(imageFiles);
                reviewDTO.setImageUrls(imageUrls);
            }

            feedbackService.submitServiceReview(reviewDTO, id, customer);

            redirectAttributes.addFlashAttribute("message", "Your review has been submitted successfully. Thank you!");
            return "redirect:/customer/appointments/" + id + "/review";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/appointments/" + id + "/review";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload images. Please try again.");
            return "redirect:/customer/appointments/" + id + "/review";
        }
    }

    private String processImageUploads(MultipartFile[] imageFiles) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : imageFiles) {
            if (file != null && !file.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
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
