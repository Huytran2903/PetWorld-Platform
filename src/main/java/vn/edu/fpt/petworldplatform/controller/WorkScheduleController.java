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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.IWorkScheduleService;
import vn.edu.fpt.petworldplatform.service.StaffService;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Controller
@RequestMapping("/staff/work-schedule")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final IWorkScheduleService workScheduleService;
    private final StaffService staffService;

    @GetMapping
    public String viewWorkSchedule(@RequestParam(required = false) LocalDate startDate,
                                   @RequestParam(required = false) LocalDate endDate,
                                   Principal principal,
                                   HttpSession session,
                                   Model model) {
        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = startDate != null
                ? startDate
                : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = endDate != null
                ? endDate
                : weekStart.plusDays(6);
        if (weekEnd.isBefore(weekStart)) {
            LocalDate tmp = weekStart;
            weekStart = weekEnd;
            weekEnd = tmp;
        }

        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate currentWeekEnd = currentWeekStart.plusDays(6);
        boolean isCurrentWeek = weekStart.equals(currentWeekStart) && weekEnd.equals(currentWeekEnd);
        boolean isPrevWeek = weekStart.equals(currentWeekStart.minusWeeks(1)) && weekEnd.equals(currentWeekEnd.minusWeeks(1));
        boolean isNextWeek = weekStart.equals(currentWeekStart.plusWeeks(1)) && weekEnd.equals(currentWeekEnd.plusWeeks(1));

        try {
            model.addAttribute("activePage", "work-schedule");
            model.addAttribute("schedules", workScheduleService.getStaffSchedule(staff.getStaffId(), weekStart, weekEnd));
            model.addAttribute("startDate", weekStart);
            model.addAttribute("endDate", weekEnd);
            model.addAttribute("nextWeekStart", weekStart.plusWeeks(1));
            model.addAttribute("nextWeekEnd", weekEnd.plusWeeks(1));
            model.addAttribute("prevWeekStart", weekStart.minusWeeks(1));
            model.addAttribute("prevWeekEnd", weekEnd.minusWeeks(1));
            model.addAttribute("isCurrentWeek", isCurrentWeek);
            model.addAttribute("isPrevWeek", isPrevWeek);
            model.addAttribute("isNextWeek", isNextWeek);
            return "staff/work_schedule";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("schedules", java.util.List.of());
            model.addAttribute("startDate", weekStart);
            model.addAttribute("endDate", weekEnd);
            model.addAttribute("nextWeekStart", weekStart.plusWeeks(1));
            model.addAttribute("nextWeekEnd", weekEnd.plusWeeks(1));
            model.addAttribute("prevWeekStart", weekStart.minusWeeks(1));
            model.addAttribute("prevWeekEnd", weekEnd.minusWeeks(1));
            model.addAttribute("isCurrentWeek", isCurrentWeek);
            model.addAttribute("isPrevWeek", isPrevWeek);
            model.addAttribute("isNextWeek", isNextWeek);
            return "staff/work_schedule";
        }
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
