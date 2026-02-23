package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.dto.StaffFormDTO;
import vn.edu.fpt.petworldplatform.entity.Role;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.RoleRepo;
import vn.edu.fpt.petworldplatform.repository.StaffRepo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StaffService {

    @Autowired
    private StaffRepo staffRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RoleRepo roleRepo;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    //Register - HuyTPN
    public Staff register(Staff staff) {
        staff.setPasswordHash(passwordEncoder.encode(staff.getPasswordHash()));
        staff.setIsActive(true);
        return staffRepo.save(staff);
    }

    //Login - HuyTPN
    public Optional<Staff> login(String usernameOrEmail, String rawPassword) {
        Optional<Staff> staffOpt = staffRepo.findByEmail(usernameOrEmail);

        if (staffOpt.isEmpty()) {
            staffOpt = staffRepo.findByUsername(usernameOrEmail);
        }

        if (staffOpt.isPresent()) {
            if (passwordEncoder.matches(rawPassword, staffOpt.get().getPasswordHash())) {
                return staffOpt;
            }
        }

        return Optional.empty();
    }

    public List<Staff> getAllStaffs() {
        return staffRepo.findAll();
    }

    @Transactional
    public void createStaff(StaffFormDTO dto) {
        Staff staffEntity = new Staff();

        staffEntity.setFullName(dto.getFullName());
        staffEntity.setUsername(dto.getUsername());
        staffEntity.setEmail(dto.getEmail());
        staffEntity.setPhone(dto.getPhone());

        Role role = (Role) roleRepo.findById(dto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        staffEntity.setRole(role);

        // 3. Xử lý các trường dữ liệu ngầm (Business Logic)
        String rawPassword = UUID.randomUUID().toString().substring(0, 8);
        String hashed = passwordEncoder.encode(rawPassword);

        staffEntity.setPasswordHash(hashed);
        staffEntity.setIsActive(true);
        staffEntity.setHireDate(LocalDate.now());

        staffRepo.save(staffEntity);

        String subject = "Welcome to Pet World - Your Staff Account";
        String htmlContent = "<html>" +
                "<body>" +
                "  <h2>Hello " + dto.getFullName() + ",</h2>" +
                "  <p>Your staff account has been created successfully.</p>" +
                "  <p><b>Username:</b> " + dto.getUsername() + "</p>" +
                "  <p><b>Password:</b> <span style='color: #e67e22; font-weight: bold;'>" + rawPassword + "</span></p>" +
                "  <p>Please log in and change your password immediately.</p>" +
                "  <br><p>Best regards,<br>Pet World Admin Team</p>" +
                "</body>" +
                "</html>";

        emailService.sendEmail(dto.getEmail(), subject, htmlContent);
    }

    @Transactional
    public void updateStaff(StaffFormDTO dto) {

        Staff existingStaff = staffRepo.findById(dto.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        existingStaff.setFullName(dto.getFullName());
        existingStaff.setUsername(dto.getUsername());
        existingStaff.setEmail(dto.getEmail());
        existingStaff.setPhone(dto.getPhone());

        Role role = (Role) roleRepo.findById(dto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        existingStaff.setRole(role);

        staffRepo.save(existingStaff);
    }

    public StaffFormDTO getStaffDtoById(Integer id) {
        Staff staff = staffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found with ID: " + id));

        StaffFormDTO dto = new StaffFormDTO();
        dto.setStaffId(staff.getStaffId());
        dto.setFullName(staff.getFullName());
        dto.setUsername(staff.getUsername());
        dto.setEmail(staff.getEmail());
        dto.setPhone(staff.getPhone());

        if (staff.getRole() != null) {
            dto.setRoleId(staff.getRole().getRoleId());
        }

        return dto;
    }


    @Transactional
    public void deleteStaff(Integer id) {
        staffRepo.deleteById(id);
    }
}
