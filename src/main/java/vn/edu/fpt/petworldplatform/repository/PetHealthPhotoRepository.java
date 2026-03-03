package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.PetHealthPhoto;

import java.util.List;

public interface PetHealthPhotoRepository extends JpaRepository<PetHealthPhoto, Integer> {
    List<PetHealthPhoto> findByRecord_Id(Integer recordId);
}
