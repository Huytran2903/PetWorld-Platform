package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payments", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    private Integer paymentID;

    //order hoặc service
    @Column(name = "PaymentType", nullable = false, length = 10)
    private String paymentType;

    // Ràng buộc khóa ngoại đến bảng Orders
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID")
    private Order order;

    // Ràng buộc khóa ngoại đến bảng Appointments
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AppointmentID")
    private Appointment appointment;

    // Phương thức: cod, bank, momo, vnpay, paypal, cash, other
    @Column(name = "Method", nullable = false, length = 30)
    private String method;

    @Column(name = "Amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Mặc định là 'pending'
    @Column(name = "Status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "PaidAt")
    private LocalDateTime paidAt;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "pending";
        }
    }
}