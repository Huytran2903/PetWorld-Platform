package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "AccessControl")
@IdClass(AccessControlId.class)
public class AccessControl {

    @Id
    @Column(name = "RoleID")
    private Integer roleId;

    @Id
    @Column(name = "PermissionCode")
    private String permissionCode;

    @Column(name = "IsAllowed")
    private Boolean isAllowed;

    @Column(name = "GrantedAt")
    @CreationTimestamp
    private LocalDateTime grantedAt;
}
