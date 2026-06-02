package com.plr.aduaja.service;

import com.plr.aduaja.dto.DisputeDTO;
import com.plr.aduaja.model.ConfirmationRequest;
import com.plr.aduaja.model.DisputeRecord;
import com.plr.aduaja.model.DisputeRecord.ResolutionType;
import com.plr.aduaja.model.MergeRecord;
import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.ConfirmationRequestRepository;
import com.plr.aduaja.repository.DisputeRecordRepository;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ============================================================
// POLYMORPHISM — @Override (Run-time Polymorphism)
// ============================================================
@Service
public class DisputeServiceImpl implements DisputeService {

    @Autowired
    private DisputeRecordRepository disputeRecordRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfirmationRequestRepository confirmationRequestRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private MergeRecordService mergeRecordService;

    private static final Logger log = LoggerFactory.getLogger(DisputeServiceImpl.class);

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public DisputeRecord createDispute(DisputeDTO dto, String disputantId) {
        // FR-RSL-10: Validasi alasan sengketa wajib diisi
        if (dto.getReason() == null || dto.getReason().isBlank()) {
            throw new IllegalArgumentException("Alasan sengketa wajib diisi.");
        }
        // FR-RSL-10: Validasi foto bukti wajib dilampirkan
        if (dto.getEvidencePhotoUrl() == null || dto.getEvidencePhotoUrl().isBlank()) {
            throw new IllegalArgumentException("Foto bukti sengketa wajib dilampirkan.");
        }

        Report report = reportRepository.findById(dto.getReportId())
                .orElseThrow(() -> new RuntimeException("Report tidak ditemukan: " + dto.getReportId()));
        User filedBy = userRepository.findById(disputantId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan: " + disputantId));

        // Sengketa hanya bisa diajukan saat status MENUNGGU_VALIDASI
        if (report.getStatus() != Report.ReportStatus.MENUNGGU_VALIDASI) {
            throw new IllegalStateException("Sengketa hanya dapat diajukan saat laporan berstatus 'Menunggu Konfirmasi Warga'.");
        }

        // FR-RSL-11: Maksimal 1 sengketa per tiket (cek langsung & via parent untuk merge group)
        if (disputeRecordRepository.findByReportReportId(dto.getReportId()).isPresent()) {
            throw new IllegalStateException("Sengketa untuk laporan ini sudah pernah diajukan. Maksimal 1 kali pengajuan sengketa per tiket.");
        }
        Report mergeParent = findMergeParent(report);
        if (mergeParent != null && !mergeParent.getReportId().equals(dto.getReportId())
                && disputeRecordRepository.findByReportReportId(mergeParent.getReportId()).isPresent()) {
            throw new IllegalStateException("Sengketa untuk grup laporan ini sudah pernah diajukan. Maksimal 1 kali pengajuan sengketa per grup.");
        }

        // Jika ada ConfirmationRequest yang belum dikunci, tandai sebagai TOLAK dan kunci
        confirmationRequestRepository.findByReportReportId(dto.getReportId()).ifPresent(conf -> {
            if (!Boolean.TRUE.equals(conf.getIsLocked())) {
                conf.setResponse(ConfirmationRequest.ResponseType.TOLAK);
                conf.setRespondedAt(LocalDateTime.now());
                conf.setIsLocked(true);
                confirmationRequestRepository.save(conf);
            }
        });

        DisputeRecord dispute = new DisputeRecord();
        // Untuk merge group, tautkan DisputeRecord ke PARENT agar terlihat di admin dinas (parent punya disposisi)
        dispute.setReport(mergeParent != null ? mergeParent : report);
        dispute.setFiledBy(filedBy);
        dispute.setReasonText(dto.getReason());
        dispute.setEvidencePhotoUrl(dto.getEvidencePhotoUrl() != null ? dto.getEvidencePhotoUrl() : "");
        dispute.setFiledAt(LocalDateTime.now());

        Report.ReportStatus oldStatus = report.getStatus();

        if (mergeParent != null) {
            // Merge group: sengketa pada child → parent jadi DALAM_EVALUASI_SENGKETA
            Report.ReportStatus mergeParentOldStatus = mergeParent.getStatus();
            mergeParent.setStatus(Report.ReportStatus.DALAM_EVALUASI_SENGKETA);
            reportRepository.save(mergeParent);
            reportService.addReportRevision(mergeParent, mergeParentOldStatus, Report.ReportStatus.DALAM_EVALUASI_SENGKETA,
                "Sengketa diajukan oleh warga pada child report (merge group)", disputantId);
            reportService.cascadeStatusToChildren(mergeParent.getReportId(), Report.ReportStatus.DALAM_EVALUASI_SENGKETA,
                "Status diselaraskan dengan parent", disputantId);
            log.info("Merge group dispute: parent {} set to DALAM_EVALUASI_SENGKETA due to child {} dispute",
                mergeParent.getReportId(), report.getReportId());
        } else {
            // Non-merge: normal flow
            report.setStatus(Report.ReportStatus.SENGKETA);
            reportRepository.save(report);
            reportService.addReportRevision(report, oldStatus, Report.ReportStatus.SENGKETA,
                "Sengketa diajukan oleh warga", disputantId);
        }

        return disputeRecordRepository.save(dispute);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public DisputeRecord resolveDispute(String disputeId, ResolutionType resolution, String adminId, String resolutionNotes) {
        DisputeRecord dispute = disputeRecordRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Sengketa tidak ditemukan: " + disputeId));

        if (dispute.getResolution() != null) {
            throw new IllegalStateException("Sengketa ini sudah pernah diputus sebelumnya.");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan: " + adminId));

        dispute.setResolution(resolution);
        dispute.setResolvedBy(admin);
        dispute.setResolutionNotes(resolutionNotes);
        dispute.setResolvedAt(LocalDateTime.now());

        Report report = dispute.getReport();
        Report.ReportStatus oldStatus = report.getStatus();
        Report.ReportStatus targetStatus;
        if (resolution == ResolutionType.TUGASKAN_KEMBALI) {
            targetStatus = Report.ReportStatus.DITUGASKAN;
        } else {
            targetStatus = Report.ReportStatus.SELESAI;
        }

        report.setStatus(targetStatus);
        reportRepository.save(report);
        reportService.addReportRevision(report, oldStatus, targetStatus,
            resolutionNotes, adminId);

        // Cascade status ke child tickets dalam merge group
        reportService.cascadeStatusToChildren(report.getReportId(), targetStatus, resolutionNotes, adminId);

        return disputeRecordRepository.save(dispute);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<DisputeRecord> getDisputeById(String disputeId) {
        return disputeRecordRepository.findById(disputeId);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 1 param String)
    public List<DisputeRecord> getDisputes(String reportId) {
        Optional<DisputeRecord> direct = disputeRecordRepository.findByReportReportId(reportId);
        if (direct.isPresent()) {
            return direct.map(List::of).orElse(List.of());
        }
        // Cek apakah report ini child dari merge group → cari sengketa di parent
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report != null && report.getParentReport() != null) {
            Optional<MergeRecord> activeMerge = mergeRecordService.getActiveMergeByChild(reportId);
            if (activeMerge.isPresent()) {
                return disputeRecordRepository.findByReportReportId(activeMerge.get().getParentReport().getReportId())
                        .map(List::of).orElse(List.of());
            }
        }
        return List.of();
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 1 param ResolutionType)
    public List<DisputeRecord> getDisputes(ResolutionType resolution) {
        if (resolution == null) {
            return disputeRecordRepository.findByResolutionIsNull();
        }
        return disputeRecordRepository.findByResolution(resolution);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<DisputeRecord> getAllDisputes() {
        return disputeRecordRepository.findAll();
    }

    @Override
    public List<DisputeRecord> getPendingDisputes() {
        return disputeRecordRepository.findByResolutionIsNull();
    }

    /**
     * Cari parent report jika report ini bagian dari merge group.
     */
    private Report findMergeParent(Report report) {
        if (report.getParentReport() != null) {
            Optional<MergeRecord> activeMerge = mergeRecordService.getActiveMergeByChild(report.getReportId());
            if (activeMerge.isPresent()) {
                return activeMerge.get().getParentReport();
            }
        }
        List<MergeRecord> children = mergeRecordService.getActiveMergesByParent(report.getReportId());
        if (!children.isEmpty()) {
            return report;
        }
        return null;
    }
}
