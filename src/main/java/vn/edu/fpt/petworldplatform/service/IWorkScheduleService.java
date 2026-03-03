package vn.edu.fpt.petworldplatform.service;

import vn.edu.fpt.petworldplatform.entity.StaffSchedule;

import java.time.LocalDate;
import java.util.List;

public interface IWorkScheduleService {
    List<StaffSchedule> getStaffSchedule(Integer staffId, LocalDate startDate, LocalDate endDate);

    boolean isStaffActive(Integer staffId);
}
