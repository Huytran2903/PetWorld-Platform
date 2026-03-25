package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffFormDTO {
    private Integer staffId;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s]+$", message = "Full name must contain letters and spaces only, no numbers")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 60, message = "Username must be between 4 and 60 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Invalid phone number format")
    private String phone;

    @NotNull(message = "Please select a role")
    private Integer roleId;
}
