package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProfileFormDTO {
    @NotBlank(message = "Full name không được để trống")
    private String fullName;

    // Username chỉ để hiện, không cần validate kỹ
    private String username;

    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,12}$", message = "Số điện thoại phải từ 10-12 số")
    private String phoneNumber;
}
