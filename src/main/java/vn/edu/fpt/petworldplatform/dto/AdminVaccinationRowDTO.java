package vn.edu.fpt.petworldplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminVaccinationRowDTO {
    /** Khóa bản ghi PetVaccinations (bản mới nhất theo pet + vaccine) — dùng cho nút Sửa */
    private Integer vaccinationId;
    private Integer petId;
    private String petName;
    private String ownerName;
    private String vaccineName;
    private LocalDate administeredDate;
    private LocalDate nextDueDate;
    private String performedByName;
}

