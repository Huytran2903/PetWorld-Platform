package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.GeneralFeedbackDTO;
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
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

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

    private String processImageUploads(MultipartFile[] imageFiles) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        validateImageUploads(imageFiles);
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        for (MultipartFile file : imageFiles) {
            if (file != null && !file.isEmpty()) {
                // Generate unique filename
                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFilename = UUID.randomUUID().toString() + fileExtension;
                
                // Save file
                Path filePath = uploadPath.resolve(newFilename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                // Create URL for accessing the file
                String imageUrl = "/uploads/feedback-images/" + newFilename;
                imageUrls.add(imageUrl);
            }
        }
        
        return String.join(",", imageUrls);
    }

    @GetMapping
    public String showFeedbackForm(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // Check if user is logged in using session
        Object loggedInAccount = request.getSession().getAttribute("loggedInAccount");
        boolean isLoggedIn = loggedInAccount != null;

        if (!isLoggedIn) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to submit feedback.");
            return "redirect:/login";
        }
        
        model.addAttribute("feedbackDTO", new GeneralFeedbackDTO());
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("activePage", "feedback");
        return "feedback/general-feedback";
    }

    @PostMapping
    public String submitFeedback(@Valid @ModelAttribute("feedbackDTO") GeneralFeedbackDTO feedbackDTO,
                               BindingResult bindingResult,
                               @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                               Model model,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        
        // Check if user is logged in using session
        Object loggedInAccount = request.getSession().getAttribute("loggedInAccount");
        boolean isLoggedIn = loggedInAccount != null;

        if (!isLoggedIn) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to submit feedback.");
            return "redirect:/login";
        }
        
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("activePage", "feedback");

        if (bindingResult.hasErrors()) {
            return "feedback/general-feedback";
        }

        try {
            Customer loggedInCustomer = (loggedInAccount instanceof Customer) ? (Customer) loggedInAccount : null;

            // Check anti-spam cooldown before handling file uploads.
            feedbackService.validateGeneralFeedbackCooldown(loggedInCustomer);

            // Process image uploads
            if (imageFiles != null && imageFiles.length > 0) {
                String imageUrls = processImageUploads(imageFiles);
                feedbackDTO.setImageUrls(imageUrls);
            }

            feedbackService.submitGeneralFeedback(feedbackDTO, loggedInCustomer);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback submitted successfully! Thank you for your feedback.");
            return "redirect:/feedback";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "feedback/general-feedback";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "feedback/general-feedback";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error submitting feedback: " + e.getMessage());
            return "feedback/general-feedback";
        }
    }

    @GetMapping("/success")
    public String showSuccessPage() {
        return "feedback/feedback-success";
    }
}
