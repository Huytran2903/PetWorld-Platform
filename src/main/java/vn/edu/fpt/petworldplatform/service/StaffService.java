package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.dto.StaffFormDTO;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepo;
    private final PetVaccinationRepository petVaccinationRepo;
    private final PetHealthRecordRepository petHealthRecordRepo;
    private final StaffScheduleRepository staffScheduleRepo;
    private final FeedbackRepository feedbackRepo;
    private final AppointmentServiceLineRepository appointmentServiceRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    //Register - HuyTPN
    public Staff register(Staff staff) {
        staff.setPasswordHash(passwordEncoder.encode(staff.getPasswordHash()));
        staff.setIsActive(true);
        return staffRepo.save(staff);
    }


    public Optional<Staff> findByUsername(String username) {
        return staffRepo.findByUsername(username);
    }

    public Optional<Staff> findByEmail(String email) {
        return staffRepo.findByEmail(email);
    }

    public List<Staff> getAvailableStaffs() {
        return staffRepo.findByIsActiveTrue();
    }

    public Boolean isEmailExists(String email) {
        return staffRepo.existsByEmail(email);
    }

    public Boolean isPhoneExists(String phone) {
        return staffRepo.existsByPhone(phone);
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

    public Page<Staff> getStaffsWithPaginationAndSearch(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("staffId").descending());

        if (keyword != null && !keyword.trim().isEmpty()) {
            return staffRepo.searchStaffs(keyword.trim(), pageable);
        }
        return staffRepo.findAll(pageable);
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


    @Transactional
    public void deleteAndTransferWork(Integer oldStaffId, Integer newStaffId) {

        // 1. Kiểm tra nhân viên cũ có tồn tại không
        Staff oldStaff = staffRepo.findById(oldStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên cần xóa!"));

        // ==========================================
        // CHỐT CHẶN BẢO VỆ NGHIỆP VỤ: KIỂM TRA IN-PROGRESS
        // ==========================================
        long inProgressCount = appointmentServiceRepo.countInProgressServices(oldStaffId);

        if (inProgressCount > 0) {
            throw new IllegalStateException("Hủy thao tác! Nhân viên này đang có " + inProgressCount + " dịch vụ đang thực hiện (In-Progress). Vui lòng hoàn thành hoặc gán lại dịch vụ trước khi xóa.");
        }

        // 2. Kiểm tra nhân viên mới (nếu có chọn bàn giao)
        if (newStaffId != null) {
            if (!staffRepo.existsById(newStaffId)) {
                throw new IllegalArgumentException("Không tìm thấy nhân viên nhận bàn giao!");
            }
        }

        // ==========================================
        // BƯỚC 1: XỬ LÝ VIỆC TƯƠNG LAI / CHƯA HOÀN THÀNH
        // ==========================================
        if (newStaffId != null) {
            // Có người nhận -> Bàn giao việc dở dang
            petVaccinationRepo.transferFutureVaccinations(oldStaffId, newStaffId);
            appointmentServiceRepo.transferPendingServices(oldStaffId, newStaffId);
        } else {
            // Không ai nhận -> Trả dịch vụ về trạng thái vô chủ
            appointmentServiceRepo.unassignPendingServices(oldStaffId);
        }

        // ==========================================
        // BƯỚC 2: CHỐT CHẶN KHÓA NGOẠI (QUÉT SẠCH DẤU VẾT QUÁ KHỨ)
        // ==========================================
        petVaccinationRepo.clearAllVaccinationReferences(oldStaffId);
        appointmentServiceRepo.clearAllStaffReferences(oldStaffId);
        petHealthRecordRepo.unassignAllHealthRecords(oldStaffId);

        // ==========================================
        // BƯỚC 3: XÓA DỮ LIỆU PHỤ THUỘC & XÓA STAFF
        // ==========================================
        staffScheduleRepo.deleteByStaff(oldStaff);
        feedbackRepo.deleteByStaff(oldStaff);

        staffRepo.delete(oldStaff);
    }

    public Optional<Staff> findById(Integer id) {
        return staffRepo.findById(id);
    }
}
