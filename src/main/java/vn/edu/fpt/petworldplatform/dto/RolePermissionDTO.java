package vn.edu.fpt.petworldplatform.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionDTO {

    private Integer roleId;

    private String roleName;

    private List<AccessControlDTO> permissions;

}