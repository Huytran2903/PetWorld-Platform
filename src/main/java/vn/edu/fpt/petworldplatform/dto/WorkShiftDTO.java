package vn.edu.fpt.petworldplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkShiftDTO {
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String note;
    private String displayStatus;
}

