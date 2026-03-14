package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummary;

import java.util.Optional;

public interface AppointmentSummaryRepository extends JpaRepository<AppointmentSummary, Integer> {
    Optional<AppointmentSummary> findByAppointment_Id(Integer appointmentId);
}
