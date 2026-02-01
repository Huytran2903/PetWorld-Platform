package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "ServiceTypes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ServiceTypeID")
    private Integer id;

    @NotBlank(message = "Name is required")
    @Column(name = "Name", nullable = false, unique = true, length = 80)
    private String name;

    @Column(name = "Description", length = 500)
    private String description;

    @Column(name = "IsActive", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
