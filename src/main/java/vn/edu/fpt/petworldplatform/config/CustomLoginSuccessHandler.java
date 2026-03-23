package vn.edu.fpt.petworldplatform.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private StaffRepository staffRepo;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        request.getSession().setAttribute("loggedInAccount", userDetails.getAccount());

        Object account = userDetails.getAccount();

        if (account instanceof Customer) {
            Customer cus = (Customer) account;
            if (cus.getFailedAttempts() != null && cus.getFailedAttempts() > 0) {
                cus.setFailedAttempts(0);
                cus.setLockedUntil(null);
                customerRepo.save(cus);
            }
        } else if (account instanceof Staff) {
            Staff stf = (Staff) account;
            if (stf.getFailedAttempts() != null && stf.getFailedAttempts() > 0) {
                stf.setFailedAttempts(0);
                stf.setLockedUntil(null);
                staffRepo.save(stf);
            }
        }

        String redirectUrl = "/";
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            String role = auth.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/reports/revenue";
                break;
            } else if (role.equals("ROLE_CUSTOMER")) {
                redirectUrl = "/";
                break;
            } else if (role.startsWith("ROLE_")) {
                redirectUrl = "/staff/assigned_list";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}