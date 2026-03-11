package vn.edu.fpt.petworldplatform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.SystemConfigs;
import vn.edu.fpt.petworldplatform.service.CartService;
import vn.edu.fpt.petworldplatform.service.ConfigService;
import vn.edu.fpt.petworldplatform.service.CustomerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalConfigAdvice {

    @Autowired
    private ConfigService configService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CustomerService customerService;

    // Hàm này sẽ tự động chạy ở MỌI TRANG để đếm giỏ hàng
    @ModelAttribute("cartCount")
    public int getCartCountGlobal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return 0; // Chưa đăng nhập thì giỏ = 0
        }
        try {
            Integer customerId = getCustomerIdFromAuth(authentication);
            if (customerId != null) {
                return cartService.getCountCartItems(customerId);
            }
        } catch (Exception e) {
            System.out.println("Lỗi đếm giỏ hàng: " + e.getMessage());
        }
        return 0;
    }

    // Hàm xử lý ID thông minh (Google & Form) mang sang đây dùng chung
    private Integer getCustomerIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // Nếu là Google
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
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

    @ModelAttribute("globalConfigs")
    public Map<String, String> populateGlobalConfigs() {
        List<SystemConfigs> configsList = configService.getAllConfigs();

        Map<String, String> configMap = new HashMap<>();

        for (SystemConfigs config : configsList) {
            if (config.getConfigValue() != null) {
                configMap.put(config.getConfigKey(), config.getConfigValue());
            }
        }

        return configMap;
    }


}