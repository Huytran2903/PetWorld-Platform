package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Products") // Tên bảng trong DB của bạn
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productId;

    // Quan hệ ManyToOne với Categories
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID") // Tên cột khóa ngoại trong DB (như trong ảnh)
    private Categories category;

    @Column(name = "Name")
    private String name;

    @Column(name = "SKU")
    private String sku;

    @Column(name = "Price")
    private BigDecimal price;

    @Column(name = "SalePrice")
    private BigDecimal salePrice;

    @Column(name = "DiscountPercent")
    private Integer discountPercent;

    @Column(name = "Stock")
    private Integer stock;

    @Column(name = "ImageUrl")
    private String imageUrl;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "IsActive")
    private Boolean isActive;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    // Tự động gán thời gian khi tạo mới hoặc cập nhật
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isActive = true; // Mặc định là active khi tạo mới
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}