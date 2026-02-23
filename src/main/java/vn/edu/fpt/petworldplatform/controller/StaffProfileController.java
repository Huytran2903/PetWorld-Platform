package vn.edu.fpt.petworldplatform.controller;

import vn.edu.fpt.petworldplatform.dto.ChangePasswordForm;
import vn.edu.fpt.petworldplatform.dto.StaffProfileForm;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.StaffProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller Staff Profile
 *
 * Giả định: sau khi login, Authentication.getPrincipal() trả về Staff entity
 * (hoặc custom UserDetails chứa staffId).
 *
 * Nếu project dùng custom UserDetails khác, thay getStaffIdFromAuth() cho phù hợp.
 */
@Controller
@RequestMapping("/staff")
public class StaffProfileController {

    private final StaffProfileService service;

    public StaffProfileController(StaffProfileService service) {
        this.service = service;
    }

    // -------------------------------------------------------
    // Helper: lấy StaffID từ Authentication
    // -------------------------------------------------------
    private Integer getStaffIdFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Staff) {
            return ((Staff) auth.getPrincipal()).getStaffId();
        }
        throw new RuntimeException("Chưa đăng nhập hoặc Principal không hợp lệ");
    }

    // -------------------------------------------------------
    // GET /staff/profile — hiển thị form profile
    // -------------------------------------------------------
    @GetMapping("/profile")
    public String showProfile(Model model) {
        Integer staffId = getStaffIdFromAuth();
        Staff staff = service.getStaffById(staffId);

        // Nếu chưa có form trong model (không phải redirect sau lỗi), tạo mới từ entity
        if (!model.containsAttribute("profileForm")) {
            StaffProfileForm form = new StaffProfileForm();
            form.setEmail(staff.getEmail());
            form.setPhone(staff.getPhone());
            form.setFullName(staff.getFullName());
            form.setHireDate(staff.getHireDate());
            form.setBio(staff.getBio());
            model.addAttribute("profileForm", form);
        }

        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
        }

        model.addAttribute("staff", staff);
        return "staff/profile";
    }

    // -------------------------------------------------------
    // POST /staff/profile — update profile
    // -------------------------------------------------------
    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") StaffProfileForm form,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            // Trả về view trực tiếp (không redirect) để giữ BindingResult
            Integer staffId = getStaffIdFromAuth();
            Staff staff = service.getStaffById(staffId);

            // Thêm passwordForm rỗng cho section đổi mật khẩu
            model.addAttribute("passwordForm", new ChangePasswordForm());
            // Dùng model attribute cho staff
            model.addAttribute("staff", staff);
            return "staff/profile"; // Spring tự bind profileForm + errors vào model
        }

        try {
            Integer staffId = getStaffIdFromAuth();
            service.updateProfile(staffId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/staff/profile";
    }

    // -------------------------------------------------------
    // POST /staff/profile/change-password — đổi mật khẩu
    // -------------------------------------------------------
    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("passwordForm", form);
            redirectAttributes.addFlashAttribute("passwordErrors", result.getAllErrors());
            return "redirect:/staff/profile";
        }

        try {
            Integer staffId = getStaffIdFromAuth();
            service.changePassword(staffId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/staff/profile";
    }
}