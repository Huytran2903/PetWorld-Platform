package vn.edu.fpt.petworldplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentSummaryRequest {
    private Double weightKg;
    private Double temperature;
    private String conditionBefore;
    private String conditionAfter;
    private String findings;
    private String recommendations;
    private String note;
    private Boolean warningFlag;
}
