package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AccessControl")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(AccessControlId.class)
public class AccessControl {


    @Id
    @Column(name = "RoleID", nullable = false)
    private Integer roleId;

    @Id
    @Column(name = "PermissionCode", nullable = false, length = 100)
    private String permissionCode;



    @Column(name = "IsAllowed", nullable = false)
    private Boolean isAllowed;

    @Column(name = "GrantedAt")
    private LocalDateTime grantedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleID", insertable = false, updatable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Role role;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.grantedAt = LocalDateTime.now();
    }
}