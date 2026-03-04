package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PetVaccinations") // Tên bảng trong DB
@Data // Tự động tạo Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // Tạo constructor không tham số
@AllArgsConstructor // Tạo constructor đầy đủ tham số
public class PetVaccinations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VaccinationID")
    private Long vaccinationId;

    @Column(name = "PetID")
    private Long petId;

    @Column(name = "VaccineName", length = 255)
    private String vaccineName;

    @Column(name = "AdministeredDate")
    private LocalDate administeredDate;

    @Column(name = "NextDueDate")
    private LocalDate nextDueDate;

    @Column(name = "AppointmentID")
    private Long appointmentId;

    @Column(name = "PerformedByStaffID")
    private Long performedByStaffId;

    @Column(name = "Note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
