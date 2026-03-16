package vn.edu.fpt.petworldplatform.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    /**
     * New evidence photos uploaded by manager when creating summary.
     */
    private List<MultipartFile> photos;

    /**
     * Existing evidence photos (from service notes) that manager wants to attach to summary.
     * Values are imageUrl strings.
     */
    private List<String> selectedExistingPhotos;
}

