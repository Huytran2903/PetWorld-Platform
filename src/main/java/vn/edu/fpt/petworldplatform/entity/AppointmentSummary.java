package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AppointmentSummaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SummaryID")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AppointmentID", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "WeightKg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "Temperature", precision = 4, scale = 2)
    private BigDecimal temperature;

    @Column(name = "ConditionBefore", length = 500)
    private String conditionBefore;

    @Column(name = "ConditionAfter", length = 500)
    private String conditionAfter;

    @Column(name = "Findings", columnDefinition = "NVARCHAR(MAX)")
    private String findings;

    @Column(name = "Recommendations", columnDefinition = "NVARCHAR(MAX)")
    private String recommendations;

    @Column(name = "Note", length = 500)
    private String note;

    @Column(name = "WarningFlag")
    private Boolean warningFlag;

    @Column(name = "SummaryByStaffID", insertable = false, updatable = false)
    private Integer summaryByStaffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SummaryByStaffID")
    private Staff summaryByStaff;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
