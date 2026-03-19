package vn.edu.fpt.petworldplatform.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ServiceNoteRequest {
    private String note;
    private List<MultipartFile> photos = new ArrayList<>();

    // Vaccine record fields (only used when serviceType is vaccine/vaccination)
    private String vaccineName;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueDate;

    private String vaccineNote;
}
