package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final StaffRepository staffRepository;

    @Override
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Override
    public Appointment getAppointmentById(Integer id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));
    }

    @Override
    public void assignStaffToAppointment(Integer appointmentId, Long staffId) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (!"pending".equalsIgnoreCase(appointment.getStatus())) {
            throw new IllegalStateException("Only pending appointments can be assigned.");
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        if (!staff.getIsActive()) {
            throw new IllegalStateException("Staff is not active.");
        }

        // Check for schedule conflict
        long conflictCount = appointmentRepository.countOverlappingStaffAppointments(
                staffId,
                appointmentId,
                appointment.getAppointmentDate(),
                appointment.getEndTime()
        );

        if (conflictCount > 0) {
            throw new IllegalStateException("Staff is busy in this time slot.");
        }

        appointment.setStaffId(staffId);
        appointment.setStatus("confirmed");
        appointment.setUpdatedAt(java.time.LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    @Override
    public void reassignStaff(Integer appointmentId, Long newStaffId) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (!"confirmed".equalsIgnoreCase(appointment.getStatus())) {
            throw new IllegalStateException("Only confirmed appointments can be reassigned.");
        }

        Staff staff = staffRepository.findById(newStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + newStaffId));

        if (!staff.getIsActive()) {
            throw new IllegalStateException("Staff is not active.");
        }

        // Check for schedule conflict
        long conflictCount = appointmentRepository.countOverlappingStaffAppointments(
                newStaffId,
                appointmentId,
                appointment.getAppointmentDate(),
                appointment.getEndTime()
        );

        if (conflictCount > 0) {
            throw new IllegalStateException("Staff is busy in this time slot.");
        }

        appointment.setStaffId(newStaffId);
        appointment.setUpdatedAt(java.time.LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    public List<Staff> getAvailableStaffForAppointment(Integer appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);
        List<Staff> activeStaff = staffRepository.findByIsActiveTrue();

        return activeStaff.stream().filter(staff -> {
            long conflictCount = appointmentRepository.countOverlappingStaffAppointments(
                    staff.getStaffId(),
                    appointmentId,
                    appointment.getAppointmentDate(),
                    appointment.getEndTime()
            );
            return conflictCount == 0;
        }).collect(Collectors.toList());
    }
}
