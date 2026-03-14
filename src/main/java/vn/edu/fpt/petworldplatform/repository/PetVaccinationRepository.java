package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.util.List;

public interface PetVaccinationRepository extends JpaRepository<PetVaccinations, Integer> {
    List<PetVaccinations> findByPerformedByStaff(Staff staff);
}
