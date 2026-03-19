package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.util.List;
import java.util.Optional;

public interface PetVaccinationRepository extends JpaRepository<PetVaccinations, Integer> {
    // 1. Chuyển giao lịch tiêm chủng TƯƠNG LAI (Dùng ID và SQL thuần)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE PetVaccinations SET PerformedByStaffID = :newStaffId WHERE PerformedByStaffID = :oldStaffId AND NextDueDate >= CAST(GETDATE() AS DATE)", nativeQuery = true)
    void transferFutureVaccinations(@Param("oldStaffId") Integer oldStaffId, @Param("newStaffId") Integer newStaffId);

    // 2. Lệnh Quét sạch dấu vết QUÁ KHỨ (Dùng ID và SQL thuần)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE PetVaccinations SET PerformedByStaffID = NULL WHERE PerformedByStaffID = :oldStaffId", nativeQuery = true)
    void clearAllVaccinationReferences(@Param("oldStaffId") Integer oldStaffId);

    List<PetVaccinations> findByPerformedByStaff(Staff staff);

    List<PetVaccinations> findByPet_PetIDOrderByAdministeredDateDescCreatedAtDesc(Integer petId);

    Optional<PetVaccinations> findTopByPet_PetIDAndVaccineNameIgnoreCaseOrderByAdministeredDateDescCreatedAtDesc(
            Integer petId,
            String vaccineName
    );

    long countByVaccineNameIgnoreCase(String vaccineName);

    @Query("select pv from PetVaccinations pv " +
            "left join fetch pv.performedByStaff " +
            "where pv.appointment.id = :appointmentId " +
            "order by pv.administeredDate desc, pv.createdAt desc")
    List<PetVaccinations> findByAppointmentIdWithStaff(@Param("appointmentId") Integer appointmentId);

    @Query("select pv from PetVaccinations pv " +
            "left join fetch pv.performedByStaff " +
            "where pv.appointment.id in :appointmentIds " +
            "order by pv.appointment.id asc, pv.administeredDate desc, pv.createdAt desc")
    List<PetVaccinations> findByAppointmentIdsWithStaff(@Param("appointmentIds") List<Integer> appointmentIds);

    @Query("select pv from PetVaccinations pv " +
            "join fetch pv.pet p " +
            "left join fetch p.owner o " +
            "left join fetch pv.performedByStaff s " +
            "order by pv.administeredDate desc, pv.createdAt desc")
    List<PetVaccinations> findAllWithPetOwnerStaffOrderByAdministeredDateDescCreatedAtDesc();
}
