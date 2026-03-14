package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummaryPhoto;

import java.util.List;

public interface AppointmentSummaryPhotoRepository extends JpaRepository<AppointmentSummaryPhoto, Integer> {

    List<AppointmentSummaryPhoto> findBySummary_Id(Integer summaryId);

    void deleteBySummary_Id(Integer summaryId);
}

