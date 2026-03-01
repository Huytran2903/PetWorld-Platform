package vn.edu.fpt.petworldplatform.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SaveHealthReportDraftRequest {
    private Double weightKg;
    private Double temperature;
    private String conditionBefore;
    private String conditionAfter;
    private String findings;
    private String conditionNotes;
    private Boolean warningFlag;
    private String recommendations;
    private List<MultipartFile> photos = new ArrayList<>();
}
