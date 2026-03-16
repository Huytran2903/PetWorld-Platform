package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.util.List;

public interface PetVaccinationRepository extends JpaRepository<PetVaccinations, Integer> {
    // 1. Chuyển giao lịch tiêm chủng TƯƠNG LAI (Dùng ID và SQL thuần)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE PetVaccinations SET PerformedByStaffID = :newStaffId WHERE PerformedByStaffID = :oldStaffId AND NextDueDate >= CAST(GETDATE() AS DATE)", nativeQuery = true)
    void transferFutureVaccinations(@Param("oldStaffId") Integer oldStaffId, @Param("newStaffId") Integer newStaffId);

    // 2. Lệnh Quét sạch dấu vết QUÁ KHỨ (Dùng ID và SQL thuần)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE PetVaccinations SET PerformedByStaffID = NULL WHERE PerformedByStaffID = :oldStaffId", nativeQuery = true)
    void clearAllVaccinationReferences(@Param("oldStaffId") Integer oldStaffId);
}
