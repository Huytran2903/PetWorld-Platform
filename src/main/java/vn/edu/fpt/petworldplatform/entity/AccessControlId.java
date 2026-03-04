package vn.edu.fpt.petworldplatform.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccessControlId implements Serializable {
    private Integer roleId;
    private String permissionCode;
}
