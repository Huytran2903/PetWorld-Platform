package vn.edu.fpt.petworldplatform.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import vn.edu.fpt.petworldplatform.dto.PetFormDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Pets")
public class Pets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PetID")
    private Long petID;

    @Column(nullable = false, name = "Name")
    private String name;

    @Column(name = "PetType")
    private String petType;

    @Column(name = "Breed")
    private String breed;

    @Column(name = "Gender")
    private String gender;

    @Column(name = "AgeMonths")
    private Integer ageMonths;

    @Column(name = "WeightKg")
    private Double weightKg;

    @Column(name = "Color")
    private String color;

    @Column(columnDefinition = "TEXT", name = "Note")
    private String note;

    @Column(name = "ImageUrl")
    private String imageUrl;

    @Column(columnDefinition = "TEXT", name = "Description")
    private String description;

    @Column(precision = 18, scale = 2, name = "Price")
    private BigDecimal price;

    @Column(precision = 18, scale = 2, name = "SalePrice")
    private BigDecimal salePrice;

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

    public Long getId() {
        return this.petID;
    }

    public void setId(Long id) {
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



