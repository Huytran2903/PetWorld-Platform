package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileFormDTO {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s]+$", message = "Full name must contain letters and spaces only, no numbers")
    private String fullName;

    private String username;

    @NotBlank(message = "Email is require")
    private String email;

    @NotBlank(message = "Phone number is require")
    @Pattern(regexp = "^[0-9]{10,12}$", message = "Phone number must be 10-12 digits")
    private String phoneNumber;
}
