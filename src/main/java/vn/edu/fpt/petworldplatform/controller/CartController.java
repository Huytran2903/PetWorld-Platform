package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.OrderRepo;
import vn.edu.fpt.petworldplatform.repository.PaymentRepository;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.repository.ProductRepo;
import vn.edu.fpt.petworldplatform.service.CartService;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.MomoService;
import vn.edu.fpt.petworldplatform.service.NotificationService;
import vn.edu.fpt.petworldplatform.service.OrderService;

import java.math.BigDecimal;
import java.util.Map;

@Controller
public class CartController {


    @Autowired
    private MomoService momoService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PetRepo petRepo;

    @GetMapping("/cart/add-pet/{id}")
    public String addPetToCart(@PathVariable("id") Integer id,
                                   Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }


        Integer customerId = getCustomerIdFromAuth(authentication);

        if (customerId == null) {
            return "redirect:/login?error=account_not_found";
        }


        Pets pet = petRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pet not found with ID: " + id));


        cartService.addToCart(customerId, pet, null, 1);


        return "redirect:/pets";
    }

    @GetMapping("/cart/add-product/{id}")
    public String addProductToCart(@PathVariable("id") Integer id,
                                   @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                                   Authentication authentication) {


        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }


        Integer customerId = getCustomerIdFromAuth(authentication);

        if (customerId == null) {
            return "redirect:/login?error=account_not_found";
        }

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));

        cartService.addToCart(customerId, null, product, qty);

        return "redirect:/products";
    }

    @GetMapping("/cart/view")
    public String viewCart(Model model, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Integer customerId = getCustomerIdFromAuth(authentication);

        if (customerId == null) {
            return "redirect:/login?error=account_not_found";
        }

        // =========================================================
        // THÊM 2 DÒNG NÀY ĐỂ LẤY THÔNG TIN TỪ DB TRUYỀN RA GIAO DIỆN
        Customer currentCustomer = customerService.findById(customerId).orElse(null);
        model.addAttribute("customer", currentCustomer);
        // =========================================================

        Carts cart = cartService.getCartDetail(customerId);

        // Nếu chưa có giỏ hàng
        if (cart == null) {
            model.addAttribute("subtotal", BigDecimal.ZERO);
            model.addAttribute("tax", BigDecimal.ZERO);
            model.addAttribute("total", BigDecimal.ZERO);
            return "customer/shopping-cart";
        }

        // 1. Lấy Subtotal từ Service
        BigDecimal subtotal = cartService.calculateSubtotal(cart);

        // 2. THAY ĐỔI TẠI ĐÂY: Gán cứng phí ship 25,000 (Bỏ phần tính Tax 0.05)
        BigDecimal shippingFee = new BigDecimal("25000");

        // 3. THAY ĐỔI TẠI ĐÂY: Tổng cộng = Tiền hàng + Phí ship
        BigDecimal total = subtotal.add(shippingFee);

        // 4. Đưa dữ liệu ra giao diện
        model.addAttribute("cart", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingfree", shippingFee);
        model.addAttribute("total", total);

        return "customer/shopping-cart";
    }

    @GetMapping("/cart/update/{id}")
    public String updateCartQuantity(@PathVariable("id") Integer cartItemId,
                                     @RequestParam("action") String action,
                                     RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(cartItemId, action);
        } catch (RuntimeException e) {
            // Gửi thông báo lỗi xuống giao diện nếu hết kho
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/cart/view";
    }


    @GetMapping("/cart/checkout-order")
    public String checkoutOrder(Model model) {

        if (!model.containsAttribute("order")) {
            return "redirect:/";
        }

        //Lấy đối tượng order "tạm" từ bộ nhớ đệm
        Order tempOrder = (Order) model.getAttribute("order");

        if (tempOrder != null && tempOrder.getOrderID() != null) {
            // 3. Truy vấn lại Order THẬT từ Database để lấy toàn bộ dữ liệu (bao gồm cả OrderItems)
            Order realOrder = orderRepo.findById(tempOrder.getOrderID()).orElse(tempOrder);

            // 4. Ghi đè order thật vào lại Model cho Thymeleaf đọc
            model.addAttribute("order", realOrder);
        }

        return "customer/checkout-order";
    }

    @PostMapping("/cart/checkout")
    public String processCheckout(
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("shipName") String shipName,
            @RequestParam("shipPhone") String shipPhone,
            @RequestParam("shipAddress") String shipAddress,
            @RequestParam(value = "note", required = false) String note,
            Authentication authentication,
            RedirectAttributes ra) {

        try {
            // 1. BẢO VỆ LỚP 1: Kiểm tra trạng thái đăng nhập
            if (authentication == null || !authentication.isAuthenticated()) {
                ra.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để đặt hàng!");
                return "redirect:/login";
            }

            // =================================================================
            // 2. BẢO VỆ LỚP 2: Lấy Customer ID an toàn (ĐÃ SỬA Ở ĐÂY)
            // =================================================================
            Integer customerId = getCustomerIdFromAuth(authentication);

            // 3. BẢO VỆ LỚP 3: Chặn đứng lỗi SQL INSERT NULL
            if (customerId == null) {
                ra.addFlashAttribute("errorMessage", "Phiên đăng nhập bị lỗi. Vui lòng đăng xuất và đăng nhập lại!");
                return "redirect:/login";
            }

            // 4. GỌI SERVICE TẠO ĐƠN HÀNG TRONG DATABASE
            Order newOrder = orderService.createOrder(
                    customerId, shipName, shipPhone, shipAddress, note, paymentMethod
            );

            // 5. ĐIỀU HƯỚNG THANH TOÁN
            if ("MOMO".equalsIgnoreCase(paymentMethod)) {
                String orderCode = newOrder.getOrderCode();
                String orderInfo = "Thanh toán đơn hàng Pet World - Mã: " + orderCode;

                // Gọi API MoMo với số tiền TotalAmount chính xác từ DB
                String payUrl = momoService.createPaymentUrl(orderCode, newOrder.getTotalAmount(), orderInfo);

                return "redirect:" + payUrl;

            } else {
                cartService.clearCart(customerId);
                // Trường hợp COD: Đơn hàng đã được tạo ở trạng thái 'pending'
                ra.addFlashAttribute("successMessage", "Order Completed Successfully!");
                ra.addFlashAttribute("order", newOrder);
                return "redirect:/cart/checkout-order";
            }

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi xử lý: " + e.getMessage());
            return "redirect:/cart/view";
        }
    }

    @GetMapping("/cart/remove/{id}")
    public String removeCartItem(@PathVariable("id") Integer cartItemId, RedirectAttributes ra) {
        try {
            cartService.removeCartItem(cartItemId);
            ra.addFlashAttribute("successMessage", "Deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Có lỗi xảy ra, không thể xóa sản phẩm!");
        }
        return "redirect:/cart/view";
    }


    @GetMapping("/cart/momo-return")
    public String momoReturn(@RequestParam("resultCode") String resultCode,
                             @RequestParam("orderId") String orderId, // orderId này chính là orderCode bạn gửi sang MoMo
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        // 1. Nếu giao dịch thành công (MoMo trả về resultCode = "0")
        if ("0".equals(resultCode)) {
            try {
                // Try find as Order payment first
                Order order = orderService.findByOrderCode(orderId);
                if (order != null) {
                    order.setStatus("paid");
                    orderService.updateOrder(order);
                    redirectAttributes.addFlashAttribute("order", order);

                    // Xóa cart khi thanh toán order
                    Integer customerId = getCustomerIdFromAuth(authentication);
                    if (customerId != null) {
                        cartService.clearCart(customerId);
                    }

                    redirectAttributes.addFlashAttribute("successMessage", "Đã thanh toán thành công đơn hàng " + orderId);
                    return "redirect:/cart/checkout-order";
                }

                // Appointment payment: orderId = "APTP{paymentId}T{timestamp}" hoặc số (legacy)
                Integer paymentId = parseAppointmentPaymentId(orderId);
                if (paymentId == null) {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy payment phù hợp.");
                    return "redirect:/customer/appointments";
                }
                Payment payment = paymentRepository.findById(paymentId).orElse(null);
                if (payment != null && payment.getAppointment() != null) {
                    Integer customerId = getCustomerIdFromAuth(authentication);
                    if (customerId != null
                            && payment.getAppointment().getCustomerId() != null
                            && payment.getAppointment().getCustomerId().equals(customerId)) {
                        boolean alreadyPaid = payment.getPaidAt() != null;
                        if (!alreadyPaid) {
                            payment.setPaidAt(java.time.LocalDateTime.now());
                            paymentRepository.save(payment);

                            Appointment appointment = payment.getAppointment();
                            Integer apptCustomerId = appointment.getCustomerId();
                            Customer notifyCustomer = apptCustomerId != null
                                    ? customerService.findById(apptCustomerId).orElse(null)
                                    : null;

                            String title = "Payment successful";
                            String apptCode = appointment.getAppointmentCode() != null ? appointment.getAppointmentCode() : "-";
                            String message = "You have successfully paid for appointment " + apptCode + ".";
                            notificationService.createForCustomer(notifyCustomer, appointment, title, message, "payment_success");
                        }

                        Integer appointmentId = payment.getAppointment().getId();
                        redirectAttributes.addFlashAttribute(
                                "message",
                                "Payment successful for appointment " + payment.getAppointment().getAppointmentCode()
                        );
                        return "redirect:/customer/appointments/" + appointmentId;
                    }
                }

                redirectAttributes.addFlashAttribute("error", "Payment not found or you do not have access.");
                return "redirect:/customer/appointments";

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Payment succeeded but the system failed to update. Please contact support.");
                return "redirect:/cart/checkout-order";
            }
        }

        // Failed payment
        try {
            Order order = orderService.findByOrderCode(orderId);
            if (order != null) {
                order.setStatus("cancled");
                orderService.updateOrder(order);
                redirectAttributes.addFlashAttribute("errorMessage", "Payment was not completed. Please try again.");
                return "redirect:/cart/view";
            }

            Integer paymentId = parseAppointmentPaymentId(orderId);
            if (paymentId != null) {
                Payment payment = paymentRepository.findById(paymentId).orElse(null);
                if (payment != null && payment.getAppointment() != null) {
                    Integer appointmentId = payment.getAppointment().getId();
                    redirectAttributes.addFlashAttribute("error", "Payment was not completed. Please try again.");
                    return "redirect:/customer/appointments/" + appointmentId;
                }
            }
        } catch (Exception e) {
            // ignore and redirect with generic error
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Payment was not completed. Please try again.");
        return "redirect:/cart/view";
    }

    /**
     * MOMO IPN (Instant Payment Notification) - callback từ MOMO server khi thanh toán hoàn tất.
     * Cập nhật trạng thái Paid ngay cả khi user đóng trình duyệt trước redirect.
     * Phải trả về HTTP 204 trong vòng 15 giây.
     */
    @PostMapping("/cart/momo-notify")
    @ResponseBody
    public ResponseEntity<Void> momoNotify(@RequestBody Map<String, Object> body) {
        if (!momoService.verifyIpnSignature(body)) {
            return ResponseEntity.status(400).build();
        }
        Object rc = body.get("resultCode");
        int resultCode = rc instanceof Number ? ((Number) rc).intValue() : -1;
        if (resultCode != 0 && resultCode != 9000) {
            return ResponseEntity.noContent().build(); // 204 - vẫn trả 204 cho MOMO
        }
        String orderId = body.get("orderId") != null ? body.get("orderId").toString() : null;
        if (orderId == null) return ResponseEntity.noContent().build();

        try {
            Order order = orderService.findByOrderCode(orderId);
            if (order != null) {
                order.setStatus("paid");
                orderService.updateOrder(order);
                return ResponseEntity.noContent().build();
            }
            Integer paymentId = parseAppointmentPaymentId(orderId);
            if (paymentId != null) {
                Payment payment = paymentRepository.findById(paymentId).orElse(null);
                if (payment != null && payment.getAppointment() != null && "service".equals(payment.getPaymentType())) {
                    if (payment.getPaidAt() == null) {
                        payment.setPaidAt(java.time.LocalDateTime.now());
                        paymentRepository.save(payment);

                        Appointment appointment = payment.getAppointment();
                        Integer apptCustomerId = appointment.getCustomerId();
                        Customer notifyCustomer = apptCustomerId != null
                                ? customerService.findById(apptCustomerId).orElse(null)
                                : null;

                        String title = "Payment successful";
                        String apptCode = appointment.getAppointmentCode() != null ? appointment.getAppointmentCode() : "-";
                        String message = "You have successfully paid for appointment " + apptCode + ".";
                        notificationService.createForCustomer(notifyCustomer, appointment, title, message, "payment_success");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return ResponseEntity.noContent().build();
    }

    /** orderId: "APTP{paymentId}T{timestamp}" hoặc số thuần (legacy) */
    private Integer parseAppointmentPaymentId(String orderId) {
        if (orderId == null) return null;
        if (orderId.startsWith("APTP")) {
            int tIdx = orderId.indexOf("T", 4);
            if (tIdx > 4) {
                try {
                    return Integer.parseInt(orderId.substring(4, tIdx));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        try {
            return Integer.parseInt(orderId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getCustomerIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // Nếu là Google
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            return customerService.findIdByEmail(email);
        }
        // Nếu là Customer lưu sẵn
        if (authentication.getPrincipal() instanceof Customer customer) {
            return customer.getCustomerId();
        }
        // Nếu là Form thường
        return customerService.findIdByUsername(authentication.getName());
    }


    @GetMapping("/cart/order-history")
    public String orderHistory(Model model, @PageableDefault(size = 10, page = 0) Pageable pageable, Authentication authentication) {
        Integer customerId = getCustomerIdFromAuth(authentication);

        Page<Order> orderHistory = orderService.getAllOrderById(pageable, customerService.getCustomerById(customerId));
        model.addAttribute("orderHistory",orderHistory);
        return "customer/order-history";
    }

    @PostMapping("/orders/cancel/{id}")
    public String cancelOrder(@PathVariable("id") Integer orderId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            // 1. Lấy thông tin khách hàng đang đăng nhập (Tận dụng hàm lấy ID bạn đã có)
            Integer customerId = getCustomerIdFromAuth(authentication);

            // 2. Tìm đơn hàng để kiểm tra quyền sở hữu (Bảo mật: Tránh việc khách A hủy đơn khách B)
            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

            if (!order.getCustomer().getCustomerId().equals(customerId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền hủy đơn hàng này!");
                return "redirect:/order-history";
            }

            // 3. Gọi Service để thực thi logic hủy (đã bao gồm trả lại Pet về AVAILABLE)
            orderService.cancelOrderById(orderId);

            // 4. Thông báo thành công qua URL parameter để HTML hiển thị Alert
            return "redirect:/order-history?canceledSuccess=true";

        } catch (RuntimeException e) {
            // Bắt các lỗi như: đơn không ở trạng thái pending, không tìm thấy đơn...
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/order-history";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi hệ thống khi hủy đơn.");
            return "redirect:/cart/order-history";
        }
    }


}
