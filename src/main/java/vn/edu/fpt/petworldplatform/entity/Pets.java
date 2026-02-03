package vn.edu.fpt.petworldplatform.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
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
    private Long petID; // ID là Long

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

    // Dùng BigDecimal theo thiết kế của nhóm
    @Column(precision = 18, scale = 2, name = "Price")
    private BigDecimal price;

    @Column(precision = 18, scale = 2,name = "SalePrice")
    private BigDecimal salePrice;

    @Column(name = "DiscountPercent")
    private Integer discountPercent;

    @Column(name = "IsAvailable")
    private Boolean isAvailable;

    @ManyToOne
    @JoinColumn(name = "OwnerCustomerID")
    private Customer owner;

    @Column(name = "PurchasedAt")
    private LocalDateTime purchasedAt;

    @Column(updatable = false,name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return this.petID;
    }

    public void setId(Long id) {
        this.petID = id;
    }
}