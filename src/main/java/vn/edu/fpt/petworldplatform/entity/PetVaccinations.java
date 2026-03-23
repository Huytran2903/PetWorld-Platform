package vn.edu.fpt.petworldplatform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    private LocalDate nextDueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PetID", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
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