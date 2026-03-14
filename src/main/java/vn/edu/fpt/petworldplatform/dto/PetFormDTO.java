package vn.edu.fpt.petworldplatform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import vn.edu.fpt.petworldplatform.entity.Pets;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    @NotNull(message = "Price must be require")
    private BigDecimal price;

    @Min(0)
    @Max(100)
    private Integer discountPercent = 0;

    private Boolean isAvailable;

    private String description;

    private String imageUrl;

    private Boolean isVaccinated = false;

    private Integer vaccinationStaffID;

    private String vaccineName;

    private String vaccineNote;

    private LocalDate nextDueDate;

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

        if (pet.getVaccinations() != null && !pet.getVaccinations().isEmpty()) {
            this.isVaccinated = true;
        } else {
            this.isVaccinated = false;
        }
    }

    public PetFormDTO() {
    }
}