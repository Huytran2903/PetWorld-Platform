package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    List<Appointment> findByCustomerIdOrderByAppointmentDateDesc(Long customerId);

    List<Appointment> findByCustomerIdAndStatusInOrderByAppointmentDateDesc(Long customerId, List<String> statuses);

    /** Count appointments at same date/time (for double-booking check). */
    long countByAppointmentDateAndStatusNot(LocalDateTime appointmentDate, String excludedStatus);

    long countByAppointmentDateAndStatusNotIn(LocalDateTime appointmentDate, List<String> excludedStatuses);
}
