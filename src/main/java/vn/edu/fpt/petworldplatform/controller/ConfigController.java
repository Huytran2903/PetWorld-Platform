package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.ConfigDTO;
import vn.edu.fpt.petworldplatform.entity.SystemConfigs;
import vn.edu.fpt.petworldplatform.service.ConfigService;

import java.util.List;

@Controller
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping("/admin/config")
    public String showConfigForm(Model model) {
        List<SystemConfigs> configsList = configService.getAllConfigs();

        ConfigDTO dto = new ConfigDTO();
        dto.setConfigs(configsList);

        model.addAttribute("configDTO", dto);
        return "admin/config-form";
    }

    @PostMapping("/admin/config/update")
    public String updateConfigs(@ModelAttribute("configDTO") ConfigDTO dto) {
        configService.updateConfigs(dto.getConfigs());

        return "redirect:/admin/config?success";
    }

    @GetMapping("/admin/config/add")
    public String showAddConfigForm(Model model) {
        model.addAttribute("newConfig", new SystemConfigs());
        return "admin/config-add";
    }

    @PostMapping("/admin/config/add")
    public String addNewConfig(@ModelAttribute("newConfig") SystemConfigs newConfig,
                               RedirectAttributes redirectAttrs) {
        try {
            configService.saveSingleConfig(newConfig);

            redirectAttrs.addFlashAttribute("success", "New configuration added successfully!");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error: This Config Key may already exist or is invalid!");
        }
        return "redirect:/admin/config/add";
    }
}
