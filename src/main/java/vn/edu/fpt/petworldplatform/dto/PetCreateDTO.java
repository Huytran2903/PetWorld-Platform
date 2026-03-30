package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PetCreateDTO {

    private String createPetOwnerType;

    @NotBlank(message = "Pet name is required.")
    @Pattern(regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$", message = "Pet name must start with an uppercase letter and contain letters/spaces only.")
    private String name;

    @NotBlank(message = "Species is required.")
    @Pattern(regexp = "^(Dog|Cat|Other)$", message = "Species must be Dog, Cat, or Other.")
    private String species;

    @NotBlank(message = "Breed is required.")
    @Pattern(regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$", message = "Breed must start with an uppercase letter and contain letters/spaces only.")
    private String breed;

    @NotNull(message = "Age is required.")
    @Min(value = 1, message = "Age must be at least 1 month.")
    @Max(value = 50, message = "Age must not exceed 50 months.")
    private Integer age;

    private Integer ownerId;

    private Double price;
    private String imageUrl;
    private String description;

    @NotNull(message = "Weight is required.")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg.")
    @DecimalMax(value = "100.0", message = "Weight must not exceed 100 kg.")
    private Double weightKg;

    @NotBlank(message = "Color is required.")
    @Pattern(regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$", message = "Color must start with an uppercase letter and contain letters/spaces only.")
    private String color;

    @NotBlank(message = "Gender is required.")
    @Pattern(regexp = "^(Male|Female)$", message = "Gender must be Male or Female.")
    private String gender;

    private String note;

    private MultipartFile imageFile;
}
