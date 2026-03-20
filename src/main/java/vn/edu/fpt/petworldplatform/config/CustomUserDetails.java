package vn.edu.fpt.petworldplatform.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.time.LocalDateTime;
import java.util.Collection;

public class CustomUserDetails extends User {

    private final Object account;

    public CustomUserDetails(String username, String password, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities, Object account) {

        // Thay chữ 'true' thứ 3 bằng hàm checkLockStatus()
        super(username, password, enabled, true, true, checkLockStatus(account), authorities);
        this.account = account;
    }

    public Object getAccount() {
        return account;
    }

    // --- HÀM KIỂM TRA TRẠNG THÁI KHÓA ---
    private static boolean checkLockStatus(Object account) {
        if (account instanceof Customer) {
            Customer customer = (Customer) account;
            // Nếu LockedUntil khác null VÀ thời gian khóa vẫn còn ở TƯƠNG LAI -> BỊ KHÓA (return false)
            if (customer.getLockedUntil() != null && customer.getLockedUntil().isAfter(LocalDateTime.now())) {
                return false;
            }
        } else if (account instanceof Staff) {
            Staff staff = (Staff) account;
            if (staff.getLockedUntil() != null && staff.getLockedUntil().isAfter(LocalDateTime.now())) {
                return false;
            }
        }

        return true;
    }
}