package com.plr.aduaja.repository;

import com.plr.aduaja.model.MergeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MergeRecordRepository extends JpaRepository<MergeRecord, String> {

    List<MergeRecord> findByParentReportReportId(String parentReportId);

    List<MergeRecord> findByChildReportReportId(String childReportId);

    List<MergeRecord> findByMergedByUserId(String mergedByUserId);

    List<MergeRecord> findByIsActiveTrue();

    Optional<MergeRecord> findByParentReportReportIdAndChildReportReportIdAndIsActiveTrue(String parentReportId, String childReportId);

    Optional<MergeRecord> findByChildReportReportIdAndIsActiveTrue(String childReportId);

    List<MergeRecord> findByParentReportReportIdAndIsActiveTrue(String parentReportId);
}
