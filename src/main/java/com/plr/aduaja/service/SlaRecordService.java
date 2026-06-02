package com.plr.aduaja.service;

import com.plr.aduaja.model.SlaRecord;
import com.plr.aduaja.model.SlaRecord.SlaStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ============================================================
// ABSTRACTION — Interface sebagai kontrak (disembunyikan dari Controller)
// Controller hanya tahu Interface, tidak tahu SlaRecordServiceImpl
// ============================================================
public interface SlaRecordService {

    Optional<SlaRecord> findById(String id);
    Optional<SlaRecord> findByReportId(String reportId);
    List<SlaRecord> getAllRecords();

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    // POLYMORPHISM (Compile-time) — Overloading
    List<SlaRecord> getRecords(SlaStatus status);                            // 1 parameter
    List<SlaRecord> getRecords(LocalDateTime start, LocalDateTime end);      // 2 parameter — OVERLOAD

    SlaRecord createSlaRecord(String reportId, Integer durationHours);
    SlaRecord pauseSla(String slaId, String reason, String pausedByUserId);
    SlaRecord resumeSla(String slaId);
    SlaRecord completeSla(String slaId);
    void checkAndUpdateOverdueSla();

    // Mark overdue SLA as reviewed
    SlaRecord markOverdueReviewed(String slaId, String notes);
}
