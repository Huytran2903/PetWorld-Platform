package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.PetHealthPhoto;
import vn.edu.fpt.petworldplatform.entity.PetHealthRecord;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.PetHealthPhotoRepository;
import vn.edu.fpt.petworldplatform.repository.PetHealthRecordRepository;
import vn.edu.fpt.petworldplatform.service.IAssignedAppointmentService;
import vn.edu.fpt.petworldplatform.service.StaffService;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final IAssignedAppointmentService assignedAppointmentService;
    private final StaffService staffService;
    private final PetHealthRecordRepository petHealthRecordRepository;
    private final PetHealthPhotoRepository petHealthPhotoRepository;

    @GetMapping("/assigned_list")
    public String viewAssignedList(
            Principal principal,
            HttpSession session,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            Model model) {

        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        session.setAttribute("currentStaffId", staff.getStaffId());

        model.addAttribute("appointments",
                assignedAppointmentService.getAssignedAppointments(staff.getStaffId(), date, status));
        model.addAttribute("date", date);
        model.addAttribute("status", status);
        model.addAttribute("staffName", staff.getFullName());
        model.addAttribute("currentStaffId", staff.getStaffId());

        return "staff/assigned_list";
    }

    @GetMapping("/appointment_detail")
    public String viewAppointmentDetail(@RequestParam Integer id,
                                        Principal principal,
                                        HttpSession session,
                                        Model model) {
        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        session.setAttribute("currentStaffId", staff.getStaffId());

        Appointment appointment = assignedAppointmentService.getAppointmentDetail(staff.getStaffId(), id);
        model.addAttribute("appointment", appointment);
        model.addAttribute("currentStaffId", staff.getStaffId());

        List<AppointmentServiceLine> myServiceLines = appointment.getServiceLines() == null
                ? List.of()
                : appointment.getServiceLines().stream()
                .filter(line -> line.getAssignedStaffId() != null && line.getAssignedStaffId().equals(staff.getStaffId()))
                .toList();
        model.addAttribute("myServiceLines", myServiceLines);

        Map<Integer, PetHealthRecord> healthRecordByLineId = new HashMap<>();
        Map<Integer, List<PetHealthPhoto>> healthPhotosByLineId = new HashMap<>();

        for (AppointmentServiceLine line : myServiceLines) {
            if (line.getId() == null) {
                continue;
            }
            petHealthRecordRepository.findByAppointmentServiceLine_Id(line.getId()).ifPresent(record -> {
                healthRecordByLineId.put(line.getId(), record);
                healthPhotosByLineId.put(line.getId(), petHealthPhotoRepository.findByRecord_Id(record.getId()));
            });
        }

        model.addAttribute("healthRecordByLineId", healthRecordByLineId);
        model.addAttribute("healthPhotosByLineId", healthPhotosByLineId);

        return "staff/appointment_detail";
    }

    @PostMapping("/appointment/checkin")
    public String checkIn(@RequestParam Integer id,
                          Principal principal,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        try {
            assignedAppointmentService.checkIn(staff.getStaffId(), id);
            redirectAttributes.addFlashAttribute("message", "Checked in successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/staff/appointment_detail?id=" + id;
    }

    @PostMapping("/appointment/no-show")
    public String reportNoShow(@RequestParam Integer id,
                               Principal principal,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        try {
            assignedAppointmentService.reportNoShow(staff.getStaffId(), id);
            redirectAttributes.addFlashAttribute("message", "Appointment marked as No Show.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/staff/appointment_detail?id=" + id;
    }

    @GetMapping("/booking")
    public String staffBooking() {
        return "redirect:/booking";
    }

    private Staff resolveCurrentStaff(Principal principal, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
            Object principalObj = authToken.getPrincipal();
            if (principalObj instanceof Staff staff) {
                return staff;
            }
            if (principalObj instanceof UserDetails userDetails) {
                Staff byUsername = staffService.findByUsername(userDetails.getUsername()).orElse(null);
                if (byUsername != null) {
                    return byUsername;
                }
            }
        }

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            String email = oauth2Token.getPrincipal().getAttribute("email");
            if (email != null && !email.isBlank()) {
                Optional<Staff> byEmail = staffService.findByEmail(email);
                if (byEmail.isPresent()) {
                    return byEmail.get();
                }
            }
        }

        if (principal != null) {
            Staff byPrincipal = staffService.findByUsername(principal.getName()).orElse(null);
            if (byPrincipal != null) {
                return byPrincipal;
            }
        }

        Object loggedInStaff = session.getAttribute("loggedInStaff");
        if (loggedInStaff instanceof Staff staff) {
            return staff;
        }

        Object currentStaffId = session.getAttribute("currentStaffId");
        if (currentStaffId instanceof Long idFromSession) {
            return staffService.findById(idFromSession).orElse(null);
        }
        if (currentStaffId instanceof Integer idFromSessionInt) {
            return staffService.findById(idFromSessionInt.longValue()).orElse(null);
        }

        return null;
    }
}
