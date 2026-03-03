package vn.edu.fpt.petworldplatform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessControlDTO {

    private String permissionCode;

    private Boolean isAllowed;

}