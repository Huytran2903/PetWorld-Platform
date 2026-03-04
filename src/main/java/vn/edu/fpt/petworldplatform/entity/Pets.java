package vn.edu.fpt.petworldplatform.entity;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import vn.edu.fpt.petworldplatform.dto.PetFormDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Pets")
@DynamicInsert
public class Pets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PetID")
    private Integer petID;

    @NotBlank(message = "Name is required!")
    @Pattern(
            regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$",
            message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\""
    )
    @Column(nullable = false, name = "Name")
    private String name;

    @NotBlank(message = "Type is required!")
    @Pattern(
            regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$",
            message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\""
    )
    @Column(name = "PetType")
    private String petType;

    @NotBlank(message = "Breed is required!")
    @Pattern(
            regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$",
            message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\""
    )
    @Column(name = "Breed")
    private String breed;

    @Column(name = "Gender")
    private String gender;

    @NotNull(message = "Age is required!")
    @Min(value = 1, message = "Age must be at least 1 month")
    @Max(value = 50, message = "Age cannot exceed 50 months")
    @Column(name = "AgeMonths")
    private Integer ageMonths;

    @NotNull(message = "Weight is required!")
    @Min(value = 1, message = "Weight must be at least 1kg")
    @Max(value = 100, message = "Weight must be less than 100kg")
    @Column(name = "WeightKg")
    private Double weightKg;

    @NotBlank(message = "Color is required!")
    @Pattern(
            regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$",
            message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\""
    )
    @Column(name = "Color")
    private String color;

    @Column(columnDefinition = "TEXT", name = "Note")
    private String note;

    @Column(name = "ImageUrl")
    private String imageUrl;

    @Column(columnDefinition = "TEXT", name = "Description")
    private String description;

    @NotNull(message = "Price is required!") // Bắt buộc nhập
    // Dùng DecimalMin/Max là chuẩn nhất cho tiền tệ (BigDecimal)
//    @DecimalMin(value = "300000", message = "Price must be at least 300,000")
//    @DecimalMax(value = "5000000", message = "Price must be at most 5,000,000")
    @Column(precision = 18, scale = 2, name = "Price")
    private BigDecimal price;

    @Column(precision = 18, scale = 2, name = "SalePrice")
    private BigDecimal salePrice;


    @Min(value = 0, message = "Discount cannot be negative")
    @Max(value = 100, message = "Discount cannot exceed 100%")
    @Column(name = "DiscountPercent")
    private Integer discountPercent;

    @Column(name = "IsAvailable")
    private Boolean isAvailable;


    @Column(name = "PurchasedAt")
    private LocalDateTime purchasedAt;

    @Column(updatable = false, name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "Species", length = 50)
    private String species;

    @ManyToOne
    @JoinColumn(name = "OwnerCustomerID")
    private Customer owner;

    public Integer getId() {
        return this.petID;
    }

    public void setId(Integer id) {
        this.petID = id;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Pets(PetFormDTO dto) {
        this.petID = dto.getPetID();
        this.name = dto.getName();
        this.breed = dto.getBreed();
        this.petType = dto.getPetType();
        this.gender = dto.getGender();
        this.ageMonths = dto.getAgeMonths();
        this.weightKg = dto.getWeightKg();
        this.color = dto.getColor();
        this.price = dto.getPrice();
        this.discountPercent = dto.getDiscountPercent();
        this.description = dto.getDescription();
        this.isAvailable = dto.getIsAvailable() != null ? dto.getIsAvailable() : true;
        this.imageUrl = dto.getImageUrl();
    }

}



