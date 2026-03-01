package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.PetHealthRecord;

import java.util.Optional;

public interface PetHealthRecordRepository extends JpaRepository<PetHealthRecord, Integer> {
    Optional<PetHealthRecord> findByAppointment_Id(Integer appointmentId);
}
