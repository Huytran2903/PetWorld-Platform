package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PetHealthPhotos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetHealthPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PhotoID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HealthRecordID", nullable = false)
    private PetHealthRecord record;

    @Column(name = "ImageUrl", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "CapturedAt", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (capturedAt == null) {
            capturedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
