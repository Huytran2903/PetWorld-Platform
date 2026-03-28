package vn.edu.fpt.petworldplatform.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

        if (auth == null || !auth.isAuthenticated() || session.getAttribute("loggedInAccount") != null) {
            return true;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof String || "anonymousUser".equals(principal)) {
            return true;
        }

        if (principal instanceof CustomUserDetails userDetails) {
            session.setAttribute("loggedInAccount", userDetails.getAccount());
            return true;
        }

        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                return true;
            }
            customerService.findByEmail(email)
                    .ifPresentOrElse(
                            c -> session.setAttribute("loggedInAccount", c),
                            () -> staffService.findByEmail(email)
                                    .ifPresent(s -> session.setAttribute("loggedInAccount", s)));
            return true;
        }

        return true;
    }
}