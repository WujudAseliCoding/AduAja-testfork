package com.plr.aduaja.service;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.dto.CreateReportDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ============================================================
// ABSTRACTION (Abstraksi): ReportService Interface sebagai kontrak
// Controller hanya tahu interface ini, tidak tahu implementasinya
//
// POLYMORPHISM (Compile-time / Overloading):
// - getReportsByStatus(status)
// - getReportsByWarga(wargaId)
// - getReportsByDateRange(start, end)           ← Overload
// - getReportsByStatusAndDateRange(...)         ← Overload
// - updateStatus(id, status, notes, changedBy)
// - updateStatus(id, status, rej, notes, changedBy) ← Overload
// ============================================================
public interface ReportService {

    // ===========================
    // OVERLOADING (Compile-time Polymorphism)
    // Method sama, parameter beda
    // ===========================
    Optional<Report> findById(String id);
    Optional<Report> findByTicketNumber(String ticketNumber);
    List<Report> getAllReports();
    List<Report> getReportsByStatus(Report.ReportStatus status);
    List<Report> getReportsByWarga(String wargaId);
    List<Report> getReportsByDateRange(LocalDate start, LocalDate end);        // OVERLOAD
    List<Report> getReportsByStatusAndDateRange(Report.ReportStatus status,    // OVERLOAD
                                                 LocalDate start, LocalDate end);

    // ===========================
    // METHOD UTAMA
    // ===========================
    Report createReport(CreateReportDTO dto, String wargaId);

    // OVERLOADING: updateStatus dengan parameter beda
    Report updateStatus(String reportId, Report.ReportStatus newStatus, String notes, String changedBy);
    Report updateStatus(String reportId, Report.ReportStatus newStatus,
                        String rejectionReason, String adminNotes, String changedBy);  // OVERLOAD

    Report saveReportPhoto(String reportId, String photoBase64);
    long countByStatus(Report.ReportStatus status);
    long countByStatusAndRegion(Report.ReportStatus status, String regionId);
    List<Report> getReportsByStatusAndRegion(Report.ReportStatus status, String regionId);
    String generateTicketNumber();

    void addReportRevision(Report report, Report.ReportStatus oldStatus,
                           Report.ReportStatus newStatus, String notes, String changedBy);

    /**
     * Cascade status change to all active child tickets in a merge group.
     * Child tickets will mirror the parent status.
     */
    void cascadeStatusToChildren(String parentReportId, Report.ReportStatus newStatus,
                                  String notes, String changedBy);

    // ===========================
    // BACKWARD COMPATIBILITY (untuk WebController lama)
    // ===========================
    default Report updateStatus(String id, Report.ReportStatus status) {
        return updateStatus(id, status, null, "SYSTEM");
    }

    default List<Report> searchReports(String query) {
        return getAllReports().stream()
            .filter(r -> r.getDescription() != null &&
                         r.getDescription().toLowerCase().contains(query.toLowerCase()))
            .toList();
    }

    default List<Report> getReportsForDisposisi() {
        return getReportsByStatus(Report.ReportStatus.DITERIMA);
    }

    default Optional<Report> getReportById(String id) {
        return findById(id);
    }

    default Optional<Report> getReportByTicketNumber(String ticketNumber) {
        return findByTicketNumber(ticketNumber);
    }

    default List<Report> getReportsByUser(String userId) {
        return getReportsByWarga(userId);
    }

    default long countByReporterUserId(String userId) { return 0L; }

    // Backward compat: old WebController passes Report entity directly
    default Report createReport(Report report, String userId) {
        CreateReportDTO dto = new CreateReportDTO();
        dto.setDescription(report.getDescription());
        dto.setLocationHint(report.getLocationHint());
        dto.setLatitude(report.getLatitude());
        dto.setLongitude(report.getLongitude());
        dto.setPhotoBase64(report.getPhotoBase64());
        if (report.getPhotoTakenAt() != null) {
            dto.setPhotoTakenAt(report.getPhotoTakenAt().toString());
        }
        if (report.getCategory() != null) {
            dto.setCategoryId(report.getCategory().getCategoryId());
        }
        return createReport(dto, userId);
    }
}
