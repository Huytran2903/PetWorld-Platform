package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.AppointmentService;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import vn.edu.fpt.petworldplatform.dto.AppointmentFilterRequest;

// ... other imports ...

@Controller
@RequestMapping("/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    @ModelAttribute("filter")
    public AppointmentFilterRequest appointmentFilterRequest() {
        return new AppointmentFilterRequest();
    }

    /** UC-32: Manage Appointments - List with filter and pagination */
    @GetMapping
    public String listAppointments(@ModelAttribute("filter") AppointmentFilterRequest filter, Model model) {
        Page<Appointment> appointmentPage = appointmentService.getAppointments(filter);
        model.addAttribute("appointments", appointmentPage.getContent());
        model.addAttribute("currentPage", filter.getPage());
        model.addAttribute("totalPages", appointmentPage.getTotalPages());
        model.addAttribute("totalItems", appointmentPage.getTotalElements());
        return "admin/appt-manage";
    }

    /** UC-32: Cancel Appointment */
    @PostMapping("/{id}/cancel")
    public String cancelAppointment(@PathVariable Integer id, @RequestParam String reason, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.cancelAppointment(id, reason);
            redirectAttributes.addFlashAttribute("message", "Appointment canceled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/appointments/" + id;
    }

    /** UC-32: Delete Canceled Appointment */
    @PostMapping("/{id}/delete")
    public String deleteAppointment(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.deleteAppointment(id);
            redirectAttributes.addFlashAttribute("message", "Appointment deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/appointments";
    }

    /** UC-32: Export to Excel */
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportToExcel(@ModelAttribute AppointmentFilterRequest filter) {
        ByteArrayInputStream in = appointmentService.exportToExcel(filter);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=appointments.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    /** UC-33: View Appointment Detail */
    @GetMapping("/{id}")
    public String viewDetail(@PathVariable Integer id, Model model) {
        Appointment appointment = appointmentService.getAppointmentById(id);
        List<Staff> availableStaff = appointmentService.getAvailableStaffForAppointment(id);
        
        model.addAttribute("appointment", appointment);
        model.addAttribute("availableStaff", availableStaff);
        return "admin/appt-detail";
    }

    /** UC-33: Assign Staff */
    @PostMapping("/{id}/assign")
    public String assignStaff(@PathVariable Integer id, @RequestParam Integer staffId, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.assignStaffToAppointment(id, staffId);
            redirectAttributes.addFlashAttribute("message", "Staff assigned successfully and appointment confirmed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/appointments/" + id;
    }

    /** UC-33: Re-assign Staff */
    @PostMapping("/{id}/reassign")
    public String reassignStaff(@PathVariable Integer id, @RequestParam Integer staffId, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.reassignStaff(id, staffId);
            redirectAttributes.addFlashAttribute("message", "Staff reassigned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/appointments/" + id;
    }
}
