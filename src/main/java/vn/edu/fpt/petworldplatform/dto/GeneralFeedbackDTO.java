package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GeneralFeedbackDTO {
    private String subject;
    
    @NotBlank(message = "Comment is required!")
    private String comment;
    
    private String imageUrls;
    
    @Email(message = "Please enter a valid email address")
    private String email;
    
    private String phoneNumber;
}
