package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.AppointmentService;

import java.util.List;

@Controller
@RequestMapping("/admin/appointments")
@RequiredArgsConstructor
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    /** UC-32: Manage Appointments - List all */
    @GetMapping
    public String listAppointments(Model model) {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        model.addAttribute("appointments", appointments);
        return "admin/appt-manage";
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
    public String assignStaff(@PathVariable Integer id, @RequestParam Long staffId, RedirectAttributes redirectAttributes) {
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
    public String reassignStaff(@PathVariable Integer id, @RequestParam Long staffId, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.reassignStaff(id, staffId);
            redirectAttributes.addFlashAttribute("message", "Staff reassigned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/appointments/" + id;
    }
}
