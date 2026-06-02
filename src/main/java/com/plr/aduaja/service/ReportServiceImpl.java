package com.plr.aduaja.service;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.ReportRevision;
import com.plr.aduaja.model.ReportCategory;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.ReportCategoryRepository;
import com.plr.aduaja.repository.ReportRevisionRepository;
import com.plr.aduaja.repository.RegionRepository;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.dto.CreateReportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ============================================================
// POLYMORPHISM (Run-time): ReportServiceImpl implements ReportService
// Setiap method @Override = POLYMORPHISM sejati
// ABSTRACTION: Controller hanya tahu interface ReportService
// ============================================================
@Service
@Transactional
public class ReportServiceImpl implements ReportService {  // ← POLYMORPHISM

    // ABSTRACTION: hanya inject interface Repository
    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportCategoryRepository categoryRepository;

    @Autowired
    private ReportRevisionRepository revisionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    @Lazy
    private MergeRecordService mergeRecordService;

    // ===========================
    // @Override — Run-time Polymorphism
    // ===========================

    @Override  // ← POLYMORPHISM
    public Optional<Report> findById(String id) {
        return reportRepository.findById(id);
    }

    @Override  // ← POLYMORPHISM
    public Optional<Report> findByTicketNumber(String ticketNumber) {
        return reportRepository.findByTicketNumber(ticketNumber);
    }

    @Override  // ← POLYMORPHISM
    public List<Report> getAllReports() {
        return reportRepository.findAllByOrderBySubmittedAtDesc();
    }

    @Override  // ← POLYMORPHISM
    public List<Report> getReportsByStatus(Report.ReportStatus status) {
        return reportRepository.findByStatusOrderBySubmittedAtDesc(status);
    }

    @Override  // ← POLYMORPHISM
    public List<Report> getReportsByWarga(String wargaId) {
        return reportRepository.findByReporterUserIdOrderBySubmittedAtDesc(wargaId);
    }

    @Override  // ← POLYMORPHISM (Overload: 2 parameter)
    public List<Report> getReportsByDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(LocalTime.MAX);
        return reportRepository.findByDateRange(startDt, endDt);
    }

    @Override  // ← POLYMORPHISM (Overload: 3 parameter beda)
    public List<Report> getReportsByStatusAndDateRange(Report.ReportStatus status,
                                                        LocalDate start,
                                                        LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(LocalTime.MAX);
        return reportRepository.findByStatusAndDateRange(status, startDt, endDt);
    }

    @Override  // ← POLYMORPHISM
    public Report createReport(CreateReportDTO dto, String wargaId) {
        // ABSTRACTION: Controller tidak tahu detail pembuatan laporan
        User reporter = userRepository.findById(wargaId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        Report report = new Report();
        report.setTicketNumber(generateTicketNumber());
        report.setReporter(reporter);
        report.setDescription(dto.getDescription());
        report.setLocationHint(dto.getLocationHint());
        report.setLatitude(dto.getLatitude());
        report.setLongitude(dto.getLongitude());
        report.setPhotoBase64(dto.getPhotoBase64());
        report.setSubmittedAt(LocalDateTime.now());

        // FR-ADM-06: Set photo taken at from EXIF if provided
        if (dto.getPhotoTakenAt() != null && !dto.getPhotoTakenAt().isBlank()) {
            try {
                report.setPhotoTakenAt(LocalDateTime.parse(dto.getPhotoTakenAt()));
            } catch (Exception e) {
                // ignore parse failure
            }
        }

        // FR-ADM-04: Auto-reject if no photo or GPS coordinates
        boolean hasPhoto = dto.getPhotoBase64() != null && !dto.getPhotoBase64().isBlank();
        boolean hasGps = dto.getLatitude() != null && dto.getLongitude() != null;
        if (!hasPhoto || !hasGps) {
            StringBuilder reason = new StringBuilder("Laporan ditolak otomatis: ");
            if (!hasPhoto) reason.append("tidak menyertakan foto. ");
            if (!hasGps) reason.append("tidak menyertakan koordinat GPS. ");
            report.setStatus(Report.ReportStatus.DITOLAK);
            report.setRejectionReason(reason.toString().trim());
            report.setAdminNotes("Ditolak otomatis oleh sistem (FR-ADM-04)");
        } else {
            report.setStatus(Report.ReportStatus.MENUNGGU_VERIFIKASI);
        }

        if (dto.getCategoryId() != null && !dto.getCategoryId().isBlank()) {
            categoryRepository.findById(dto.getCategoryId())
                .ifPresent(report::setCategory);
        }

        if (dto.getRegionId() != null && !dto.getRegionId().isBlank()) {
            regionRepository.findById(dto.getRegionId())
                .ifPresent(report::setRegion);
        }

        return reportRepository.save(report);
    }

    @Override  // ← POLYMORPHISM
    public Report updateStatus(String reportId, Report.ReportStatus newStatus,
                                String notes, String changedBy) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));

        // Cegah perubahan status langsung pada laporan yang sudah digabungkan
        if (report.getStatus() == Report.ReportStatus.TERGABUNG) {
            throw new IllegalStateException("Laporan yang sudah digabungkan tidak dapat diubah statusnya secara langsung.");
        }

        // Cegah tolak/revisi pada parent yang memiliki child aktif (query langsung ke DB)
        if ((newStatus == Report.ReportStatus.DITOLAK || newStatus == Report.ReportStatus.MENUNGGU_REVISI)
            && reportRepository.countByParentReportReportId(reportId) > 0) {
            throw new IllegalStateException("Laporan ini memiliki child ticket yang digabungkan. Tidak dapat ditolak atau direvisi.");
        }

        Report.ReportStatus oldStatus = report.getStatus();

        if (notes != null) {
            report.setAdminNotes(notes);
        }
        report.setStatus(newStatus);

        Report saved = reportRepository.save(report);

        // Buat revision record (audit trail)
        createRevision(saved, oldStatus, newStatus, notes, changedBy);

        // Cascade status ke child tickets (merge group)
        cascadeStatusToChildren(reportId, newStatus, notes, changedBy);

        return saved;
    }

    @Override  // ← POLYMORPHISM (Overload: 4 parameter, beda signature)
    public Report updateStatus(String reportId, Report.ReportStatus newStatus,
                                String rejectionReason, String adminNotes,
                                String changedBy) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));

        // Cegah perubahan status langsung pada laporan yang sudah digabungkan
        if (report.getStatus() == Report.ReportStatus.TERGABUNG) {
            throw new IllegalStateException("Laporan yang sudah digabungkan tidak dapat diubah statusnya secara langsung.");
        }

        // Cegah tolak/revisi pada parent yang memiliki child aktif (query langsung ke DB)
        if ((newStatus == Report.ReportStatus.DITOLAK || newStatus == Report.ReportStatus.MENUNGGU_REVISI)
            && reportRepository.countByParentReportReportId(reportId) > 0) {
            throw new IllegalStateException("Laporan ini memiliki child ticket yang digabungkan. Tidak dapat ditolak atau direvisi.");
        }

        Report.ReportStatus oldStatus = report.getStatus();
        report.setStatus(newStatus);
        report.setRejectionReason(rejectionReason);
        report.setAdminNotes(adminNotes);

        Report saved = reportRepository.save(report);

        createRevision(saved, oldStatus, newStatus, rejectionReason, changedBy);

        // Cascade status ke child tickets (merge group)
        cascadeStatusToChildren(reportId, newStatus, rejectionReason, changedBy);

        return saved;
    }

    @Override  // ← POLYMORPHISM
    public Report saveReportPhoto(String reportId, String photoBase64) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));
        report.setPhotoBase64(photoBase64);
        return reportRepository.save(report);
    }

    @Override  // ← POLYMORPHISM
    public long countByStatus(Report.ReportStatus status) {
        return reportRepository.countByStatus(status);
    }

    @Override
    public long countByStatusAndRegion(Report.ReportStatus status, String regionId) {
        return reportRepository.countByStatusAndRegionRegionId(status, regionId);
    }

    @Override
    public List<Report> getReportsByStatusAndRegion(Report.ReportStatus status, String regionId) {
        return reportRepository.findByStatusAndRegionRegionIdOrderBySubmittedAtDesc(status, regionId);
    }

    @Override
    public void addReportRevision(Report report, Report.ReportStatus oldStatus,
                                   Report.ReportStatus newStatus, String notes, String changedBy) {
        if (oldStatus == newStatus) return;
        createRevision(report, oldStatus, newStatus, notes, changedBy);
    }

    @Override  // ← POLYMORPHISM
    public String generateTicketNumber() {
        return "RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ===========================
    // PRIVATE HELPER — ENKAPSULASI
    // Disembunyikan dari luar (private)
    // ===========================
    private void createRevision(Report report, Report.ReportStatus oldStatus,
                                 Report.ReportStatus newStatus,
                                 String notes, String changedBy) {
        ReportRevision revision = new ReportRevision();
        revision.setReport(report);
        revision.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        revision.setNewStatus(newStatus.name());
        revision.setNotes(notes);
        revision.setChangedBy(changedBy != null ? changedBy : "SYSTEM");
        revision.setChangedAt(LocalDateTime.now());
        revisionRepository.save(revision);
    }

    /**
     * Cascade status change to all active child tickets in merge group.
     * Child tickets will mirror the parent status so they can participate
     * in confirmation/dispute flows.
     */
    @Override
    public void cascadeStatusToChildren(String parentReportId, Report.ReportStatus newStatus,
                                          String notes, String changedBy) {
        try {
            List<Report> children = mergeRecordService.getAllChildReportsForParent(parentReportId);
            for (Report child : children) {
                if (child.getStatus() == newStatus) continue;
                Report.ReportStatus childOldStatus = child.getStatus();
                child.setStatus(newStatus);
                reportRepository.save(child);
                createRevision(child, childOldStatus, newStatus,
                    "Status diselaraskan dengan parent: " + (notes != null ? notes : ""), changedBy);
            }
        } catch (Exception e) {
            // Cascade failure should not break the primary status update
        }
    }
}
