package com.plr.aduaja.service;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.SlaRecord;
import com.plr.aduaja.model.SlaRecord.SlaStatus;
import com.plr.aduaja.model.TaskPostponement;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.SlaRecordRepository;
import com.plr.aduaja.repository.TaskPostponementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

// ============================================================
// POLYMORPHISM — @Override (Run-time Polymorphism)
// ENCAPSULATION — inject lewat Interface, field private
// ============================================================
@Service
public class SlaMonitoringServiceImpl implements SlaMonitoringService {

    @Autowired
    private SlaRecordRepository slaRecordRepository;

    @Autowired
    private TaskPostponementRepository taskPostponementRepository;

    @Autowired
    private NotificationService notificationService;

    // FIX SCN-08: Inject ConfirmationService untuk processTimeouts scheduler
    @Autowired
    private ConfirmationService confirmationService;

    @Autowired
    private ReportRepository reportRepository;

    // Scheduled job — cek SLA violations tiap jam
    @Scheduled(fixedRate = 3600000)
    public void checkSlaViolations() {
        LocalDateTime now = LocalDateTime.now();
        List<SlaRecord> overdue = slaRecordRepository.findOverdue(now);

        for (SlaRecord sla : overdue) {
            if (sla.getCurrentStatus() == SlaStatus.BERJALAN) {
                sla.setCurrentStatus(SlaStatus.TERLAMBAT);
                slaRecordRepository.save(sla);

                // Auto-set Report.status to TERLAMBAT
                Report report = sla.getReport();
                if (report != null && report.getStatus() != Report.ReportStatus.TERLAMBAT
                        && report.getStatus() != Report.ReportStatus.SELESAI
                        && report.getStatus() != Report.ReportStatus.SELESAI_OTOMATIS
                        && report.getStatus() != Report.ReportStatus.DITOLAK) {
                    report.setStatus(Report.ReportStatus.TERLAMBAT);
                    reportRepository.save(report);
                }
            }
        }

        // FIX SCN-08: Jalankan processTimeouts untuk konfirmasi yang expired
        try {
            confirmationService.processTimeouts();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(SlaMonitoringServiceImpl.class)
                .error("[SCN-08] Gagal proses confirmation timeouts: {}", e.getMessage(), e);
        }
    }

    // Cron job — pantau batas waktu penundaan tugas tiap jam
    @Scheduled(fixedRate = 3600000)
    public void checkOverduePostponements() {
        LocalDateTime now = LocalDateTime.now();
        List<TaskPostponement> pendingPostponements = taskPostponementRepository
            .findByApprovalStatus(TaskPostponement.ApprovalStatus.MENUNGGU);

        for (TaskPostponement postponement : pendingPostponements) {
            // Jika estimatedResumeAt sudah terlewat dan masih MENUNGGU persetujuan
            if (postponement.getEstimatedResumeAt() != null 
                    && postponement.getEstimatedResumeAt().isBefore(now)) {
                long hoursOverdue = Duration.between(postponement.getEstimatedResumeAt(), now).toHours();
                // Log peringatan — admin perlu meninjau
                org.slf4j.LoggerFactory.getLogger(SlaMonitoringServiceImpl.class).warn(
                    "Pengajuan penundaan ID {} untuk tugas {} sudah melewati estimasi resume "
                    + "sejak {} jam lalu. Mohon admin meninjau dan mengambil tindakan.",
                    postponement.getPostponementId(),
                    postponement.getTask() != null ? postponement.getTask().getTaskId() : "N/A",
                    hoursOverdue
                );
            }
        }
    }

    // Notifikasi Prediktif In-App Petugas (SLA Kritis & Terlewat)
    @Scheduled(fixedRate = 1800000) // 30 menit
    public void predictiveSlaNotificationAlert() {
        LocalDateTime now = LocalDateTime.now();
        List<SlaRecord> activeSlas = slaRecordRepository.findAll().stream()
            .filter(sla -> sla.getCurrentStatus() == SlaStatus.BERJALAN || sla.getCurrentStatus() == SlaStatus.TERLAMBAT)
            .toList();

        for (SlaRecord sla : activeSlas) {
            if (sla.getReport() == null || sla.getReport().getFieldTasks() == null) continue;

            for (com.plr.aduaja.model.FieldTask task : sla.getReport().getFieldTasks()) {
                if (task.getOfficer() == null || (task.getTaskStatus() != com.plr.aduaja.model.FieldTask.TaskStatus.BARU && task.getTaskStatus() != com.plr.aduaja.model.FieldTask.TaskStatus.SEDANG_DIKERJAKAN)) {
                    continue;
                }

                String officerId = task.getOfficer().getUserId();
                String taskId = task.getTaskId();

                if (sla.getCurrentStatus() == SlaStatus.TERLAMBAT || (sla.getSlaDeadlineAt() != null && sla.getSlaDeadlineAt().isBefore(now))) {
                    List<com.plr.aduaja.model.Notification> exist = notificationService.getNotificationsByType(officerId, "SLA_LATE_" + taskId);
                    if (exist.isEmpty()) {
                        notificationService.createNotification(officerId, "🚨 SLA Terlewat", 
                            "Tugas " + taskId + " telah melewati batas waktu SLA!", "SLA_LATE_" + taskId, taskId);
                    }
                } else if (sla.getSlaDeadlineAt() != null) {
                    long hoursLeft = Duration.between(now, sla.getSlaDeadlineAt()).toHours();
                    if (hoursLeft < 2 && hoursLeft >= 0) {
                        List<com.plr.aduaja.model.Notification> exist = notificationService.getNotificationsByType(officerId, "SLA_WARNING_" + taskId);
                        if (exist.isEmpty()) {
                            notificationService.createNotification(officerId, "⏳ Peringatan SLA", 
                                "Batas waktu tugas " + taskId + " tersisa kurang dari 2 jam!", "SLA_WARNING_" + taskId, taskId);
                        }
                    }
                }
            }
        }
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Map<String, Object> getSlaStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", slaRecordRepository.count());
        stats.put("berjalan", slaRecordRepository.countByCurrentStatus(SlaStatus.BERJALAN));
        stats.put("tertunda", slaRecordRepository.countByCurrentStatus(SlaStatus.TERTUNDA));
        stats.put("terlambat", slaRecordRepository.countByCurrentStatus(SlaStatus.TERLAMBAT));
        stats.put("selesai", slaRecordRepository.countByCurrentStatus(SlaStatus.SELESAI));
        return stats;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<Map<String, Object>> getLateItems() {
        LocalDateTime now = LocalDateTime.now();
        List<SlaRecord> overdue = slaRecordRepository.findOverdue(now);
        List<Map<String, Object>> items = new ArrayList<>();
        for (SlaRecord sla : overdue) {
            Map<String, Object> item = new HashMap<>();
            Report report = sla.getReport();
            if (report != null) {
                item.put("reportId", report.getReportId());
                item.put("ticketNumber", report.getTicketNumber());
                item.put("status", report.getStatus() != null ? report.getStatus().toString() : "N/A");
            }
            item.put("slaId", sla.getSlaId());
            item.put("deadline", sla.getSlaDeadlineAt());
            item.put("minutesLate", Duration.between(sla.getSlaDeadlineAt(), now).toMinutes());
            items.add(item);
        }
        return items;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Map<String, Object> getReportSlaStatus(String reportId) {
        Optional<SlaRecord> slaOpt = slaRecordRepository.findByReportReportId(reportId);
        Map<String, Object> result = new HashMap<>();
        if (slaOpt.isPresent()) {
            SlaRecord sla = slaOpt.get();
            result.put("slaId", sla.getSlaId());
            result.put("status", sla.getCurrentStatus());
            result.put("deadline", sla.getSlaDeadlineAt());
            result.put("pausedMinutes", sla.getTotalPausedMinutes());
        } else {
            result.put("status", "TIDAK_ADA");
        }
        return result;
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — tanpa parameter)
    public List<Map<String, Object>> getSlaSummary() {
        List<Map<String, Object>> summary = new ArrayList<>();
        Map<String, Object> stats = getSlaStatistics();
        summary.add(stats);
        return summary;
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 1 parameter)
    public List<Map<String, Object>> getSlaSummary(String dinasId) {
        // Filter by dinasId bila diperlukan — saat ini return semua
        return getSlaSummary();
    }
}
