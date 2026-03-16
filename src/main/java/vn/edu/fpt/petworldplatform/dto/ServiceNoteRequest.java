package vn.edu.fpt.petworldplatform.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ServiceNoteRequest {
    private String note;
    private List<MultipartFile> photos = new ArrayList<>();
}
