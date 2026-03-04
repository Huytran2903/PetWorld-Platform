package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.entity.StaffSchedule;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;
import vn.edu.fpt.petworldplatform.repository.StaffScheduleRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkScheduleService implements IWorkScheduleService {

    private final StaffScheduleRepository staffScheduleRepository;
    private final StaffRepository staffRepository;

    @Override
    public List<StaffSchedule> getStaffSchedule(Integer staffId, LocalDate startDate, LocalDate endDate) {
        if (!isStaffActive(staffId)) {
            throw new IllegalStateException("Account is not active");
        }

        return staffScheduleRepository.findByStaffIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(
                staffId.longValue(), startDate, endDate
        );
    }

    @Override
    public boolean isStaffActive(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));
        return staff.getIsActive() != null && staff.getIsActive();
    }
}
