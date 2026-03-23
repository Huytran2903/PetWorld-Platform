package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Payment;
import vn.edu.fpt.petworldplatform.repository.PaymentRepository;
import vn.edu.fpt.petworldplatform.service.BookingService;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.MomoService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/customer/appointments")
@RequiredArgsConstructor
public class AppointmentPaymentController {

    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final MomoService momoService;
    private final CustomerService customerService;

    @PostMapping("/{id}/pay")
    public String pay(
            @PathVariable Integer id,
            @RequestParam("paymentMethod") String paymentMethod,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        Integer customerId = getCustomerIdFromAuth(authentication);
        if (customerId == null) {
            redirectAttributes.addFlashAttribute("error", "Please sign in to pay.");
            return "redirect:/login";
        }

        Appointment appointment = bookingService.findAppointmentByIdAndCustomerId(id, customerId)
                .orElse(null);
        if (appointment == null) {
            redirectAttributes.addFlashAttribute("error", "We could not find your appointment.");
            return "redirect:/customer/appointments";
        }

        if (appointment.getStatus() == null || !"done".equalsIgnoreCase(appointment.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "You can only pay after the appointment is completed.");
            return "redirect:/customer/appointments/" + id;
        }

        Payment latestPayment = paymentRepository.findTopByAppointment_IdAndPaymentTypeOrderByCreatedAtDesc(
                appointment.getId(),
                "service"
        );

        boolean hasPaid = latestPayment != null && latestPayment.getPaidAt() != null;

        if (hasPaid) {
            redirectAttributes.addFlashAttribute("message", "This appointment has already been paid.");
            return "redirect:/customer/appointments/" + id;
        }

        String normalizedMethod = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase();

        // Nếu đã tồn tại payment pending:
        // - MOMO: tạo lại URL và redirect sang MOMO (cho phép user thử thanh toán lại)
        // - COD: đổi payment method sang cod, chờ staff xác nhận
        if (latestPayment != null && latestPayment.getStatus() != null
                && "pending".equalsIgnoreCase(latestPayment.getStatus())) {
            if ("COD".equals(normalizedMethod)) {
                latestPayment.setMethod("cod");
                paymentRepository.save(latestPayment);

                redirectAttributes.addFlashAttribute(
                        "message",
                        "You chose cash on delivery. Please wait for staff to confirm payment."
                );
                return "redirect:/customer/appointments";
            }

            if ("MOMO".equals(normalizedMethod)) {
                // MOMO từ chối orderId trùng (resultCode 41) - luôn xóa payment cũ và tạo mới để có orderId mới
                paymentRepository.delete(latestPayment);
                latestPayment = null;
            }
        }

        // Compute total amount from appointment service lines
        List<AppointmentServiceLine> serviceLines = bookingService.findServiceLinesByAppointmentId(id);
        BigDecimal totalAmount = serviceLines.stream()
                .map(line -> {
                    BigDecimal price = line.getPrice() != null ? line.getPrice() : BigDecimal.ZERO;
                    Integer qty = line.getQuantity() != null ? line.getQuantity() : 1;
                    return price.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "Cannot pay: the total amount is invalid.");
            return "redirect:/customer/appointments/" + id;
        }

        Payment payment = new Payment();
        payment.setPaymentType("service");
        payment.setAppointment(appointment);
        payment.setAmount(totalAmount);
        payment.setStatus("pending");

        try {
            if ("MOMO".equals(normalizedMethod)) {
                payment.setMethod("momo");
                paymentRepository.save(payment); // need generated PaymentID

                String orderInfo = "Pet World appointment payment - Code: " + appointment.getAppointmentCode();

                // orderId phải unique mỗi lần - format APTP{paymentId}T{timestamp} để tránh lỗi 41 (trùng orderId)
                String momoOrderId = "APTP" + payment.getPaymentID() + "T" + System.currentTimeMillis();

                try {
                    String payUrl = momoService.createPaymentUrl(
                            momoOrderId,
                            payment.getAmount(),
                            orderInfo
                    );
                    return "redirect:" + payUrl;
                } catch (Exception e) {
                    // Nếu tạo payment MoMo thất bại => hủy bản ghi payment để không bị kẹt pending
                    paymentRepository.delete(payment);
                    throw e;
                }
            }

            if ("COD".equals(normalizedMethod)) {
                payment.setMethod("cod");
                paymentRepository.save(payment);

                redirectAttributes.addFlashAttribute(
                        "message",
                        "You chose cash on delivery. Please wait for staff to confirm payment."
                );
                return "redirect:/customer/appointments";
            }

            redirectAttributes.addFlashAttribute("error", "Invalid payment method.");
            return "redirect:/customer/appointments/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment error: " + e.getMessage());
            return "redirect:/customer/appointments/" + id;
        }
    }

    private Integer getCustomerIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email == null) return null;
            return customerService.findIdByEmail(email);
        }

        if (authentication.getPrincipal() instanceof Customer customer) {
            return customer.getCustomerId();
        }

        return customerService.findIdByUsername(authentication.getName());
    }
}

