package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.dto.StaffFormDTO;
import vn.edu.fpt.petworldplatform.entity.Role;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.RoleRepo;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;

import java.util.List;
import java.util.Optional;

@Service
public class StaffService {

    @Autowired
    private StaffRepository staffRepo;

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
        String input = usernameOrEmail == null ? "" : usernameOrEmail.trim();

        Optional<Staff> staffOpt = staffRepo.findByUsernameIgnoreCase(input);

        if (staffOpt.isEmpty()) {
            staffOpt = staffRepo.findByEmailIgnoreCase(input);
        }

        if (staffOpt.isPresent()) {
            String storedHash = staffOpt.get().getPasswordHash();

            boolean passwordValid = false;
            if (storedHash != null) {
                if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
                    passwordValid = passwordEncoder.matches(rawPassword, storedHash);
                } else {
                    passwordValid = rawPassword.equals(storedHash);
                }
            }

            if (passwordValid) {
                return staffOpt;
            }
        }

        return Optional.empty();
    }

    public void updateCustomer(Staff staff) {
        staffRepo.save(staff);
    }

    public Optional<Staff> findByUsername(String username) {
        return staffRepo.findByUsername(username);
    }

    public Optional<Staff> findByEmail(String email) {
        return staffRepo.findByEmail(email);
    }

    public Optional<Staff> findById(Long staffId) {
        if (staffId == null) {
            return Optional.empty();
        }
        return staffRepo.findById(staffId.intValue());
    }

    public List<Staff> getAllStaffs() {
        return staffRepo.findAll();
    }

    public Staff createStaff(StaffFormDTO staffDTO) {
        if (staffRepo.existsByEmail(staffDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (staffRepo.findByUsername(staffDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Role role = roleRepo.findById(staffDTO.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Staff staff = Staff.builder()
                .fullName(staffDTO.getFullName())
                .username(staffDTO.getUsername())
                .email(staffDTO.getEmail())
                .phone(staffDTO.getPhone())
                .role(role)
                .isActive(true)
                .build();

        String defaultPassword = staffDTO.getUsername() + "@123";
        staff.setPasswordHash(passwordEncoder.encode(defaultPassword));

        return staffRepo.save(staff);
    }

    public StaffFormDTO getStaffDtoById(Integer id) {
        Staff staff = staffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        StaffFormDTO dto = new StaffFormDTO();
        dto.setStaffId(staff.getStaffId());
        dto.setFullName(staff.getFullName());
        dto.setUsername(staff.getUsername());
        dto.setEmail(staff.getEmail());
        dto.setPhone(staff.getPhone());
        dto.setRoleId(staff.getRole() != null ? staff.getRole().getRoleId() : null);
        return dto;
    }

    public Staff updateStaff(StaffFormDTO staffDTO) {
        Staff staff = staffRepo.findById(staffDTO.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        Role role = roleRepo.findById(staffDTO.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        staff.setFullName(staffDTO.getFullName());
        staff.setEmail(staffDTO.getEmail());
        staff.setPhone(staffDTO.getPhone());
        staff.setRole(role);

        return staffRepo.save(staff);
    }

    public void deleteStaff(Integer id) {
        Staff staff = staffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        staffRepo.delete(staff);
    }
}
