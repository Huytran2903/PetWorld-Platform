package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.petworldplatform.entity.Pets;

import java.math.BigDecimal;

@Data
public class PetFormDTO {

    private Integer petID;

    @NotBlank(message = "Name is required")
    private String name;

    private String breed;

    @NotBlank(message = "Pet type is required")
    private String petType;

    @NotBlank(message = "Gender is required")
    private String gender;

    @Min(value = 0, message = "Age must be >= 0")
    private Integer ageMonths;

    @DecimalMin(value = "0.0", inclusive = true)
    private Double weightKg;

    private String color;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @Min(0)
    @Max(100)
    private Integer discountPercent;

    private Boolean isAvailable;

    private String description;

    private String imageUrl;

    public PetFormDTO(Pets pet) {
        this.petID = pet.getPetID();
        this.name = pet.getName();
        this.breed = pet.getBreed();
        this.petType = pet.getPetType();
        this.gender = pet.getGender();
        this.ageMonths = pet.getAgeMonths();
        this.weightKg = pet.getWeightKg();
        this.color = pet.getColor();
        this.price = pet.getPrice();
        this.discountPercent = pet.getDiscountPercent();
        this.description = pet.getDescription();
        this.isAvailable = pet.getIsAvailable();
        this.imageUrl = pet.getImageUrl();

    }



    public PetFormDTO() {
    }
}
