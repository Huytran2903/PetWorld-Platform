package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;

import java.util.List;

public interface AppointmentServiceLineRepository extends JpaRepository<AppointmentServiceLine, Integer> {

    List<AppointmentServiceLine> findByAppointment_Id(Integer appointmentId);
}
