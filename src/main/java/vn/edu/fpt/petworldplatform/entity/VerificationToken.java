package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "verification_tokens")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @OneToOne(targetEntity = Customer.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = true)
    private Customer customer;

    @OneToOne(targetEntity = Staff.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = true)
    private Staff staff;

    public VerificationToken(Customer customer) {
        this.customer = customer;
        this.staff = null;
        this.expiryDate = LocalDateTime.now().plusMinutes(10);
        this.token = UUID.randomUUID().toString();
    }

    public VerificationToken(Staff staff) {
        this.staff = staff;
        this.customer = null; // Đảm bảo customer null
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.token = UUID.randomUUID().toString();
    }
}