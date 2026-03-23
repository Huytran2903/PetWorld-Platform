package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.dto.AccessControlDTO;
import vn.edu.fpt.petworldplatform.dto.RolePermissionDTO;
import vn.edu.fpt.petworldplatform.entity.AccessControl;
import vn.edu.fpt.petworldplatform.entity.Role;
import vn.edu.fpt.petworldplatform.repository.AccessControlRepository;
import vn.edu.fpt.petworldplatform.repository.RoleRepo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {
    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private AccessControlRepository accessRepo;

    private final List<String> ALL_PERMISSION_CODES = List.of(
            // 1. Nhóm Quản lý tài khoản
            "MANAGE_STAFF",
            "MANAGE_CUSTOMER",

            // 2. Nhóm Quản lý Cửa hàng & Sản phẩm
            "MANAGE_CATEGORY",
            "MANAGE_PRODUCT",
            "MANAGE_ORDER",

            // 3. Nhóm Quản lý Dịch vụ Spa / Thú y
            "MANAGE_PET",
            "MANAGE_SERVICE",
            "MANAGE_APPOINTMENT",
            "MANAGE_FEEDBACK",

            // 4. Nhóm Báo cáo & Cài đặt chung
            "VIEW_REPORT",
            "SYSTEM_SETTINGS"
    );

    public List<Role> getAllRoles() {
        return roleRepo.findAll();
    }

    public RolePermissionDTO getRolePermissions(Integer roleId) {

        Role role = roleRepo.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));

        List<AccessControl> existingPermissions = accessRepo.findByRoleId(roleId);

        List<AccessControlDTO> permissionDTOs = new ArrayList<>();

        for (String code : ALL_PERMISSION_CODES) {

            boolean isAllowed = false;

            for (AccessControl acc : existingPermissions) {
                if (acc.getPermissionCode().equals(code) && acc.getIsAllowed() != null && acc.getIsAllowed()) {
                    isAllowed = true;
                    break;
                }
            }

            AccessControlDTO dto = new AccessControlDTO(code, isAllowed);
            permissionDTOs.add(dto);
        }

        RolePermissionDTO result = new RolePermissionDTO();
        result.setRoleId(role.getRoleId());
        result.setRoleName(role.getRoleName());
        result.setPermissions(permissionDTOs);

        return result;
    }

    @Transactional
    public void updatePermissions(RolePermissionDTO dto) {

        accessRepo.deleteByRoleId(dto.getRoleId());

        List<AccessControl> newAccessControls = new ArrayList<>();

        for (AccessControlDTO pDto : dto.getPermissions()) {

            if (pDto.getIsAllowed() != null && pDto.getIsAllowed()) {

                AccessControl entity = new AccessControl();
                entity.setRoleId(dto.getRoleId());
                entity.setPermissionCode(pDto.getPermissionCode());
                entity.setIsAllowed(true);

                newAccessControls.add(entity);
            }
        }

        accessRepo.saveAll(newAccessControls);
    }

}

