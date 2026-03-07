package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.repository.ProductRepo;
import vn.edu.fpt.petworldplatform.service.CartService;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.PetService;
import vn.edu.fpt.petworldplatform.service.ProductService;

import java.security.Principal;
//import vn.edu.fpt.petworldplatform.service.PetService;

@Controller
public class ProductController {

    @Autowired
    private PetService petService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private PetRepo petRepo;

    //Product List
    @GetMapping("/products")
    public String getAllProducts(Model model, @RequestParam(name = "kw", required = false, defaultValue = "") String keyword) {

        if(!keyword.equals("")) {
            model.addAttribute("product", productService.searchProductsByName(keyword   ));
        }
        else {
            model.addAttribute("product", productService.getAllProducts());
        }
            return "/product/productList";

    }

    //Pet List
    @GetMapping("/pets")
    public String getAllPet(Model model) {
        model.addAttribute("pet", petService.getAllPets());
        return "/product/petList";
    }

    //Product Detail
    @GetMapping("/product/detail/{id}")
    public String productDetail(Model model, @PathVariable("id") Integer id) {
        model.addAttribute("proDetail", productService.getProductById(id));
        return "product/product-detail";
    }

    //Pet Detail
    @GetMapping("/pet/detail/{id}")
    public String petDetail(Model model, @PathVariable("id") Integer id) {
        model.addAttribute("petDetail", petService.getPetById(id));
        return "product/pet-detail";
    }

    @GetMapping("/cart/add/{id}") // Khớp {id} với PathVariable
    public String addToCart(@PathVariable("id") Integer id,
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

    @GetMapping("/add-product/{id}")
    public String addProductToCart(@PathVariable("id") Integer id, // Đổi thành Integer cho an toàn
                                   @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                                   Authentication authentication) { // Đổi Principal thành Authentication

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // 1. Lấy chuẩn Username hoặc Email để không bị lỗi 500
        String identifier = getIdentifier(authentication);

        // 2. Tìm Customer ID
        Integer customerId = customerService.findIdByUsername(identifier);

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        // 4. GỌI SERVICE: Truyền đối tượng product vào thay vì truyền id
        cartService.addToCart(customerId, null, product, qty);

        // 4. BẮT BUỘC DÙNG REDIRECT để tránh lỗi F5 cộng dồn sản phẩm
        return "redirect:/product/productList";
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

}
