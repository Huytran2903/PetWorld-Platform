package vn.edu.fpt.petworldplatform.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDisplayDTO {
    private Integer staffId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String roleName;
    private Boolean isActive;

    private long pendingVaccinesCount;
    private long pendingAppointmentsCount;
    private long inProgressAppointmentsCount;
}