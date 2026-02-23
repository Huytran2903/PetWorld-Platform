package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AppointmentID")
    private Integer id;

    @Column(name = "AppointmentCode", nullable = false, unique = true, length = 30)
    private String appointmentCode;

    @Column(name = "CustomerID", nullable = false)
    private Long customerId;

    @Column(name = "PetID", nullable = false)
    private Long petId;

    @Column(name = "StaffID")
    private Long staffId;

    @Column(name = "AppointmentDate", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(name = "Note", length = 255)
    private String note;

    @Column(name = "Status", nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "CanceledAt")
    private LocalDateTime canceledAt;

    @Column(name = "CancellationReason", length = 255)
    private String cancellationReason;

    @Column(name = "RescheduledAt")
    private LocalDateTime rescheduledAt;

    @Column(name = "PreviousAppointmentDate")
    private LocalDateTime previousAppointmentDate;

    @Column(name = "EndTime")
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AppointmentServiceLine> serviceLines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
