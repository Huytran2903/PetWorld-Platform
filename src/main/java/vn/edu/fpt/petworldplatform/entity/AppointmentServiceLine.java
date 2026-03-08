package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "AppointmentServices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentServiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AppointmentServiceID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AppointmentID", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ServiceID", nullable = false)
    private ServiceItem service;

    @Column(name = "AssignedStaffID", insertable = false, updatable = false)
    private Integer assignedStaffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AssignedStaffID")
    private Staff assignedStaff;

    @Column(name = "Price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "Quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "ServiceStatus", nullable = false, length = 20)
    @Builder.Default
    private String serviceStatus = "pending";
}
