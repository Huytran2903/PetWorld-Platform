package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PetVaccinations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetVaccinations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VaccinationID")
    private Integer vaccinationId;

    @Column(name = "VaccineName", nullable = false, length = 100)
    private String vaccineName;

    @Column(name = "AdministeredDate", nullable = false)
    private LocalDate administeredDate;

    @Column(name = "NextDueDate")
    @FutureOrPresent(message = "Next due date cannot be in the past!")
    private LocalDate nextDueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PetID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Pets pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PerformedByStaffID")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Staff performedByStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AppointmentID")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Appointment appointment;

    @Column(name = "Note", length = 500)
    private String note;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.administeredDate == null) {
            this.administeredDate = LocalDate.now();
        }
    }
}