package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Form binding cho POST /staff/profile (chỉ update các field cho phép)
 * Username, PasswordHash, IsActive, CreatedAt -> không đổi được từ UI profile
 */
public class StaffProfileForm {

    @NotBlank(message = "Email không được rỗng")
    @Email(message = "Email không hợp lệ")
    @Size(max = 120, message = "Email tối đa 120 ký tự")
    private String email;

    @Size(max = 20, message = "Phone tối đa 20 ký tự")
    private String phone;

    @Size(max = 120, message = "Tên tối đa 120 ký tự")
    private String fullName;

    private LocalDate hireDate;

    @Size(max = 300, message = "Bio tối đa 300 ký tự")
    private String bio;

    // ===== Getters & Setters =====

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
