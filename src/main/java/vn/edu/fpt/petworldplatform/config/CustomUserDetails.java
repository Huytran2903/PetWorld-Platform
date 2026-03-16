package vn.edu.fpt.petworldplatform.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {

    private final Object account;

    public CustomUserDetails(String username, String password, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities, Object account) {
        super(username, password, enabled, true, true, true, authorities);
        this.account = account;
    }

    public Object getAccount() {
        return account;
    }
}