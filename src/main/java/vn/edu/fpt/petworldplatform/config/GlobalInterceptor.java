package vn.edu.fpt.petworldplatform.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

            // Lấy dữ liệu từ thẻ VIP của Spring Security
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            Object account = userDetails.getAccount();

            // "Bơm" ngược lại vào Session cho bạn
            session.setAttribute("loggedInAccount", account);

        }
        return true;
    }
}