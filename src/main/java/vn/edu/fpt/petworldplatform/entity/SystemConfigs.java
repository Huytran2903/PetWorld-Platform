package vn.edu.fpt.petworldplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_configs")
@Getter
@Setter
public class SystemConfigs {

    @Id
    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_value")
    private String configValue;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "updated_by_staff_id")
    private Integer updatedByStaffId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preSave() {
        this.updatedAt = LocalDateTime.now();
        if (this.updatedByStaffId == null) {
            this.updatedByStaffId = 1;
        }
    }
}