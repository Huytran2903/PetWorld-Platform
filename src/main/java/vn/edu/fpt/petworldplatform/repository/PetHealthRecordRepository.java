package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.PetHealthRecord;

import java.util.List;
import java.util.Optional;

public interface PetHealthRecordRepository extends JpaRepository<PetHealthRecord, Integer> {
    Optional<PetHealthRecord> findTopByAppointment_IdOrderByUpdatedAtDesc(Integer appointmentId);

    List<PetHealthRecord> findByAppointment_Id(Integer appointmentId);

    Optional<PetHealthRecord> findByAppointment_IdAndAppointmentServiceLine_Id(Integer appointmentId, Integer serviceLineId);

    Optional<PetHealthRecord> findByAppointmentServiceLine_Id(Integer serviceLineId);
}
