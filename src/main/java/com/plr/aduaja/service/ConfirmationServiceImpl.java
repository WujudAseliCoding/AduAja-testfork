package com.plr.aduaja.service;

import com.plr.aduaja.model.ConfirmationRequest;
import com.plr.aduaja.model.ConfirmationRequest.ResponseType;
import com.plr.aduaja.model.MergeRecord;
import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.ConfirmationRequestRepository;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// ============================================================
// POLYMORPHISM — @Override (Run-time Polymorphism)
// ============================================================
@Service
public class ConfirmationServiceImpl implements ConfirmationService {

    @Autowired
    private ConfirmationRequestRepository confirmationRequestRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    // Notifikasi ke warga saat Selesai Otomatis
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private MergeRecordService mergeRecordService;

    private static final Logger log = LoggerFactory.getLogger(ConfirmationServiceImpl.class);

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public ConfirmationRequest createConfirmation(String reportId, String wargaId, int deadlineHours) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report tidak ditemukan: " + reportId));
        User warga = userRepository.findById(wargaId)
                .orElseThrow(() -> new RuntimeException("Warga tidak ditemukan: " + wargaId));

        ConfirmationRequest confirmation = new ConfirmationRequest();
        confirmation.setReport(report);
        confirmation.setWarga(warga);
        confirmation.setDeadlineAt(LocalDateTime.now().plusHours(deadlineHours));
        confirmation.setIsLocked(false);

        return confirmationRequestRepository.save(confirmation);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public ConfirmationRequest respond(String reportId, ResponseType response) {
        ConfirmationRequest confirmation = confirmationRequestRepository.findByReportReportId(reportId)
                .orElseThrow(() -> new RuntimeException("Confirmation tidak ditemukan untuk report: " + reportId));

        confirmation.setResponse(response);
        confirmation.setRespondedAt(LocalDateTime.now());
        confirmation.setIsLocked(true);
        confirmationRequestRepository.save(confirmation);

        Report report = confirmation.getReport();
        Report.ReportStatus oldStatus = report.getStatus();

        // Cek apakah report ini bagian dari merge group
        Report mergeParent = findMergeParent(report);
        if (mergeParent != null) {
            // --- MERGE GROUP LOGIC ---
            if (response == ResponseType.TOLAK) {
                // Satu TOLAK dari warga mana pun → parent DALAM_EVALUASI_SENGKETA
                setParentStatus(mergeParent, Report.ReportStatus.DALAM_EVALUASI_SENGKETA,
                    "Warga menolak hasil tugas (merge group)", "WARGA");
            } else if (response == ResponseType.TERIMA || response == ResponseType.TIMEOUT) {
                // Cek apakah semua konfirmasi dalam group sudah direspons
                if (allConfirmationsResolved(mergeParent)) {
                    if (anyConfirmationRejected(mergeParent)) {
                        setParentStatus(mergeParent, Report.ReportStatus.DALAM_EVALUASI_SENGKETA,
                            "Ada warga menolak hasil tugas (merge group)", "SYSTEM");
                    } else {
                        setParentStatus(mergeParent, Report.ReportStatus.SELESAI,
                            "Semua warga menerima hasil tugas (merge group)", "SYSTEM");
                    }
                }
                // else: masih menunggu warga lain, jangan ubah status parent
            }
        } else {
            // --- SINGLE REPORT LOGIC (non-merge) ---
            if (response == ResponseType.TERIMA) {
                report.setStatus(Report.ReportStatus.SELESAI);
                reportRepository.save(report);
                reportService.addReportRevision(report, oldStatus, Report.ReportStatus.SELESAI,
                    "Warga menerima hasil tugas", "WARGA");
            } else if (response == ResponseType.TOLAK) {
                report.setStatus(Report.ReportStatus.SENGKETA);
                reportRepository.save(report);
                reportService.addReportRevision(report, oldStatus, Report.ReportStatus.SENGKETA,
                    "Warga menolak hasil tugas", "WARGA");
            }
        }

        return confirmation;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<ConfirmationRequest> getByReportId(String reportId) {
        return confirmationRequestRepository.findByReportReportId(reportId);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 1 param ResponseType)
    public List<ConfirmationRequest> getRequests(ResponseType responseType) {
        return confirmationRequestRepository.findByResponse(responseType);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 1 param String)
    public List<ConfirmationRequest> getRequests(String reportId) {
        return confirmationRequestRepository.findByReportReportId(reportId)
                .map(List::of)
                .orElse(List.of());
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<ConfirmationRequest> getAllRequests() {
        return confirmationRequestRepository.findAll();
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public void processTimeouts() {
        List<ConfirmationRequest> timedOut = confirmationRequestRepository.findByDeadlineAtBeforeAndResponseIsNull(LocalDateTime.now());
        for (ConfirmationRequest confirmation : timedOut) {
            confirmation.setResponse(ResponseType.TIMEOUT);
            confirmation.setRespondedAt(LocalDateTime.now());
            confirmation.setIsLocked(true);

            Report report = confirmation.getReport();

            // Cek apakah report ini bagian dari merge group
            Report mergeParent = findMergeParent(report);
            if (mergeParent != null) {
                // Timeout pada merge group — cek apakah semua sudah selesai
                if (allConfirmationsResolved(mergeParent)) {
                    if (anyConfirmationRejected(mergeParent)) {
                        setParentStatus(mergeParent, Report.ReportStatus.DALAM_EVALUASI_SENGKETA,
                            "Ada warga menolak hasil tugas (merge group, timeout)", "SYSTEM");
                    } else {
                        setParentStatus(mergeParent, Report.ReportStatus.SELESAI_OTOMATIS,
                            "Batas waktu konfirmasi group habis, semua warga timeout atau terima", "SYSTEM");
                    }
                }
                // else: masih menunggu warga lain, jangan ubah status parent
            } else {
                // Non-merge: langsung set SELESAI_OTOMATIS
                Report.ReportStatus oldStatus = report.getStatus();
                report.setStatus(Report.ReportStatus.SELESAI_OTOMATIS);
                reportRepository.save(report);
                reportService.addReportRevision(report, oldStatus, Report.ReportStatus.SELESAI_OTOMATIS,
                    "Batas waktu konfirmasi 3x24 jam habis, laporan ditutup otomatis", "SYSTEM");
            }

            // Kirim notifikasi ke warga saat timeout
            try {
                String wargaId = report.getReporter() != null ? report.getReporter().getUserId() : null;
                String ticketNum = report.getTicketNumber() != null ? report.getTicketNumber() : report.getReportId();
                if (wargaId != null) {
                    notificationService.createNotification(
                        wargaId,
                        "Laporan Ditutup Otomatis",
                        "Laporan " + ticketNum + " telah ditutup secara otomatis karena batas waktu konfirmasi (3x24 jam kerja) telah habis tanpa respons dari Anda.",
                        "REPORT",
                        report.getReportId()
                    );
                }
            } catch (Exception ignored) {
                // Tidak boleh hentikan proses timeout jika notifikasi gagal
            }
        }
        confirmationRequestRepository.saveAll(timedOut);
    }

    // ==========================================
    // PRIVATE HELPERS — Merge Group Logic
    // ==========================================

    /**
     * Cari parent report jika report ini bagian dari merge group.
     * Report bisa jadi child (punya parentReport) atau parent (punya childMergeRecords aktif).
     */
    private Report findMergeParent(Report report) {
        // Jika report adalah child (punya parentReport yang masih aktif), kembalikan parent
        if (report.getParentReport() != null) {
            Optional<MergeRecord> activeMerge = mergeRecordService.getActiveMergeByChild(report.getReportId());
            if (activeMerge.isPresent()) {
                return activeMerge.get().getParentReport();
            }
        }
        // Jika report adalah parent (punya child aktif)
        List<MergeRecord> children = mergeRecordService.getActiveMergesByParent(report.getReportId());
        if (!children.isEmpty()) {
            return report;
        }
        return null;
    }

    /**
     * Set status parent report dan catat audit trail.
     */
    private void setParentStatus(Report parent, Report.ReportStatus newStatus, String notes, String changedBy) {
        Report.ReportStatus oldStatus = parent.getStatus();
        parent.setStatus(newStatus);
        reportRepository.save(parent);
        reportService.addReportRevision(parent, oldStatus, newStatus, notes, changedBy);
        // Cascade to children only for terminal statuses (SELESAI, SELESAI_OTOMATIS)
        // but NOT for DALAM_EVALUASI_SENGKETA — other children must still respond
        if (newStatus == Report.ReportStatus.SELESAI || newStatus == Report.ReportStatus.SELESAI_OTOMATIS) {
            reportService.cascadeStatusToChildren(parent.getReportId(), newStatus, notes, changedBy);
        }
    }

    /**
     * Cek apakah semua ConfirmationRequest dalam merge group sudah direspons.
     */
    private boolean allConfirmationsResolved(Report parent) {
        List<ConfirmationRequest> allGroupConfirmations = getAllGroupConfirmations(parent);
        return allGroupConfirmations.stream().allMatch(c -> c.getResponse() != null);
    }

    /**
     * Cek apakah ada ConfirmationRequest dalam merge group yang berisi TOLAK.
     */
    private boolean anyConfirmationRejected(Report parent) {
        List<ConfirmationRequest> allGroupConfirmations = getAllGroupConfirmations(parent);
        return allGroupConfirmations.stream().anyMatch(c -> c.getResponse() == ResponseType.TOLAK);
    }

    /**
     * Kumpulkan semua ConfirmationRequest dalam merge group (parent + semua child).
     */
    private List<ConfirmationRequest> getAllGroupConfirmations(Report parent) {
        List<ConfirmationRequest> result = new ArrayList<>();
        // ConfirmationRequest parent
        confirmationRequestRepository.findByReportReportId(parent.getReportId()).ifPresent(result::add);
        // ConfirmationRequest untuk setiap child
        List<Report> children = mergeRecordService.getAllChildReportsForParent(parent.getReportId());
        for (Report child : children) {
            confirmationRequestRepository.findByReportReportId(child.getReportId()).ifPresent(result::add);
        }
        return result;
    }
}
