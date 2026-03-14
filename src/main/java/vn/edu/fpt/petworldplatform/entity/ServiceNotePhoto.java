package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ServiceNotePhotos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceNotePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ServiceNotePhotoID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ServiceNoteID", nullable = false)
    private ServiceNote serviceNote;

    @Column(name = "ImageUrl", length = 500, nullable = false)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;
}
