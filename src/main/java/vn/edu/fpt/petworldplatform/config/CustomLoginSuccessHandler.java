package vn.edu.fpt.petworldplatform.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        request.getSession().setAttribute("loggedInAccount", userDetails.getAccount());

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