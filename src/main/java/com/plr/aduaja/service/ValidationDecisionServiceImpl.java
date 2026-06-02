package com.plr.aduaja.service;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.User;
import com.plr.aduaja.model.ValidationDecision;
import com.plr.aduaja.model.ValidationDecision.Decision;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.repository.ValidationDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// POLYMORPHISM — @Override (Run-time Polymorphism)
// ============================================================
@Service
public class ValidationDecisionServiceImpl implements ValidationDecisionService {

    @Autowired
    private ValidationDecisionRepository validationDecisionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportService reportService;

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public ValidationDecision createDecision(String reportId, String adminId, Decision decision, String reason) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report tidak ditemukan: " + reportId));

        // Cegah validasi langsung pada child ticket yang sudah digabungkan
        if (report.getStatus() == Report.ReportStatus.TERGABUNG) {
            throw new IllegalStateException("Laporan yang sudah digabungkan tidak dapat divalidasi secara langsung.");
        }

        // Cegah tolak/revisi pada parent ticket yang memiliki child aktif
        if ((decision == Decision.DITOLAK || decision == Decision.DIREVISI)
            && !report.getChildMergeRecords().isEmpty()
            && report.getChildMergeRecords().stream().anyMatch(mr -> Boolean.TRUE.equals(mr.getIsActive()))) {
            throw new IllegalStateException("Laporan ini memiliki child ticket yang digabungkan. Tidak dapat ditolak atau direvisi.");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan: " + adminId));

        ValidationDecision vd = new ValidationDecision();
        vd.setReport(report);
        vd.setAdmin(admin);
        vd.setDecision(decision);
        vd.setRejectionReason(reason);
        vd.setDecidedAt(LocalDateTime.now());

        Report.ReportStatus oldStatus = report.getStatus();
        if (decision == Decision.DITERIMA) {
            report.setStatus(Report.ReportStatus.DITERIMA);
            reportRepository.save(report);
            reportService.addReportRevision(report, oldStatus, Report.ReportStatus.DITERIMA,
                "Laporan diterima oleh admin", adminId);
        } else if (decision == Decision.DITOLAK) {
            report.setStatus(Report.ReportStatus.DITOLAK);
            reportRepository.save(report);
            String notes = reason != null ? "Laporan ditolak: " + reason : "Laporan ditolak oleh admin";
            reportService.addReportRevision(report, oldStatus, Report.ReportStatus.DITOLAK,
                notes, adminId);
        } else if (decision == Decision.DIREVISI) {
            report.setStatus(Report.ReportStatus.MENUNGGU_REVISI);
            reportRepository.save(report);
            String notes = reason != null ? "Laporan perlu direvisi: " + reason : "Laporan perlu direvisi";
            reportService.addReportRevision(report, oldStatus, Report.ReportStatus.MENUNGGU_REVISI,
                notes, adminId);
        }

        return validationDecisionRepository.save(vd);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — tanpa parameter)
    public List<ValidationDecision> getDecisions() {
        return validationDecisionRepository.findAll();
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 1 parameter String)
    public List<ValidationDecision> getDecisions(String reportOrAdminId) {
        List<ValidationDecision> byReport = validationDecisionRepository.findByReportReportId(reportOrAdminId);
        if (!byReport.isEmpty()) return byReport;
        return validationDecisionRepository.findByAdminUserId(reportOrAdminId);
    }
}
