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
import vn.edu.fpt.petworldplatform.util.SecuritySupport;

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
    private SecuritySupport securitySupport;
    

    @ModelAttribute("cartCount")
    public int getCartCountGlobal() {
        try {
            Integer customerId = securitySupport.getCurrentAuthenticatedCustomerId();

            if (customerId != null) {
                return cartService.getCountCartItems(customerId);
            }
        } catch (Exception e) {
            System.out.println("Lỗi đếm giỏ hàng: " + e.getMessage());
        }

        return 0;
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