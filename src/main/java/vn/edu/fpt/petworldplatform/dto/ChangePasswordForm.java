package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.*;

public class ChangePasswordForm {

    @NotBlank(message = "Mật khẩu hiện tại không được rỗng")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được rỗng")
    @Size(min = 6, max = 100, message = "Mật khẩu mới phải từ 6 đến 100 ký tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được rỗng")
    private String confirmPassword;

    // ===== Getters & Setters =====

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}