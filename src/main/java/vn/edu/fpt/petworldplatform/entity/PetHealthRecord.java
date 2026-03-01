package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PetHealthRecords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetHealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HealthRecordID")
    private Integer id;

    @Column(name = "PetID", insertable = false, updatable = false)
    private Long petId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PetID", nullable = false)
    private Pets pet;

    @Column(name = "AppointmentID", insertable = false, updatable = false)
    private Integer appointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AppointmentID")
    private Appointment appointment;

    @Column(name = "PerformedByStaffID", insertable = false, updatable = false)
    private Long performedByStaffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PerformedByStaffID")
    private Staff performedByStaff;

    @Column(name = "CheckDate", nullable = false)
    private LocalDateTime checkDate;

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
    @Builder.Default
    private Boolean warningFlag = false;

    @Column(name = "IsDraft")
    @Builder.Default
    private Boolean isDraft = false;

    @Column(name = "IsDeleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PetHealthPhoto> photos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (checkDate == null) {
            checkDate = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (warningFlag == null) {
            warningFlag = false;
        }
        if (isDraft == null) {
            isDraft = false;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
