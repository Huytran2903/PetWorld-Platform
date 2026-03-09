package vn.edu.fpt.petworldplatform.controller;

import jakarta.persistence.Column;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

        // 2. Lấy Customer trực tiếp từ Principal (Vì bạn đã lưu nó lúc login)
        // Cách này giúp tránh lỗi "Customer not found with username"
        Integer customerId;
        if (authentication.getPrincipal() instanceof Customer customer) {
            customerId = customer.getCustomerId();
        } else {
            // Phòng hờ trường hợp login khác (OAuth2)
            customerId = customerService.findIdByUsername(authentication.getName());
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

        // 2. Lấy Customer ID trực tiếp từ Principal (Giống hệt addPetToCart)
        // Cách này giúp tránh lỗi "Customer not found" khi getName() trả về chuỗi lạ
        Integer customerId;
        if (authentication.getPrincipal() instanceof Customer customer) {
            customerId = customer.getCustomerId(); // Lấy ID trực tiếp từ đối tượng trong Session
        } else {
            // Dự phòng cho trường hợp đăng nhập bằng OAuth2/Google
            customerId = customerService.findIdByUsername(authentication.getName());
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

        // Lấy đối tượng Principal (chính là đối tượng customer bạn đã lưu lúc login)
        Object principal = authentication.getPrincipal();

        Integer customerId;
        if (principal instanceof Customer customer) {
            // Lấy trực tiếp ID từ đối tượng trong phiên đăng nhập
            customerId = customer.getCustomerId();
        } else {
            // Trường hợp phòng hờ nếu là OAuth2 hoặc kiểu khác
            customerId = customerService.findIdByUsername(authentication.getName());
        }

        Carts cart = cartService.getCartDetail(customerId);
        model.addAttribute("cart", cart);

        // 1. Lấy Subtotal từ Service (kết quả trả về đã là BigDecimal)
        BigDecimal subtotal = cartService.calculateSubtotal(cart);

        // 2. Tính thuế 10% (Nhân với 0.1)
        // Chú ý: Dùng String "0.1" trong constructor để đảm bảo độ chính xác tuyệt đối
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05"));

        // 3. Tính tổng cộng (Subtotal + Tax)
        BigDecimal total = subtotal.add(tax);

        // 4. Đưa dữ liệu ra giao diện (Thymeleaf sẽ nhận các đối tượng BigDecimal này)
        model.addAttribute("cart", cart);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("tax", tax);
        model.addAttribute("total", total);
        return "customer/shopping-cart";
    }

    // Viết thêm hàm nhỏ này xuống cuối file Controller để dùng chung cho gọn
    private String getIdentifier(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            // Đăng nhập Google -> Lấy Email
            return oauth2Token.getPrincipal().getAttribute("email");
        }
        // Đăng nhập Form thường -> Lấy Username
        return authentication.getName();
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

    @ModelAttribute("cartCount")
    public int getCartCount(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            // Lấy ID khách hàng từ Principal để đếm
            Integer customerId;
            if (authentication.getPrincipal() instanceof Customer customer) {
                customerId = customer.getCustomerId();
            } else {
                customerId = customerService.findIdByUsername(authentication.getName());
            }
            return cartService.getCountCartItems(customerId);
        }
        return 0; // Trả về 0 nếu chưa đăng nhập
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

            // 2. BẢO VỆ LỚP 2: Lấy Customer ID an toàn
            Integer customerId = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof Customer customer) {
                // Chú ý: Đảm bảo tên getter khớp với Entity của bạn (getCustomerId hoặc getCustomerID)
                customerId = customer.getCustomerId();
            }

            // Nếu lấy từ Session bị null, thử móc lại từ Database bằng username
            if (customerId == null) {
                // Lấy trực tiếp con số ID gán vào biến customerId
                customerId = customerService.findIdByUsername(authentication.getName());
            }

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
                             @RequestParam("orderId") String orderId,
                             RedirectAttributes redirectAttributes) {

        // MoMo quy ước resultCode = "0" là giao dịch thành công
        if ("0".equals(resultCode)) {

            // ==========================================
            // BƯỚC 1: XỬ LÝ DATABASE (Bạn sẽ code thêm phần này)
            // - Tìm đơn hàng có mã orderId này trong DB.
            // - Cập nhật trạng thái đơn hàng thành "ĐÃ THANH TOÁN" (PAID).
            // - Lấy ID khách hàng hiện tại và xóa sạch các món trong giỏ hàng.
            // ==========================================

            // BƯỚC 2: GỬI THÔNG BÁO THÀNH CÔNG SANG VIEW
            redirectAttributes.addFlashAttribute("successMessage", "Tuyệt vời! Bạn đã thanh toán thành công đơn hàng " + orderId);

            // BƯỚC 3: CHUYỂN HƯỚNG VỀ TRANG CHECKOUT-ORDER NHƯ BẠN MUỐN
            return "redirect:/cart/checkout-order";

        } else {
            // Nếu giao dịch thất bại (Khách hủy, hết tiền, lỗi...)
            redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán chưa hoàn tất. Vui lòng thử lại!");
            return "redirect:/cart/view"; // Quay lại giỏ hàng
        }
    }

}
