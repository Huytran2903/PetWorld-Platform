package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Notification;
import vn.edu.fpt.petworldplatform.service.NotificationService;
import vn.edu.fpt.petworldplatform.util.SecuritySupport;

@Controller
@RequestMapping("/customer/notifications")
@RequiredArgsConstructor
public class CustomerNotificationController {

    private final NotificationService notificationService;
    private final SecuritySupport securitySupport;

    @GetMapping
    public String listNotifications(@RequestParam(defaultValue = "0") int page, Model model) {
        Integer customerId = securitySupport.getCurrentAuthenticatedCustomerId();
        if (customerId == null) {
            return "redirect:/login";
        }

        Page<Notification> notificationPage = notificationService.getPage(customerId, page, 10);
        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("activePage", "notifications");

        return "customer/notifications";
    }

    @GetMapping("/{id}")
    public String viewNotification(@PathVariable Integer id,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        Integer customerId = securitySupport.getCurrentAuthenticatedCustomerId();
        if (customerId == null) {
            return "redirect:/login";
        }

        try {
            Notification notification = notificationService.getAndMarkRead(customerId, id);
            model.addAttribute("notification", notification);
            model.addAttribute("activePage", "notifications");
            return "customer/notification-detail";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/customer/notifications";
        }
    }
}
