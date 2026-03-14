package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "AppointmentSummaryPhotos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentSummaryPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AppointmentSummaryPhotoID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SummaryID", nullable = false)
    private AppointmentSummary summary;

    @Column(name = "ImageUrl", length = 500, nullable = false)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;
}

