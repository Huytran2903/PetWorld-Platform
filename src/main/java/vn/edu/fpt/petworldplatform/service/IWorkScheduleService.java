package vn.edu.fpt.petworldplatform.service;

import vn.edu.fpt.petworldplatform.dto.WorkShiftDTO;

import java.time.LocalDate;
import java.util.List;

public interface IWorkScheduleService {
    List<WorkShiftDTO> getStaffSchedule(Integer staffId, LocalDate startDate, LocalDate endDate);

    boolean isStaffActive(Integer staffId);
}
