package vn.edu.fpt.petworldplatform.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.StaffService;

@Component
@RequiredArgsConstructor
public class GlobalInterceptor implements HandlerInterceptor {

    private final CustomerService customerService;
    private final StaffService staffService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String) // Không phải người dùng ẩn danh
                && session.getAttribute("loggedInAccount") == null) {

            // Handle CustomUserDetails (traditional login)
            if (auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                Object account = userDetails.getAccount();
                session.setAttribute("loggedInAccount", account);
            }
            // Handle DefaultOidcUser (OAuth2 - Google, GitHub, etc.)
            else if (auth.getPrincipal() instanceof DefaultOidcUser) {
                // OAuth2 users don't have a local account object yet
                // You can handle this case as needed (e.g., create account on-the-fly)
                // For now, we skip storing in session
            }
        }
        return true;
    }
}