package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackID")
    private Integer id;

    @Column(name = "FeedbackType")
    private String type; // product, service, general

    @ManyToOne
    @JoinColumn(name = "CustomerID")
    private Customer customer;

    @Column(name = "OrderItemID")
    private Integer orderItemId;

    @Column(name = "AppointmentID")
    private Integer appointmentId;

    @Column(name = "ServiceID")
    private Integer serviceId;

    @ManyToOne
    @JoinColumn(name = "StaffID")
    private Staff staff;

    @Column(name = "Rating")
    private Integer rating;

    @Column(name = "Subject", length = 100)
    private String subject;

    @Column(name = "Comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "Email", length = 255)
    private String email;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "ImageUrls", columnDefinition = "VARCHAR(MAX)")
    private String imageUrls;

    @Column(name = "Status")
    private String status; // pending, approved, rejected

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    // Nếu feedback về service
    @Column(name = "ServiceName")
    private String serviceName; // Tạm thời lưu tên, map chi tiết sau nếu cần

    @Column(name = "ReplyMessage", columnDefinition = "NVARCHAR(MAX)")
    private String replyMessage;

    @Column(name = "RepliedAt")
    private LocalDateTime repliedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}