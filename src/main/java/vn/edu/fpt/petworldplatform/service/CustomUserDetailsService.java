package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import vn.edu.fpt.petworldplatform.entity.AccessControl;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.AccessControlRepository;
import vn.edu.fpt.petworldplatform.repository.CustomerRepository;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;
import vn.edu.fpt.petworldplatform.config.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private StaffRepository staffRepo;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccessControlRepository accessControlRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<Customer> customerOpt = customerRepository.findByUsername(username);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

            return new CustomUserDetails(customer.getUsername(), customer.getPasswordHash(), customer.getIsActive() != null ? customer.getIsActive() : true, authorities, customer);
        }

        Staff staff = staffRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Invalid account with username " + username));

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

        return new CustomUserDetails(staff.getUsername(), staff.getPasswordHash(), staff.getIsActive() != null ? staff.getIsActive() : true, authorities, staff);

    }

}