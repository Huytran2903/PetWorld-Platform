package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.StaffRepo;

import java.util.Optional;

@Service
public class StaffService {

    @Autowired
    private StaffRepo staffRepo;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    //Register - HuyTPN
    public Staff register(Staff staff) {
        staff.setPasswordHash(passwordEncoder.encode(staff.getPasswordHash()));
        staff.setIsActive(true);
        return staffRepo.save(staff);
    }

    //Login - HuyTPN
    public Optional<Staff> login(String username, String rawPassword) {
        Optional<Staff> staffOpt = staffRepo.findByUsername(username);

        if (staffOpt.isPresent()) {
            if (passwordEncoder.matches(rawPassword, staffOpt.get().getPasswordHash())) {
                return staffOpt;
            }
        }
        return Optional.empty();
    }

    public void updateCustomer(Staff staff) {
        staffRepo.save(staff);
    }
}
