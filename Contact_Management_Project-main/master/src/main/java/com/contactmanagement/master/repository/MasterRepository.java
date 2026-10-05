package com.contactmanagement.master.repository;

import com.contactmanagement.master.entity.MasterEntity;
import com.contactmanagement.master.enums.MasterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasterRepository extends JpaRepository<MasterEntity, Long> {
    Page<MasterEntity> findAllByType(MasterType type, Pageable pageable);

    List<MasterEntity> findAllByTypeAndStatusFalseOrderByNameAsc(MasterType type);

    Optional<MasterEntity> findByIdAndType(Long id, MasterType type);

    Optional<MasterEntity> findByTypeAndNameIgnoreCase(MasterType type, String name);

    boolean existsByTypeAndNameIgnoreCaseAndIdNot(MasterType type, String name, Long id);
}
