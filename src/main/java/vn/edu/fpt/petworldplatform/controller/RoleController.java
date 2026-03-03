package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.RolePermissionDTO;
import vn.edu.fpt.petworldplatform.entity.Role;
import vn.edu.fpt.petworldplatform.service.RoleService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping({"/admin/roles", "/admin/roles/"})
    public String listRoles(Model model) {
        List<Role> roles = roleService.getAllRoles();
        model.addAttribute("roles", roles);
        return "admin/role-list";
    }

    @GetMapping("admin/roles/permissions/{id}")
    public String showRolePermissions(@PathVariable("id") Integer roleId, Model model) {
        try {
            RolePermissionDTO rolePermissionDTO = roleService.getRolePermissions(roleId);
            model.addAttribute("rolePermissionDTO", rolePermissionDTO);

            return "admin/role-permissions";

        } catch (Exception e) {
            return "redirect:/admin/roles?error=RoleNotFound";
        }
    }


    @PostMapping("admin/roles/permissions/update")
    public String updateRolePermissions(
            @ModelAttribute("rolePermissionDTO") RolePermissionDTO dto,
            RedirectAttributes redirectAttributes) {

        try {
            roleService.updatePermissions(dto);

            redirectAttributes.addFlashAttribute("successMessage", "Role permissions updated successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while saving permissions: " + e.getMessage());
        }

        return "redirect:/admin/roles/permissions/" + dto.getRoleId();
    }
}
