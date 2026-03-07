package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "CartItems", schema = "dbo")
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartItemID")
    private Integer cartItemId;

    // Khóa ngoại
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CartID", nullable = false)
    private Carts cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ProductID")
    private Product product;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PetID")
    private Pets pet;

    @Column(name = "Quantity", nullable = false)
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity = 1;

    @Column(name = "AddedAt", nullable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        if (this.addedAt == null) {
            this.addedAt = LocalDateTime.now();
        }
        validateType();
    }

    // Logic kiểm tra ràng buộc CK_CartItems_OnlyOneType
    private void validateType() {
        if ((product == null && pet == null) || (product != null && pet != null)) {
            throw new IllegalStateException("Phải có chính xác một ProductID hoặc một PetID.");
        }
    }
}