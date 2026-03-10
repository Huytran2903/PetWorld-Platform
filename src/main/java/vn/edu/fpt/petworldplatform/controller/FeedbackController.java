package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.GeneralFeedbackDTO;
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
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    
    private static final String UPLOAD_DIR = "uploads/feedback-images/";

    private String processImageUploads(MultipartFile[] imageFiles) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        for (MultipartFile file : imageFiles) {
            if (file != null && !file.isEmpty()) {
                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
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
    public String showFeedbackForm(Model model, HttpServletRequest request) {
        // Check if user is logged in using session
        Object loggedInAccount = request.getSession().getAttribute("loggedInAccount");
        boolean isLoggedIn = loggedInAccount != null;
        
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
        
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("activePage", "feedback");

        if (bindingResult.hasErrors()) {
            return "feedback/general-feedback";
        }

        try {
            // Process image uploads
            if (imageFiles != null && imageFiles.length > 0) {
                String imageUrls = processImageUploads(imageFiles);
                feedbackDTO.setImageUrls(imageUrls);
            }
            
            feedbackService.submitGeneralFeedback(feedbackDTO, isLoggedIn);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback submitted successfully! Thank you for your feedback.");
            return "redirect:/feedback";
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
