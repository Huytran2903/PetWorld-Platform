package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.BookingService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        loadBookingPageModel(model, customer, type);
        return "appointment/booking";
    }

    private void loadBookingPageModel(Model model, Customer customer, String typeParam) {
        String currentType = (typeParam != null && !typeParam.isBlank()) ? typeParam.trim() : "";
        List<Pets> petList = bookingService.findPetsByCustomerId(customer.getCustomerId());
        model.addAttribute("petList", petList);
        model.addAttribute("serviceList", currentType.isBlank() ? bookingService.findActiveServices() : bookingService.findActiveServicesByType(currentType));
        model.addAttribute("currentType", currentType);
        model.addAttribute("pageTitle", "Book Service Appointment");
        model.addAttribute("heroTitle", "Book Service Appointment");
    }

    /** Normalize posted datetime for HTML datetime-local (yyyy-MM-dd'T'HH:mm). */
    private static String normalizeDatetimeLocalValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim().replace(' ', 'T');
        try {
            LocalDateTime dt = LocalDateTime.parse(s);
            return String.format("%04d-%02d-%02dT%02d:%02d",
                    dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(),
                    dt.getHour(), dt.getMinute());
        } catch (Exception e) {
            return s.length() >= 16 ? s.substring(0, 16) : s;
        }
    }

    /**
     * Re-render booking form with field values preserved (e.g. validation: no service selected).
     */
    private String renderBookingFormWithState(
            Model model,
            Customer customer,
            String serviceType,
            Integer formPetId,
            String appointmentDateRaw,
            String note,
            List<Integer> selectedServiceIds,
            String error
    ) {
        loadBookingPageModel(model, customer, serviceType != null ? serviceType : "");
        model.addAttribute("formPetId", formPetId);
        model.addAttribute("formAppointmentDate", normalizeDatetimeLocalValue(appointmentDateRaw));
        model.addAttribute("formNote", note != null ? note : "");
        model.addAttribute("selectedServiceIds", selectedServiceIds != null ? selectedServiceIds : List.of());
        model.addAttribute("error", error);
        return "appointment/booking";
    }

    /**
     * Confirm booking - validates lead time (BR-17) and operating hours (08:00–20:00).
     */
    private String redirectToBooking(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return "redirect:/appointment/booking";
        }
        return "redirect:/appointment/booking?type=" + URLEncoder.encode(serviceType.trim(), StandardCharsets.UTF_8);
    }

    @PostMapping("/create")
    public String createBooking(HttpSession session,
                                Model model,
                                @RequestParam(required = false) Integer petId,
                                @RequestParam(required = false) String appointmentDate,
                                @RequestParam(required = false) List<Integer> mainServices,
                                @RequestParam(required = false) String note,
                                @RequestParam(value = "serviceType", required = false) String serviceType,
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

        if (petId == null) {
            redirectAttributes.addFlashAttribute("error", "Please select your pet.");
            return redirectToBooking(serviceType);
        }

        boolean petBelongsToCustomer = petList.stream()
                .anyMatch(p -> p.getId() != null && p.getId().equals(petId));
        if (!petBelongsToCustomer) {
            redirectAttributes.addFlashAttribute("error", "Invalid pet selected. Please choose a pet from your list.");
            return redirectToBooking(serviceType);
        }

        if (mainServices == null || mainServices.isEmpty()) {
            return renderBookingFormWithState(
                    model,
                    customer,
                    serviceType,
                    petId,
                    appointmentDate,
                    note,
                    List.of(),
                    "Please select at least one service."
            );
        }

        if (appointmentDate == null || appointmentDate.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Please choose an appointment date and time.");
            return redirectToBooking(serviceType);
        }

        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(appointmentDate.replace(" ", "T"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Invalid date and time. Please use the picker and try again.");
            return redirectToBooking(serviceType);
        }

        var validationError = bookingService.validateAppointmentDateTime(dateTime);
        if (validationError.isPresent()) {
            redirectAttributes.addFlashAttribute("error", validationError.get());
            return redirectToBooking(serviceType);
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
            return redirectToBooking(serviceType);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to create appointment right now. Please try again.");
            return redirectToBooking(serviceType);
        }

        redirectAttributes.addFlashAttribute(
                "message",
                "Your appointment has been booked successfully and is pending staff assignment. We will confirm once a staff member is assigned."
        );
        return "redirect:/customer/appointments";
    }
}
