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

    private long calculateLockTimeInMinutes(int failedAttempts) {
        if (failedAttempts < 4) return 0;

        switch (failedAttempts) {
            case 4:
                return 2;
            case 5:
                return 5;
            case 6:
                return 15;
            case 7:
                return 30;
            default:
                return 60;
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");

        // Nếu lỗi là do tài khoản đang trong thời gian bị khóa
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

        super.setDefaultFailureUrl("/login?error=bad_credentials");
        super.onAuthenticationFailure(request, response, exception);
    }

    private void processCustomerFailure(Customer customer) {
        int attempts = customer.getFailedAttempts() == null ? 0 : customer.getFailedAttempts();
        attempts++;
        customer.setFailedAttempts(attempts);

        long lockMinutes = calculateLockTimeInMinutes(attempts);

        if (lockMinutes > 0) {
            customer.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        }

        customerRepo.save(customer);
    }

    private void processStaffFailure(Staff staff) {
        int attempts = staff.getFailedAttempts() == null ? 0 : staff.getFailedAttempts();
        attempts++;
        staff.setFailedAttempts(attempts);

        long lockMinutes = calculateLockTimeInMinutes(attempts);

        if (lockMinutes > 0) {
            staff.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        }

        staffRepo.save(staff);
    }
}