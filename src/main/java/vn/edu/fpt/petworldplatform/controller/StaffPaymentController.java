package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.Payment;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.PaymentRepository;
import vn.edu.fpt.petworldplatform.service.BookingService;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.NotificationService;
import vn.edu.fpt.petworldplatform.service.StaffService;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/staff/appointments")
@RequiredArgsConstructor
public class StaffPaymentController {

    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final CustomerService customerService;
    private final StaffService staffService;
    private final NotificationService notificationService;

    @PostMapping("/{id}/confirm-cod")
    public String confirmCod(@PathVariable Integer id,
                               Authentication authentication,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        Integer staffId = resolveCurrentStaffId(authentication, session);
        if (staffId == null) {
            redirectAttributes.addFlashAttribute("error", "Please sign in to confirm payment.");
            return "redirect:/staff/appointment_detail?id=" + id;
        }

        Appointment appointment = bookingService.findById(id).orElse(null);
        if (appointment == null) {
            redirectAttributes.addFlashAttribute("error", "Appointment not found.");
            return "redirect:/staff/appointment_detail?id=" + id;
        }

        // Only manager can confirm
        if (appointment.getStaffId() == null || !appointment.getStaffId().equals(staffId)) {
            redirectAttributes.addFlashAttribute("error", "You are not allowed to confirm payment for this appointment.");
            return "redirect:/staff/appointment_detail?id=" + id;
        }

        Payment latestPayment = paymentRepository.findTopByAppointment_IdAndPaymentTypeOrderByCreatedAtDesc(
                id,
                "service"
        );

        if (latestPayment == null || latestPayment.getMethod() == null
                || !"cod".equalsIgnoreCase(latestPayment.getMethod())) {
            redirectAttributes.addFlashAttribute("error", "No pending cash-on-delivery payment was found for this appointment.");
            return "redirect:/staff/appointment_detail?id=" + id;
        }

        if (latestPayment.getPaidAt() != null) {
            redirectAttributes.addFlashAttribute("message", "This COD payment was already confirmed.");
            return "redirect:/staff/appointment_detail?id=" + id;
        }

        // CHECK constraint on Payments.Status may block "paid".
        // So we only set PaidAt to indicate confirmation.
        latestPayment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(latestPayment);

        Customer notifyCustomer = appointment.getCustomerId() != null
                ? customerService.findById(appointment.getCustomerId()).orElse(null)
                : null;
        String title = "Payment successful";
        String apptCode = appointment.getAppointmentCode() != null ? appointment.getAppointmentCode() : "-";
        String message = "Your cash-on-delivery payment for appointment " + apptCode + " was recorded successfully.";
        notificationService.createForCustomer(notifyCustomer, appointment, title, message, "payment_success");

        redirectAttributes.addFlashAttribute("message", "Customer's cash-on-delivery payment was confirmed successfully.");
        return "redirect:/staff/appointment_detail?id=" + id;
    }

    private Integer resolveCurrentStaffId(Authentication authentication, HttpSession session) {
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Staff staff) {
                return staff.getStaffId();
            }
            if (principal instanceof UserDetails userDetails) {
                return staffService.findByUsername(userDetails.getUsername()).map(Staff::getStaffId).orElse(null);
            }
        }

        // Fallback: session attribute
        Object loggedInStaff = session.getAttribute("loggedInStaff");
        if (loggedInStaff instanceof Staff staff) {
            return staff.getStaffId();
        }

        return null;
    }
}

