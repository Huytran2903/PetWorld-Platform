package vn.edu.fpt.petworldplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentFilterRequest {
    /** inclusive */
    private LocalDate fromDate;
    /** inclusive */
    private LocalDate toDate;

    /** pending/confirmed/in_progress/done/canceled/no_show */
    private String status;

    /** Search by appointment code OR customer name OR customer phone (if joined) */
    private String keyword;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;
}
