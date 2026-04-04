package vn.edu.fpt.petworldplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.format.annotation.DateTimeFormat;
import vn.edu.fpt.petworldplatform.dto.PetFormDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @Pattern(regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$", message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\"")
    @Column(nullable = false, name = "Name")
    private String name;

    @NotBlank(message = "Type is required!")
    @Pattern(regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$", message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\"")
    @Column(name = "PetType")
    private String petType;

    @NotBlank(message = "Breed is required!")
    @Pattern(regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$", message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\"")
    @Column(name = "Breed", nullable = false)
    private String breed;

    @NotBlank(message = "Gender is required!")
    @Pattern(regexp = "^(Male|Female)$", message = "Gender must be Male or Female.")
    @Column(name = "Gender", nullable = false)
    private String gender;

    @NotNull(message = "Age is required!")
    @Min(value = 1, message = "Age must be at least 1 month")
    @Max(value = 50, message = "Age cannot exceed 50 months")
    @Column(name = "AgeMonths")
    private Integer ageMonths;

    @NotNull(message = "Weight is required!")
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1 kg.")
    @DecimalMax(value = "100.0", message = "Weight must not exceed 100 kg.")
    @Column(name = "WeightKg")
    private Double weightKg;

    @NotBlank(message = "Color is required!")
    @Pattern(regexp = "^\\p{Lu}\\p{L}*( \\p{L}+)*$", message = "Regex description: \"Starts with an uppercase letter, followed by letters and single spaces only.\"")
    @Column(name = "Color")
    private String color;

    @Column(columnDefinition = "TEXT", name = "Note")
    private String note;

    @Column(name = "ImageUrl")
    private String imageUrl;

    @Column(columnDefinition = "TEXT", name = "Description")
    private String description;

    // Validate price ở DTO/controller cho flow shop, không bắt buộc ở entity
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


    @ManyToOne
    @JoinColumn(name = "OwnerCustomerID")
    @JsonIgnore
    private Customer owner;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private List<CartItem> cartsList = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PetVaccinations> vaccinations = new ArrayList<>();

    @OneToMany(mappedBy = "pet", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @ToString.Exclude
    private List<OrderItems> orderItems;

    @Transient
    private Boolean isVaccinated;

    @Transient
    private Integer vaccinationStaffID;

    @Transient
    private String vaccineName;

    @Transient
    private String vaccineNote;

    @Transient
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @FutureOrPresent(message = "Next due date cannot be in the past!")
    private LocalDate nextDueDate;


    public Integer getId() {
        return this.petID;
    }

    public void setId(Integer id) {
        this.petID = id;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.isAvailable == null) this.isAvailable = true;
        if (this.discountPercent == null) this.discountPercent = 0;

        calculateSalePrice();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        calculateSalePrice();
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

    private void calculateSalePrice() {
        if (this.price != null) {
            if (this.discountPercent != null && this.discountPercent > 0) {
                BigDecimal discount = new BigDecimal(this.discountPercent);

                BigDecimal discountAmount = this.price.multiply(discount).divide(new BigDecimal("100"));
                this.salePrice = this.price.subtract(discountAmount);
            } else {
                this.salePrice = this.price;
            }
        }
    }

}



