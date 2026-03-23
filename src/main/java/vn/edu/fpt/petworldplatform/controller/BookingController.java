package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.BookingService;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/appointment")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Book Service Appointment - use case: Customer clicks "Book Service Appointment" on Home.
     */
    @GetMapping("/booking")
    public String bookingPage(HttpSession session, Model model,
                              @RequestParam(required = false) String type,
                              RedirectAttributes redirectAttributes) {
        Object loggedIn = session.getAttribute("loggedInAccount");
        Customer customer = null;
        if (loggedIn instanceof Customer c) {
            customer = c;
        } else if (loggedIn instanceof Staff) {
            // staff không được vào trang booking của customer
            return "redirect:/staff/assigned_list";
        } else {
            return "redirect:/login";
        }

        List<Pets> petList = bookingService.findPetsByCustomerId(customer.getCustomerId());
        if (petList.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please create a pet profile first.");
            return "redirect:/customer/pet/create";
        }

        String currentType = (type != null && !type.isBlank()) ? type.trim() : "";
        model.addAttribute("petList", petList);
        model.addAttribute("serviceList", currentType.isBlank() ? bookingService.findActiveServices() : bookingService.findActiveServicesByType(currentType));
        model.addAttribute("currentType", currentType);
        model.addAttribute("pageTitle", "Book Service Appointment");
        model.addAttribute("heroTitle", "Book Service Appointment");
        return "appointment/booking";
    }

    /**
     * Confirm booking - validates lead time (BR-17) and operating hours (08:00–20:00).
     */
    @PostMapping("/create")
    public String createBooking(HttpSession session,
                                @RequestParam Integer petId,
                                @RequestParam String appointmentDate,
                                @RequestParam(required = false) List<Integer> mainServices,
                                @RequestParam(required = false) String note,
                                RedirectAttributes redirectAttributes) {
        Object loggedIn = session.getAttribute("loggedInAccount");
        Customer customer = null;
        if (loggedIn instanceof Customer c) {
            customer = c;
        } else if (loggedIn instanceof Staff) {
            return "redirect:/staff/assigned_list";
        } else {
            return "redirect:/login";
        }

        List<Pets> petList = bookingService.findPetsByCustomerId(customer.getCustomerId());
        if (petList.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please create a pet profile first.");
            return "redirect:/customer/pet/create";
        }

        boolean petBelongsToCustomer = petList.stream()
                .anyMatch(p -> p.getId() != null && p.getId().equals(petId));
        if (!petBelongsToCustomer) {
            redirectAttributes.addFlashAttribute("error", "Invalid pet selected.");
            return "redirect:/appointment/booking";
        }

        if (mainServices == null || mainServices.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one service.");
            return "redirect:/appointment/booking";
        }

        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(appointmentDate.replace(" ", "T"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Invalid date and time.");
            return "redirect:/appointment/booking";
        }

        var validationError = bookingService.validateAppointmentDateTime(dateTime);
        if (validationError.isPresent()) {
            redirectAttributes.addFlashAttribute("error", validationError.get());
            return "redirect:/appointment/booking";
        }

        try {
            bookingService.createAppointment(
                    customer.getCustomerId(),
                    petId,
                    dateTime,
                    note != null ? note.trim() : null,
                    mainServices
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/appointment/booking";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to create appointment right now. Please try again.");
            return "redirect:/appointment/booking";
        }

        redirectAttributes.addFlashAttribute(
                "message",
                "Your appointment has been booked successfully and is pending staff assignment. We will confirm once a staff member is assigned."
        );
        return "redirect:/customer/appointments";
    }
}
