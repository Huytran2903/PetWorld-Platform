package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.ServiceNote;

import java.util.List;
import java.util.Optional;

public interface ServiceNoteRepository extends JpaRepository<ServiceNote, Integer> {
    Optional<ServiceNote> findByAppointment_IdAndServiceLine_IdAndStaff_StaffId(Integer appointmentId, Integer serviceLineId, Integer staffId);

    List<ServiceNote> findByAppointment_Id(Integer appointmentId);

    List<ServiceNote> findByAppointment_IdOrderByUpdatedAtDesc(Integer appointmentId);

    @Modifying
    @Query("UPDATE ServiceNote s SET s.staff.staffId = :newId WHERE s.staff.staffId = :oldId AND s.status = 'draft'")
    void transferDraftNotes(@Param("oldId") Integer oldId, @Param("newId") Integer newId);

    @Modifying
    @Query("UPDATE ServiceNote s SET s.staff = null WHERE s.staff.staffId = :oldId AND s.status = 'done'")
    void clearStaffFromDoneNotes(@Param("oldId") Integer oldId);
}
