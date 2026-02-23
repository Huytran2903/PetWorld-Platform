package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;

import java.util.List;

public interface AppointmentServiceLineRepository extends JpaRepository<AppointmentServiceLine, Integer> {

    List<AppointmentServiceLine> findByAppointment_Id(Integer appointmentId);

    @Query("SELECT asl FROM AppointmentServiceLine asl JOIN FETCH asl.service s WHERE asl.appointment.id IN :appointmentIds")
    List<AppointmentServiceLine> findAllByAppointmentIdsWithService(@Param("appointmentIds") List<Integer> appointmentIds);

    // delete all lines belonging to an appointment
    void deleteAllByAppointment(Appointment appointment);
}
