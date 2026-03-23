package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Feedback;
import vn.edu.fpt.petworldplatform.service.FeedbackService;

@Controller
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;
    private static final int FEEDBACK_PAGE_SIZE = 6;

    @PreAuthorize("hasAuthority('MANAGE_FEEDBACK')")
    @GetMapping
    public String showFeedbackManager(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(defaultValue = "0") int page,
                                      Model model) {
        Page<Feedback> feedbackPage = feedbackService.getFeedbacksByFilter(status, type, page, FEEDBACK_PAGE_SIZE);

        model.addAttribute("feedbacks", feedbackPage.getContent());
        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("currentPage", feedbackPage.getNumber());
        model.addAttribute("totalPages", feedbackPage.getTotalPages());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentType", type);
        model.addAttribute("activePage", "feedback");
        return "admin/feedback-manager";
    }

    @PreAuthorize("hasAuthority('MANAGE_FEEDBACK')")
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            feedbackService.approveFeedback(id);
            redirectAttributes.addFlashAttribute("message", "Feedback approved successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/feedback";
    }

    @PreAuthorize("hasAuthority('MANAGE_FEEDBACK')")
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            feedbackService.rejectFeedback(id);
            redirectAttributes.addFlashAttribute("message", "Feedback rejected.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/feedback";
    }

    @PreAuthorize("hasAuthority('MANAGE_FEEDBACK')")
    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Integer id,
                        @RequestParam String replyMessage,
                        RedirectAttributes redirectAttributes) {
        try {
            if (replyMessage == null || replyMessage.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Reply message cannot be empty.");
                return "redirect:/admin/feedback";
            }
            feedbackService.replyToFeedback(id, replyMessage.trim());
            redirectAttributes.addFlashAttribute("message", "Reply sent successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/feedback";
    }
}
