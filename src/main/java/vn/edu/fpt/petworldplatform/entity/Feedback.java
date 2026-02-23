package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "Feedbacks")
@Data
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackID")
    private Integer id;

    @Column(name = "FeedbackType")
    private String type; // service, product

    @Column(name = "Rating")
    private Integer rating;

    @Column(name = "Comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "Status")
    private String status; // pending, approved, rejected

    @ManyToOne
    @JoinColumn(name = "CustomerID")
    private Customer customer;

    // Nếu feedback về service
    @Column(name = "ServiceName")
    private String serviceName; // Tạm thời lưu tên, map chi tiết sau nếu cần
}