package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffController {

    // --- SỬA Ở ĐÂY ---
    // Đổi "/assignments" thành "/assigned_list" để khớp với link bạn đang gõ
    @GetMapping("/assigned_list")
    public String viewAssignedList() {
        return "staff/assigned_list"; // Tên file HTML trong thư mục templates
    }

    // Đổi "/detail" thành "/appointment_detail" nếu bạn cũng muốn link chi tiết giống tên file
    @GetMapping("/appointment_detail")
    public String viewAppointmentDetail() {
        return "staff/appointment_detail";
    }

    // Trong file StaffController.java
    @GetMapping("/booking")
    public String staffBooking() {
        return "redirect:/booking"; // Chuyển hướng nhân viên sang trang đặt lịch chung
    }
}