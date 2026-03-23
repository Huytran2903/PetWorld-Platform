package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional; // Thêm import này
import vn.edu.fpt.petworldplatform.entity.AccessControl;
import vn.edu.fpt.petworldplatform.entity.AccessControlId; // Thêm import này

import java.util.List;

public interface AccessControlRepository extends JpaRepository<AccessControl, AccessControlId> {

    List<AccessControl> findByRoleId(Integer roleId);

    @Modifying
    @Transactional
    void deleteByRoleId(Integer roleId);
}