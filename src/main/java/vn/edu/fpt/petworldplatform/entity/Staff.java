package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StaffID")
    private Integer staffId;

    @ManyToOne
    @JoinColumn(name = "RoleID", nullable = false)
    private Role role;

    @Column(name = "Username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "PasswordHash", nullable = false)
    private String passwordHash;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "FullName", columnDefinition = "NVARCHAR(120)")
    private String fullName;

    @Column(name = "HireDate")
    private LocalDate hireDate;

    @Column(name = "Bio")
    private String bio;

    @Column(name = "IsActive")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "performedByStaff", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<PetVaccinations> vaccinations;

    @OneToMany(mappedBy = "performedByStaff", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<PetHealthRecord> healthRecords;

    @OneToMany(mappedBy = "staff", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Feedback> feedbacks;

    @OneToMany(mappedBy = "staff", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<StaffSchedule> schedules;

    @OneToMany(mappedBy = "assignedStaff", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<AppointmentServiceLine> appointmentServiceLines;

    @OneToMany(mappedBy = "staff", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Appointment> appointments;

    public long getPendingVaccinesCount() {
        if (this.vaccinations == null) return 0;
        LocalDate today = LocalDate.now();
        return this.vaccinations.stream()
                .filter(v -> v.getNextDueDate() != null && !v.getNextDueDate().isBefore(today))
                .count();
    }

    // 1. Đếm việc CẦN BÀN GIAO (pending, assigned)
    public long getPendingAppointmentsCount() {
        if (this.appointmentServiceLines == null || this.appointmentServiceLines.isEmpty()) return 0;
        return this.appointmentServiceLines.stream()
                .filter(service -> {
                    String status = service.getServiceStatus();
                    return status != null && (status.equalsIgnoreCase("assigned") || status.equalsIgnoreCase("pending"));
                })
                .count();
    }

    // 2. Đếm việc ĐANG LÀM DỞ (in_progress) - Dùng để chặn xóa
    public long getInProgressAppointmentsCount() {
        if (this.appointmentServiceLines == null || this.appointmentServiceLines.isEmpty()) return 0;
        return this.appointmentServiceLines.stream()
                .filter(service -> "in_progress".equalsIgnoreCase(service.getServiceStatus()))
                .count();
    }

    public long getPendingHealthRecordsCount() {
        return 0;
    }
}
