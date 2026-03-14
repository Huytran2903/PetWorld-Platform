package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.ServiceNote;

import java.util.List;
import java.util.Optional;

public interface ServiceNoteRepository extends JpaRepository<ServiceNote, Integer> {
    Optional<ServiceNote> findByAppointment_IdAndServiceLine_IdAndStaff_StaffId(Integer appointmentId, Integer serviceLineId, Integer staffId);

    List<ServiceNote> findByAppointment_Id(Integer appointmentId);

    List<ServiceNote> findByAppointment_IdOrderByUpdatedAtDesc(Integer appointmentId);
}
