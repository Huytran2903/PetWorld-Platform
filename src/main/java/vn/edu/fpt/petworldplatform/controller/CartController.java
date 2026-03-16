package vn.edu.fpt.petworldplatform.controller;

import jakarta.persistence.Column;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.config.GlobalConfigAdvice;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.OrderRepo;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.repository.ProductRepo;
import vn.edu.fpt.petworldplatform.service.CartService;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.MomoService;
import vn.edu.fpt.petworldplatform.service.OrderService;

import java.math.BigDecimal;

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
    private ProductRepo productRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private PetRepo petRepo;

    @GetMapping("/cart/add-pet/{id}") // Khớp {id} với PathVariable
    public String addPetToCart(@PathVariable("id") Integer id,
                               Authentication authentication) {
        // 1. Kiểm tra đăng nhập
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // 2. Lấy Customer ID bằng hàm thông minh (Hỗ trợ cả Form và Google)
        Integer customerId = getCustomerIdFromAuth(authentication);

        if (customerId == null) {
            return "redirect:/login?error=account_not_found";
        }

        // 3. Tìm đối tượng Pet từ Database
        // Giả sử bạn đã @Autowired petRepository
        Pets pet = petRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pet not found with ID: " + id));

        // 4. Gọi Service xử lý (Lưu ý: pet truyền vào vị trí thứ 2, product là null)
        cartService.addToCart(customerId, pet, null, 1);

        // 5. BẮT BUỘC DÙNG REDIRECT
        // Để trình duyệt chuyển hướng hẳn về trang danh sách, tránh F5 bị add thêm pet
        return "redirect:/pets";
    }

    @GetMapping("/cart/add-product/{id}")
    public String addProductToCart(@PathVariable("id") Integer id,
                                   @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                                   Authentication authentication) {

        // 1. Kiểm tra đăng nhập
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // 2. Lấy Customer ID bằng hàm thông minh (Hỗ trợ cả Form và Google)
        Integer customerId = getCustomerIdFromAuth(authentication);

        if (customerId == null) {
            return "redirect:/login?error=account_not_found";
        }

        // 3. Tìm đối tượng Product từ Database
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));

        // 4. Gọi Service: Truyền Pet là null, truyền đối tượng Product và số lượng
        cartService.addToCart(customerId, null, product, qty);

        // 5. Redirect về danh sách sản phẩm để tránh lỗi F5 cộng dồn
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
        // 1. Kiểm tra xem có dữ liệu order được truyền sang không
        if (!model.containsAttribute("order")) {
            return "redirect:/"; // Tránh lỗi màn hình trắng khi người dùng nhấn F5
        }

        // 2. Lấy đối tượng order "tạm" từ bộ nhớ đệm
        Order tempOrder = (Order) model.getAttribute("order");

        if (tempOrder != null && tempOrder.getOrderID() != null) {
            // 3. Truy vấn lại Order THẬT từ Database để lấy toàn bộ dữ liệu (bao gồm cả OrderItems)
            // Chú ý: Đổi getOrderID() thành getter đúng của bạn nếu Lombok sinh tên khác
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
            Integer customerId = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof Customer customer) {
                // Trường hợp 1: Session lưu sẵn object Customer
                customerId = customer.getCustomerId();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
                // Trường hợp 2: Đăng nhập Google -> Lấy Email để dò ID
                String email = oauth2User.getAttribute("email");
                customerId = customerService.findIdByEmail(email);
            } else {
                // Trường hợp 3: Form thường nhưng chỉ có tên -> Lấy Username để dò ID
                customerId = customerService.findIdByUsername(authentication.getName());
            }
            // =================================================================

            // 3. BẢO VỆ LỚP 3: Chặn đứng lỗi SQL INSERT NULL
            if (customerId == null) {
                ra.addFlashAttribute("errorMessage", "Phiên đăng nhập bị lỗi. Vui lòng đăng xuất và đăng nhập lại!");
                return "redirect:/login";
            }

            // 4. GỌI SERVICE TẠO ĐƠN HÀNG TRONG DATABASE
            // Lưu ý: Mình đổi lại thành 'Orders' để khớp với tên Entity trong Database của bạn
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
            // Bắt các lỗi: Giỏ hàng trống, Sản phẩm hết hàng, Lỗi Database...
            ra.addFlashAttribute("errorMessage", "Lỗi xử lý: " + e.getMessage());
            return "redirect:/cart/view";
        }
    }

    @GetMapping("/cart/remove/{id}")
    public String removeCartItem(@PathVariable("id") Integer cartItemId, RedirectAttributes ra) {
        try {
            // Gọi Service để xóa
            cartService.removeCartItem(cartItemId);
            ra.addFlashAttribute("successMessage", "Đã xóa thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        // Xóa xong thì load lại đúng trang giỏ hàng đó
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
                // Bước 1: Tìm đơn hàng trong Database bằng mã orderCode
                Order order = orderService.findByOrderCode(orderId);

                if (order != null) {
                    // Bước 2: Cập nhật trạng thái thành ĐÃ THANH TOÁN
                    order.setStatus("paid");
                    orderService.updateOrder(order);
                    redirectAttributes.addFlashAttribute("order", order);
                }

                // Bước 3: Xóa sạch giỏ hàng của người dùng hiện tại
                Integer customerId = getCustomerIdFromAuth(authentication);
                if (customerId != null) {
                    cartService.clearCart(customerId);
                }

                // Bước 4: Báo thành công ra màn hình
                redirectAttributes.addFlashAttribute("successMessage", "Đã thanh toán thành công đơn hàng " + orderId);

            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thành công nhưng có lỗi cập nhật hệ thống.");
            }

            return "redirect:/cart/checkout-order";

        } else {
            try {
                Order order = orderService.findByOrderCode(orderId);
                if (order != null) {
                    order.setStatus("cancled");
                    orderService.updateOrder(order);
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán chưa hoàn tất. Vui lòng thử lại!");
            }

            return "redirect:/cart/view";
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
    public String orderHistory(Model model) {

        model.addAttribute("orderHistory", orderService.getAllOrder());
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
