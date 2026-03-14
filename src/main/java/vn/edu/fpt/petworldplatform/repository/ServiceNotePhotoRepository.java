package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.ServiceNotePhoto;

import java.util.List;

public interface ServiceNotePhotoRepository extends JpaRepository<ServiceNotePhoto, Integer> {
    List<ServiceNotePhoto> findByServiceNote_Id(Integer serviceNoteId);

    void deleteByServiceNote_Id(Integer serviceNoteId);
}
