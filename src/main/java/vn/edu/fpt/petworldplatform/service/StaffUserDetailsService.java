package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import vn.edu.fpt.petworldplatform.entity.AccessControl;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.AccessControlRepository;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class StaffUserDetailsService implements UserDetailsService {

    @Autowired
    private StaffRepository staffRepo;

    @Autowired
    private AccessControlRepository accessControlRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Staff staff = staffRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản nhân viên: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();

        if (staff.getRole() != null) {
            List<AccessControl> accessList = accessControlRepo.findByRoleId(staff.getRole().getRoleId());

            for (AccessControl acc : accessList) {
                if (Boolean.TRUE.equals(acc.getIsAllowed())) {
                    authorities.add(new SimpleGrantedAuthority(acc.getPermissionCode()));
                }
            }

            authorities.add(new SimpleGrantedAuthority("ROLE_" + staff.getRole().getRoleName().toUpperCase()));
        }

        return new User(
                staff.getUsername(),
                staff.getPasswordHash(),
                staff.getIsActive() != null ? staff.getIsActive() : true,
                true,
                true,
                true,
                authorities
        );
    }
}