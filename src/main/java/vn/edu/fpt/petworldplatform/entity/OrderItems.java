package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "OrderItems", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderItemID")
    private Integer orderItemID;

    // Khóa ngoại kết nối với Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false) //góc nhìn db: cột fk orderId trỏ sang talbe Order
    private Order order;                            //góc nhìn oop: biến này link sang class Order

    @Column(name = "ProductID")
    private Integer productID;

    @Column(name = "PetID")
    private Integer petID;

    @Column(name = "ItemName", nullable = false, length = 150)
    private String itemName;

    @Column(name = "UnitPrice", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity = 1;

    // LineTotal là cột AS (UnitPrice * Quantity) PERSISTED trong SQL
    // Do đó, trong Java ta để insertable = false và updatable = false
    @Column(name = "LineTotal", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal lineTotal;

    public void setProduct(Product product) {
    }

    public void setPet(Pets pet) {

    }
}
