package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ServiceID")
    private Integer id;

    @NotBlank(message = "Service type is required")
    @Column(name = "ServiceType", nullable = false, length = 20)
    private String serviceType;

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    @Column(name = "Name", nullable = false, length = 120)
    private String name;

    @Size(max = 500)
    @Column(name = "Description", length = 500)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", message = "Price must be >= 0")
    @Column(name = "BasePrice", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "DurationMinutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 30;

    @Column(name = "IsActive", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
