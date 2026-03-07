package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productId;

    @NotNull(message = "Product category is required!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID")
    private Categories category;

    @NotBlank(message = "Product name cannot be blank!")
    @Size(min = 2, max = 150, message = "Product name must be between 2 and 150 characters.")
    @Pattern(
            regexp = "^[\\p{L}][\\p{L}0-9\\s\\-_.,&()]*$",
            message = "Product name must start with a letter and can contain numbers afterwards."
    )
    @Column(name = "Name", nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String name;

    @Pattern(regexp = "^([A-Z0-9]+(-[A-Z0-9]+)*)?$", message = "SKU must contain only uppercase letters, numbers, and hyphens (e.g., SP-001).")
    @Column(name = "SKU", length = 50, unique = true)
    private String sku;

    @NotNull(message = "Price is required!")
    @DecimalMin(value = "0.0", inclusive = true, message = "Product price must be greater than or equal to 0.")
    @Column(name = "Price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Sale price cannot be negative.")
    @Column(name = "SalePrice", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount cannot be less than 0.")
    @DecimalMax(value = "100.0", inclusive = true, message = "Discount cannot exceed 100.")
    @Column(name = "DiscountPercent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @NotNull(message = "Stock quantity is required!")
    @Min(value = 0, message = "Stock quantity cannot be negative.")
    @Column(name = "Stock", nullable = false)
    private Integer stock;

    @Column(name = "ImageUrl", length = 255)
    private String imageUrl;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @NotNull(message = "Active status is required!")
    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<CartItem> cartItems = new ArrayList<>();

    // Tự động gán thời gian khi tạo mới hoặc cập nhật
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (stock == null) stock = 0;
        if (discountPercent == null) discountPercent = BigDecimal.ZERO;

        // Gọi hàm tự động tính giá bán
        calculateSalePrice();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Gọi hàm tự động tính giá bán lại (lỡ Admin đổi % giảm giá)
        calculateSalePrice();
    }

    // --- HÀM NGHIỆP VỤ: TỰ ĐỘNG TÍNH TOÁN GIÁ SAU KHUYẾN MÃI ---
    private void calculateSalePrice() {
        if (this.price != null) {
            // Nếu có giảm giá (> 0)
            if (this.discountPercent != null && this.discountPercent.compareTo(BigDecimal.ZERO) > 0) {
                // Công thức: SalePrice = Price - (Price * DiscountPercent / 100)
                BigDecimal discountAmount = this.price.multiply(this.discountPercent).divide(new BigDecimal("100"));
                this.salePrice = this.price.subtract(discountAmount);
            } else {
                // Nếu không giảm giá (DiscountPercent = 0 hoặc null) thì Giá bán = Giá gốc
                this.salePrice = this.price;
            }
        }
    }
}