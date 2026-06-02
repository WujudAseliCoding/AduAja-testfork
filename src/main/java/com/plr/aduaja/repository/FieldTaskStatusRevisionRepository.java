package com.plr.aduaja.repository;

import com.plr.aduaja.model.FieldTaskStatusRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldTaskStatusRevisionRepository extends JpaRepository<FieldTaskStatusRevision, String> {

    List<FieldTaskStatusRevision> findByTaskTaskIdOrderByChangedAtAsc(String taskId);
}
