package vn.edu.fpt.petworldplatform.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class GlobalInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String) // Không phải người dùng ẩn danh
                && session.getAttribute("loggedInAccount") == null) {

            Object principal = auth.getPrincipal();

            // ── Handle CustomUserDetails (local login) ──
            if (principal instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) principal;
                Object account = userDetails.getAccount();
                session.setAttribute("loggedInAccount", account);
            }
            // ── Handle OAuth2 users (Google, GitHub, etc) ──
            else if (principal instanceof DefaultOidcUser) {
                DefaultOidcUser oidcUser = (DefaultOidcUser) principal;
                // Optionally store OAuth2 user info in session
                String email = oidcUser.getEmail();
                String name = oidcUser.getFullName();
                session.setAttribute("loggedInOAuth2User", oidcUser);
                // You can add more attributes as needed
            }
        }
        return true;
    }
}