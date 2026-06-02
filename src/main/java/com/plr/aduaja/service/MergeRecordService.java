package com.plr.aduaja.service;

import com.plr.aduaja.dto.MergeDTO;
import com.plr.aduaja.model.MergeRecord;
import com.plr.aduaja.model.Report;

import java.util.List;
import java.util.Optional;

// ============================================================
// ABSTRACTION — Interface kontrak untuk MergeRecordService
// ============================================================
public interface MergeRecordService {

    MergeRecord createMerge(MergeDTO dto, String userId);

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    List<MergeRecord> getMerges();                   // tanpa parameter
    List<MergeRecord> getMerges(String reportId);    // 1 parameter String — OVERLOAD

    void cancelMerge(String mergeId);

    Optional<MergeRecord> getActiveMergeByChild(String childReportId);

    List<MergeRecord> getActiveMergesByParent(String parentReportId);

    List<Report> getAllChildReportsForParent(String parentReportId);
}
