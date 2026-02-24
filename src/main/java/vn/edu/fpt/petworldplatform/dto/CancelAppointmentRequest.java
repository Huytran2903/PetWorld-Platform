package vn.edu.fpt.petworldplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelAppointmentRequest {
    private Integer id;
    private String reason;
}
