package vn.edu.fpt.petworldplatform.service;

import vn.edu.fpt.petworldplatform.dto.ChangePasswordForm;
import vn.edu.fpt.petworldplatform.dto.StaffProfileForm;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class StaffProfileService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffProfileService(StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -------------------------------------------------------
    // Lấy Staff theo ID
    // -------------------------------------------------------
    public Staff getStaffById(Integer staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff không tìm thấy"));
    }

    // -------------------------------------------------------
    // Update profile (email, phone, fullName, hireDate, bio)
    // -------------------------------------------------------
    @Transactional
    public Staff updateProfile(Integer staffId, StaffProfileForm form) {
        Staff staff = getStaffById(staffId);

        // Kiểm tra email đã dùng bởi người khác chưa
        if (form.getEmail() != null && !form.getEmail().equals(staff.getEmail())) {
            if (staffRepository.existsByEmail(form.getEmail())) {
                throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác");
            }
        }

        staff.setEmail(form.getEmail());
        staff.setPhone(form.getPhone());
        staff.setFullName(form.getFullName());
        staff.setHireDate(form.getHireDate());
        staff.setBio(form.getBio());
        staff.setUpdatedAt(LocalDateTime.now());

        return staffRepository.save(staff);
    }

    // -------------------------------------------------------
    // Đổi mật khẩu
    // -------------------------------------------------------
    @Transactional
    public void changePassword(Integer staffId, ChangePasswordForm form) {
        Staff staff = getStaffById(staffId);

        // Verify mật khẩu hiện tại
        if (!passwordEncoder.matches(form.getCurrentPassword(), staff.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        // Kiểm tra mật khẩu mới == confirm
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới và xác nhận không khớp");
        }

        staff.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        staff.setUpdatedAt(LocalDateTime.now());

        staffRepository.save(staff);
    }
}