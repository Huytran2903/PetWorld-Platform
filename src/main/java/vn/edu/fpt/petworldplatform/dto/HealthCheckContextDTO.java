package vn.edu.fpt.petworldplatform.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HealthCheckContextDTO {
    private Integer appointmentId;
    private Integer serviceLineId;
    private String serviceName;
    private String appointmentCode;
    private Integer petId;
    private String petName;
    private String status;
    private String customerName;
    private String staffName;
    private Boolean manager;
}
