package vn.edu.fpt.petworldplatform.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import vn.edu.fpt.petworldplatform.config.CustomUserDetails; // Import class này
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.CustomerService;

@Component
@RequiredArgsConstructor
public class SecuritySupport {
    private final CustomerService customerService;

    public Customer getCurrentAuthenticatedCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // 1. TRƯỜNG HỢP FORM LOGIN
        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Object account = userDetails.getAccount();

            if (account instanceof Customer) {
                return (Customer) account;
            }


        }
        // 2. TRƯỜNG HỢP GOOGLE LOGIN
        else if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");
            return customerService.findByEmail(email).orElse(null);
        }


        return null;
    }

    public Integer getCurrentAuthenticatedCustomerId() {
        Customer currentCustomer = getCurrentAuthenticatedCustomer();

        if (currentCustomer != null) {
            return currentCustomer.getCustomerId();
        }

        return null;
    }

    public Staff getCurrentAuthenticatedStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Object account = userDetails.getAccount();

            if (account instanceof Staff) {
                return (Staff) account;
            }
        }
        return null;
    }

    public Integer getCurrentAuthenticatedStaffId() {
        Staff currentStaff = getCurrentAuthenticatedStaff();
        return (currentStaff != null) ? currentStaff.getStaffId() : null;
    }
}