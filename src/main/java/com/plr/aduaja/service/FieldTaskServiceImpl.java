package com.plr.aduaja.service;

import com.plr.aduaja.model.*;
import com.plr.aduaja.model.FieldTask.TaskStatus;
import com.plr.aduaja.repository.*;
import com.plr.aduaja.util.GeoUtils;
import com.plr.aduaja.util.PhotoWatermarkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FieldTaskServiceImpl implements FieldTaskService {

    @Autowired
    private FieldTaskRepository fieldTaskRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlaRecordRepository slaRecordRepository;

    @Autowired
    private TaskEvidenceRepository taskEvidenceRepository;

    @Autowired
    private TaskPostponementRepository taskPostponementRepository;  // FIX: inject repo untuk simpan record penundaan

    @Autowired
    private ConfirmationRequestRepository confirmationRequestRepository;  // FIX: untuk buat ConfirmationRequest saat task selesai

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private FieldTaskStatusRevisionRepository fieldTaskStatusRevisionRepository;

    @Autowired
    private MergeRecordService mergeRecordService;

    @Autowired
    private ConfirmationService confirmationService;

    @Autowired
    private NotificationService notificationService;

    private static final Logger log = LoggerFactory.getLogger(FieldTaskServiceImpl.class);

    @Override
    public List<FieldTask> getAllTasks() {
        return fieldTaskRepository.findAll();
    }

    @Override
    public Optional<FieldTask> getTaskById(String taskId) {
        return fieldTaskRepository.findById(taskId);
    }

    @Override
    public List<FieldTask> getTasksByOfficer(String officerId) {
        log.info("[QUERY] getTasksByOfficer — officerId={}", officerId);
        List<FieldTask> result = fieldTaskRepository.findByOfficerUserId(officerId);
        log.info("[QUERY] getTasksByOfficer — count={}", result.size());
        for (FieldTask t : result) {
            log.info("[QUERY] taskId={}, status={}, officerId={}, reportId={}",
                t.getTaskId(), t.getTaskStatus(),
                t.getOfficer() != null ? t.getOfficer().getUserId() : "null",
                t.getReport() != null ? t.getReport().getReportId() : "null");
        }
        return result;
    }

    @Override
    public List<FieldTask> getTasksByOfficerAndStatus(String officerId, TaskStatus status) {
        return fieldTaskRepository.findByOfficerUserIdAndTaskStatus(officerId, status);
    }

    @Override
    public List<FieldTask> getTasksByStatus(TaskStatus status) {
        return fieldTaskRepository.findByTaskStatus(status);
    }

    @Override
    public List<FieldTask> getTasksByReport(String reportId) {
        log.info("[QUERY] getTasksByReport — reportId={}", reportId);
        List<FieldTask> result = fieldTaskRepository.findByReportReportId(reportId);
        log.info("[QUERY] getTasksByReport — count={}", result.size());
        for (FieldTask t : result) {
            log.info("[QUERY] taskId={}, status={}, officerId={}",
                t.getTaskId(), t.getTaskStatus(),
                t.getOfficer() != null ? t.getOfficer().getUserId() : "null");
        }
        return result;
    }

    @Override
    public List<FieldTask> getTasksByDateRange(LocalDateTime start, LocalDateTime end) {
        return fieldTaskRepository.findByStartedAtBetween(start, end);
    }

    @Override
    public FieldTask createTask(String reportId, String officerId, String assignedById) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Officer not found"));
        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> new RuntimeException("Assigner not found"));

        FieldTask task = new FieldTask();
        task.setReport(report);
        task.setOfficer(officer);
        task.setAssignedBy(assignedBy);
        task.setTaskStatus(TaskStatus.BARU);

        SlaRecord sla = slaRecordRepository.findByReportReportId(reportId).orElse(null);
        task.setSlaRecord(sla);

        FieldTask saved = fieldTaskRepository.save(task);

        // FIX SCN-01 (4.8): Update status laporan ke DITUGASKAN setelah petugas ditugaskan
        try {
            Report.ReportStatus oldStatus = report.getStatus();
            report.setStatus(Report.ReportStatus.DITUGASKAN);
            reportRepository.save(report);
            reportService.addReportRevision(report, oldStatus, Report.ReportStatus.DITUGASKAN,
                "Laporan ditugaskan ke petugas", assignedById);
            reportService.cascadeStatusToChildren(report.getReportId(), Report.ReportStatus.DITUGASKAN,
                "Status diselaraskan dengan parent", assignedById);
        } catch (Exception e) {
            log.warn("Gagal update status laporan ke DITUGASKAN: {}", e.getMessage());
        }

        // FR-PRS-03: Validasi wilayah tugas petugas vs lokasi laporan
        try {
            UserProfile profile = userProfileRepository.findByUserUserId(officerId).orElse(null);
            if (profile != null && profile.getWilayahTugas() != null && report.getLocationHint() != null) {
                String wilayahPetugas = profile.getWilayahTugas().getRegionName().toLowerCase();
                String lokasiLaporan = report.getLocationHint().toLowerCase();
                if (!lokasiLaporan.contains(wilayahPetugas) && !wilayahPetugas.contains(lokasiLaporan)) {
                    log.warn("FR-PRS-03: Wilayah tugas petugas '{}' tidak sesuai dengan lokasi laporan '{}'",
                            profile.getWilayahTugas().getRegionName(), report.getLocationHint());
                }
            }
        } catch (Exception e) {
            log.warn("FR-PRS-03: Gagal validasi wilayah: {}", e.getMessage());
        }

        try {
            notificationService.createNotification(officerId, 
                "Tugas Baru: " + report.getTicketNumber(), 
                "Anda mendapat penugasan baru dari admin dinas. Silakan periksa daftar tugas Anda.",
                "NEW_TASK", saved.getTaskId());
        } catch (Exception e) {
            log.warn("Gagal membuat notifikasi tugas baru: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    public FieldTask startTask(String taskId, BigDecimal latitude, BigDecimal longitude) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // FR-PTG-18: Validasi jarak petugas ke lokasi laporan sebelum mulai
        // Radius toleransi: 10 km (dapat dikonfigurasi). Jika koordinat tidak ada,
        // tetap izinkan (fallback graceful agar tidak block petugas tanpa GPS).
        if (latitude != null && longitude != null && task.getReport() != null) {
            Report report = task.getReport();
            if (report.getLatitude() != null && report.getLongitude() != null) {
                double distKm = GeoUtils.haversineKm(latitude, longitude,
                        report.getLatitude(), report.getLongitude());
                        
                boolean isDummyAccount = task.getOfficer() != null && 
                    (task.getOfficer().getEmail().equalsIgnoreCase("ahmad.fauzi@aduaja.go.id") || 
                     task.getOfficer().getEmail().equalsIgnoreCase("rizal.harahap@aduaja.go.id"));
                     
                if (distKm > 10.0 && !isDummyAccount) {
                    throw new IllegalStateException(
                        String.format("Anda berada terlalu jauh dari lokasi tugas (%.1f km). " +
                                      "Maksimum jarak yang diizinkan adalah 10 km.", distKm));
                } else if (distKm > 10.0 && isDummyAccount) {
                    System.out.println("GEOFENCING BYPASS (START TASK): Akun dummy " + task.getOfficer().getEmail() + " diizinkan mulai tugas meski di luar radius.");
                }
            }
        }

        task.setTaskStatus(TaskStatus.SEDANG_DIKERJAKAN);
        task.setStartedAt(LocalDateTime.now());
        task.setOfficerLatitude(latitude);
        task.setOfficerLongitude(longitude);
        FieldTask saved = fieldTaskRepository.save(task);
        addTaskRevision(saved, TaskStatus.BARU.name(), TaskStatus.SEDANG_DIKERJAKAN.name(),
            "Pekerjaan Dimulai", "Petugas memulai pengerjaan tugas",
            saved.getOfficer() != null ? saved.getOfficer().getUserId() : "SYSTEM");
        return saved;
    }

    @Override
    public FieldTask completeTask(String taskId) {
        return completeTask(taskId, null);
    }

    @Override
    @Transactional
    public FieldTask completeTask(String taskId, String evidencePhotoUrl) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setTaskStatus(TaskStatus.SELESAI);
        task.setCompletedAt(LocalDateTime.now());
        FieldTask savedTask = fieldTaskRepository.save(task);
        addTaskRevision(savedTask, TaskStatus.SEDANG_DIKERJAKAN.name(), TaskStatus.SELESAI.name(),
            "Selesai", "Tugas selesai dikerjakan",
            savedTask.getOfficer() != null ? savedTask.getOfficer().getUserId() : "SYSTEM");

        if (evidencePhotoUrl != null && !evidencePhotoUrl.isBlank()) {
            TaskEvidence evidence = new TaskEvidence();
            evidence.setTask(task);
            evidence.setEvidenceType(TaskEvidence.EvidenceType.SESUDAH);
            evidence.setPhotoUrl(evidencePhotoUrl);
            evidence.setTakenAt(LocalDateTime.now());
            taskEvidenceRepository.save(evidence);
        }

        // CRITICAL FIX: Update status laporan ke MENUNGGU_VALIDASI (SRS Flow 5.5)
        // dan buat ConfirmationRequest dengan deadline 72 jam
        Report report = task.getReport();
        if (report != null) {
            Report.ReportStatus oldStatus = report.getStatus();
            report.setStatus(Report.ReportStatus.MENUNGGU_VALIDASI);
            reportRepository.save(report);
            String changedBy = task.getOfficer() != null ? task.getOfficer().getUserId() : "SYSTEM";
            reportService.cascadeStatusToChildren(report.getReportId(), Report.ReportStatus.MENUNGGU_VALIDASI,
                "Status diselaraskan dengan parent", changedBy);

            // PRIORITAS #1: Buat ConfirmationRequest dulu (paling critical untuk flow)
            Optional<ConfirmationRequest> existing = confirmationRequestRepository
                    .findByReportReportId(report.getReportId());
            boolean needsNew = existing.isEmpty() ||
                    Boolean.TRUE.equals(existing.get().getIsLocked());
            if (needsNew && report.getReporter() != null) {
                existing.ifPresent(confirmationRequestRepository::delete);
                ConfirmationRequest confirmation = new ConfirmationRequest();
                confirmation.setReport(report);
                confirmation.setWarga(report.getReporter());
                confirmation.setDeadlineAt(LocalDateTime.now().plusHours(72));
                confirmation.setIsLocked(false);
                confirmationRequestRepository.save(confirmation);
            }

            // FR-ADM-16: Jika parent memiliki child tiket (merge group),
            // buat ConfirmationRequest untuk setiap child reporter juga
            List<Report> childReports = mergeRecordService.getAllChildReportsForParent(report.getReportId());
            for (Report child : childReports) {
                if (child.getReporter() != null) {
                    Optional<ConfirmationRequest> childExisting = confirmationRequestRepository
                            .findByReportReportId(child.getReportId());
                    boolean childNeedsNew = childExisting.isEmpty() ||
                            Boolean.TRUE.equals(childExisting.get().getIsLocked());
                    if (childNeedsNew) {
                        childExisting.ifPresent(confirmationRequestRepository::delete);
                        ConfirmationRequest childConfirmation = new ConfirmationRequest();
                        childConfirmation.setReport(child);
                        childConfirmation.setWarga(child.getReporter());
                        childConfirmation.setDeadlineAt(LocalDateTime.now().plusHours(72));
                        childConfirmation.setIsLocked(false);
                        confirmationRequestRepository.save(childConfirmation);
                    }
                }
                // Kirim notifikasi ke child reporter
                try {
                    if (child.getReporter() != null) {
                        notificationService.createNotification(
                            child.getReporter().getUserId(),
                            "Konfirmasi Hasil Perbaikan",
                            "Laporan terkait #" + child.getTicketNumber() + " telah selesai diperbaiki. Silakan konfirmasi hasilnya.",
                            "REPORT",
                            child.getReportId()
                        );
                    }
                } catch (Exception e) {
                    log.warn("Gagal kirim notifikasi ke child reporter {}: {}", child.getReportId(), e.getMessage());
                }
            }

            // PRIORITAS #2: Catat audit trail (tidak boleh blokir flow utama)
            try {
                reportService.addReportRevision(report, oldStatus, Report.ReportStatus.MENUNGGU_VALIDASI,
                    "Tugas selesai dikerjakan, menunggu konfirmasi warga", changedBy);
            } catch (Exception e) {
                log.warn("Gagal catat audit trail completeTask {}: {}", taskId, e.getMessage());
            }
        }

        return task;
    }

    @Override
    public FieldTask postponeTask(String taskId, String reason, String requestedById) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User requestedBy = requestedById != null ? userRepository.findById(requestedById).orElse(null) : null;
        task.setTaskStatus(TaskStatus.TERTUNDA);
        FieldTask saved = fieldTaskRepository.save(task);
        addTaskRevision(saved, TaskStatus.SEDANG_DIKERJAKAN.name(), TaskStatus.TERTUNDA.name(),
            "Ditunda", reason != null ? reason : "Ditunda oleh admin",
            requestedById != null ? requestedById : "SYSTEM");

        TaskPostponement postponement = new TaskPostponement();
        postponement.setTask(task);
        postponement.setRequestedBy(requestedBy);
        postponement.setReason(reason != null && !reason.isBlank() ? reason : "Ditunda oleh admin");
        postponement.setRequestedAt(LocalDateTime.now());
        postponement.setApprovalStatus(TaskPostponement.ApprovalStatus.DISETUJUI); // Admin langsung approve
        taskPostponementRepository.save(postponement);

        return task;
    }

    @Override
    public TaskPostponement requestPostpone(String taskId, String reason, String requestedById, LocalDateTime estimatedResumeAt) {
        // FR-PTG-27: Petugas ajukan penundaan — status tugas TIDAK langsung berubah.
        // TaskPostponement disimpan dengan ApprovalStatus.MENUNGGU.
        // Admin harus approve di dashboard admin agar tugas menjadi TERTUNDA.
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User requestedBy = requestedById != null ? userRepository.findById(requestedById).orElse(null) : null;

        TaskPostponement postponement = new TaskPostponement();
        postponement.setTask(task);
        postponement.setRequestedBy(requestedBy);
        postponement.setReason(reason != null && !reason.isBlank() ? reason : "Ditunda oleh petugas");
        postponement.setRequestedAt(LocalDateTime.now());
        postponement.setEstimatedResumeAt(estimatedResumeAt);
        postponement.setApprovalStatus(TaskPostponement.ApprovalStatus.MENUNGGU);
        TaskPostponement saved = taskPostponementRepository.save(postponement);
        addTaskRevision(task, task.getTaskStatus().name(), task.getTaskStatus().name(),
            "Pengajuan Penundaan", "Alasan: " + (reason != null ? reason : "Ditunda oleh petugas"),
            requestedById != null ? requestedById : "SYSTEM");
        return saved;
    }

    @Override
    public FieldTask resumeTask(String taskId) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        task.setTaskStatus(TaskStatus.SEDANG_DIKERJAKAN);
        FieldTask saved = fieldTaskRepository.save(task);
        addTaskRevision(saved, TaskStatus.TERTUNDA.name(), TaskStatus.SEDANG_DIKERJAKAN.name(),
            "Dilanjutkan", "Tugas dilanjutkan setelah penundaan", "SYSTEM");
        return saved;
    }

    @Override
    public FieldTask setTaskAsSedangDikerjakan(String taskId) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        FieldTask saved = fieldTaskRepository.save(task);
        addTaskRevision(saved, task.getTaskStatus().name(), TaskStatus.SEDANG_DIKERJAKAN.name(),
            "Dilanjutkan", "Status tugas dikembalikan ke sedang dikerjakan", "SYSTEM");
        return saved;
    }

    @Override
    public FieldTask setTaskAsTertunda(String taskId) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        task.setTaskStatus(TaskStatus.TERTUNDA);
        FieldTask saved = fieldTaskRepository.save(task);
        addTaskRevision(saved, TaskStatus.SEDANG_DIKERJAKAN.name(), TaskStatus.TERTUNDA.name(),
            "Ditunda", "Tugas ditunda oleh admin", "SYSTEM");
        return saved;
    }

    @Override
    public FieldTask reassignTask(String taskId, String newOfficerId) {
        log.info("[REASSIGN] Mencari taskId={}, newOfficerId={}", taskId, newOfficerId);
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        User newOfficer = userRepository.findById(newOfficerId)
                .orElseThrow(() -> new RuntimeException("New officer not found: " + newOfficerId));
        log.info("[REASSIGN] Ditemukan task status={}, officer={}", task.getTaskStatus(),
                task.getOfficer() != null ? task.getOfficer().getUserId() : "null");
        task.setOfficer(newOfficer);
        task.setTaskStatus(TaskStatus.DITUGASKAN_ULANG);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        log.info("[REASSIGN] Sebelum save — task status=DITUGASKAN_ULANG, officer={}", newOfficer.getUserId());
        FieldTask saved = fieldTaskRepository.save(task);
        log.info("[REASSIGN] Setelah save — taskId={}, status={}, officer={}",
                saved.getTaskId(), saved.getTaskStatus(),
                saved.getOfficer() != null ? saved.getOfficer().getUserId() : "null");

        // VERIFIKASI: Baca ulang task dari DB untuk memastikan perubahan tersimpan
        try {
            FieldTask verify = fieldTaskRepository.findById(taskId).orElse(null);
            if (verify != null) {
                log.info("[REASSIGN] VERIFIKASI — taskId={}, status={}, officer={}",
                    verify.getTaskId(), verify.getTaskStatus(),
                    verify.getOfficer() != null ? verify.getOfficer().getUserId() : "null");
                if (verify.getTaskStatus() != TaskStatus.DITUGASKAN_ULANG) {
                    log.error("[REASSIGN] VERIFIKASI GAGAL — status bukan DITUGASKAN_ULANG, masih={}", verify.getTaskStatus());
                }
                if (verify.getOfficer() == null || !newOfficerId.equals(verify.getOfficer().getUserId())) {
                    log.error("[REASSIGN] VERIFIKASI GAGAL — officer tidak sesuai, expected={}, actual={}",
                        newOfficerId, verify.getOfficer() != null ? verify.getOfficer().getUserId() : "null");
                }
            } else {
                log.error("[REASSIGN] VERIFIKASI GAGAL — task tidak ditemukan di DB setelah save!");
            }
        } catch (Exception e) {
            log.error("[REASSIGN] VERIFIKASI exception: {}", e.getMessage(), e);
        }

        addTaskRevision(saved, TaskStatus.DITUGASKAN_ULANG.name(), null,
            "Ditugaskan Ulang", "Tugas ditugaskan ulang ke petugas baru",
            task.getAssignedBy() != null ? task.getAssignedBy().getUserId() : "SYSTEM");

        // Kembalikan status laporan ke DITUGASKAN agar petugas bisa memulai ulang
        log.info("[REASSIGN] Akan update report status ke DITUGASKAN");
        Report report = saved.getReport();
        if (report != null) {
            log.info("[REASSIGN] Report ditemukan, set status DITUGASKAN, reportId={}", report.getReportId());
            Report.ReportStatus oldStatus = report.getStatus();
            report.setStatus(Report.ReportStatus.DITUGASKAN);
            reportRepository.save(report);
            String changedBy = task.getAssignedBy() != null ? task.getAssignedBy().getUserId() : "SYSTEM";
            reportService.addReportRevision(report, oldStatus, Report.ReportStatus.DITUGASKAN,
                "Tugas ditugaskan ulang ke petugas baru", changedBy);
            reportService.cascadeStatusToChildren(report.getReportId(), Report.ReportStatus.DITUGASKAN,
                "Status diselaraskan dengan parent", changedBy);
            log.info("[REASSIGN] Report status berhasil diupdate");
        } else {
            log.warn("[REASSIGN] Report NULL pada saved task!");
        }

        return saved;
    }

    @Override
    public long countByStatus(TaskStatus status) {
        return fieldTaskRepository.countByTaskStatus(status);
    }

    @Override
    public Optional<TaskPostponement> getLatestPostponement(String taskId) {
        List<TaskPostponement> list = taskPostponementRepository.findByTaskTaskIdOrderByRequestedAtDesc(taskId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<FieldTaskStatusRevision> getTaskRevisions(String taskId) {
        return fieldTaskStatusRevisionRepository.findByTaskTaskIdOrderByChangedAtAsc(taskId);
    }

    private void addTaskRevision(FieldTask task, String oldStatus, String newStatus,
                                  String label, String notes, String changedBy) {
        try {
            FieldTaskStatusRevision rev = new FieldTaskStatusRevision();
            rev.setTask(task);
            rev.setOldStatus(oldStatus);
            rev.setNewStatus(newStatus);
            rev.setLabel(label);
            rev.setNotes(notes);
            rev.setChangedBy(changedBy != null ? changedBy : "SYSTEM");
            rev.setChangedAt(LocalDateTime.now());
            fieldTaskStatusRevisionRepository.save(rev);
        } catch (Exception e) {
            log.warn("Gagal catat task revision untuk {}: {}", task.getTaskId(), e.getMessage());
        }
    }

    @Override
    public List<TaskEvidence> getEvidencesByTaskAndType(String taskId, TaskEvidence.EvidenceType type) {
        return taskEvidenceRepository.findByTaskTaskIdAndEvidenceType(taskId, type);
    }

    @Override
    public FieldTask closeTaskByAdmin(String taskId) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setTaskStatus(TaskStatus.SELESAI);
        task.setCompletedAt(LocalDateTime.now());
        FieldTask saved = fieldTaskRepository.save(task);
        addTaskRevision(saved, null, TaskStatus.SELESAI.name(),
            "Selesai", "Tugas ditutup oleh admin", "SYSTEM");

        Report report = task.getReport();
        if (report != null) {
            Report.ReportStatus oldStatus = report.getStatus();
            report.setStatus(Report.ReportStatus.SELESAI);
            reportRepository.save(report);
            reportService.addReportRevision(report, oldStatus, Report.ReportStatus.SELESAI,
                "Tugas ditutup oleh admin", "SYSTEM");
            reportService.cascadeStatusToChildren(report.getReportId(), Report.ReportStatus.SELESAI,
                "Status diselaraskan dengan parent", "SYSTEM");
        }

        return task;
    }

    @Override
    public void saveTaskEvidence(String taskId, String photoUrl, TaskEvidence.EvidenceType type) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // FR-PTG-21: Tambahkan watermark pada foto bukti
        // Watermark berisi: ID tiket laporan, nama petugas, koordinat GPS officer, timestamp server
        String ticketNumber = task.getReport() != null ? task.getReport().getTicketNumber() : taskId.substring(0, 8);
        String officerName  = task.getOfficer() != null ? task.getOfficer().getFullName() : "Petugas";
        BigDecimal lat = task.getOfficerLatitude();
        BigDecimal lon = task.getOfficerLongitude();
        String watermarkedPhoto = PhotoWatermarkUtil.addWatermark(
                photoUrl, ticketNumber, officerName, lat, lon, LocalDateTime.now());

        TaskEvidence evidence = new TaskEvidence();
        evidence.setTask(task);
        evidence.setEvidenceType(type);
        evidence.setPhotoUrl(watermarkedPhoto);
        evidence.setLatitude(lat);
        evidence.setLongitude(lon);
        evidence.setTakenAt(LocalDateTime.now());
        taskEvidenceRepository.save(evidence);
    }

    @Override
    public void saveTaskEvidenceDirect(String taskId, String photoUrl, TaskEvidence.EvidenceType type) {
        FieldTask task = fieldTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        TaskEvidence evidence = new TaskEvidence();
        evidence.setTask(task);
        evidence.setEvidenceType(type);
        evidence.setPhotoUrl(photoUrl);
        evidence.setLatitude(task.getOfficerLatitude());
        evidence.setLongitude(task.getOfficerLongitude());
        evidence.setTakenAt(LocalDateTime.now());
        taskEvidenceRepository.save(evidence);
    }
}
