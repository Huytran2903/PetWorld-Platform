package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ServiceReviewDTO {

    @NotNull(message = "Please select a service to review")
    private Integer serviceId;

    private String subject;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @NotBlank(message = "Comment is required")
    private String comment;

    private String imageUrls;
}
