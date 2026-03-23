package vn.edu.fpt.petworldplatform.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private StaffRepository staffRepo;

    private static final int MAX_FAILED_ATTEMPTS = 4;
    private static final int LOCK_TIME_DURATION_MINUTES = 2;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");

        // Nếu lỗi là do bị khóa thì đẩy ra lỗi khóa luôn
        if (exception instanceof LockedException) {
            super.setDefaultFailureUrl("/login?error=locked");
            super.onAuthenticationFailure(request, response, exception);
            return;
        }

        Customer customer = customerRepo.findByUsername(username).orElse(null);
        if (customer != null) {
            processCustomerFailure(customer);
        } else {
            Staff staff = staffRepo.findByUsername(username).orElse(null);
            if (staff != null) {
                processStaffFailure(staff);
            }
        }

        // Đẩy về trang login báo sai mật khẩu
        super.setDefaultFailureUrl("/login?error=bad_credentials");
        super.onAuthenticationFailure(request, response, exception);
    }

    private void processCustomerFailure(Customer customer) {
        int attempts = customer.getFailedAttempts() == null ? 0 : customer.getFailedAttempts();
        attempts++;
        customer.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            customer.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION_MINUTES));
        }
        customerRepo.save(customer);
    }

    private void processStaffFailure(Staff staff) {
        int attempts = staff.getFailedAttempts() == null ? 0 : staff.getFailedAttempts();
        attempts++;
        staff.setFailedAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            staff.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION_MINUTES));
        }
        staffRepo.save(staff);
    }
}