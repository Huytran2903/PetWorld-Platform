package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummaryPhoto;

import java.util.Collection;
import java.util.List;

public interface AppointmentSummaryPhotoRepository extends JpaRepository<AppointmentSummaryPhoto, Integer> {

    List<AppointmentSummaryPhoto> findBySummary_Id(Integer summaryId);

    /**
     * Load evidence photos for health summaries on the given appointments (one query per list page).
     */
    @Query("SELECT DISTINCT p FROM AppointmentSummaryPhoto p "
            + "JOIN FETCH p.summary s "
            + "JOIN FETCH s.appointment a "
            + "WHERE a.id IN :appointmentIds")
    List<AppointmentSummaryPhoto> findAllByAppointmentIdIn(@Param("appointmentIds") Collection<Integer> appointmentIds);

    void deleteBySummary_Id(Integer summaryId);
}

