package com.plr.aduaja.service;

import com.plr.aduaja.dto.MergeDTO;
import com.plr.aduaja.model.MergeRecord;
import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.MergeRecordRepository;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.UserRepository;
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
public class MergeRecordServiceImpl implements MergeRecordService {

    @Autowired
    private MergeRecordRepository mergeRecordRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private NotificationService notificationService;

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public MergeRecord createMerge(MergeDTO dto, String userId) {
        Report parent = reportRepository.findById(dto.getPrimaryReportId())
                .orElseThrow(() -> new RuntimeException("Primary report tidak ditemukan: " + dto.getPrimaryReportId()));
        Report child = reportRepository.findById(dto.getMergedReportId())
                .orElseThrow(() -> new RuntimeException("Merged report tidak ditemukan: " + dto.getMergedReportId()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan: " + userId));

        Report.ReportStatus oldStatus = child.getStatus();

        MergeRecord merge = new MergeRecord();
        merge.setParentReport(parent);
        merge.setChildReport(child);
        merge.setMergedBy(user);
        merge.setMergeReason(dto.getReason());
        merge.setIsActive(true);
        merge.setMergedAt(LocalDateTime.now());
        merge.setPreviousChildStatus(oldStatus != null ? oldStatus.name() : null);

        child.setParentReport(parent);
        child.setStatus(Report.ReportStatus.TERGABUNG);
        reportRepository.save(child);
        reportService.addReportRevision(child, oldStatus, Report.ReportStatus.TERGABUNG,
            "Laporan digabungkan ke laporan utama", userId);

        MergeRecord saved = mergeRecordRepository.save(merge);

        // Kirim notifikasi ke reporter child bahwa laporannya digabung
        if (child.getReporter() != null && child.getReporter().getUserId() != null) {
            try {
                notificationService.createNotification(
                    child.getReporter().getUserId(),
                    "Laporan Digabungkan",
                    "Laporan Anda (" + child.getTicketNumber() + ") telah digabungkan ke laporan utama " + parent.getTicketNumber() + ". Status laporan: Tergabung.",
                    "REPORT",
                    parent.getReportId()
                );
            } catch (Exception e) {
                // notifikasi gagal tidak boleh menghentikan proses merge
            }
        }

        // Juga kirim notifikasi ke reporter parent jika berbeda
        if (parent.getReporter() != null && parent.getReporter().getUserId() != null
                && !parent.getReporter().getUserId().equals(child.getReporter() != null ? child.getReporter().getUserId() : null)) {
            try {
                notificationService.createNotification(
                    parent.getReporter().getUserId(),
                    "Laporan Baru Digabungkan ke Tiket Anda",
                    "Laporan " + child.getTicketNumber() + " telah digabungkan ke laporan utama Anda (" + parent.getTicketNumber() + ").",
                    "REPORT",
                    parent.getReportId()
                );
            } catch (Exception e) {
                // notifikasi gagal tidak boleh menghentikan proses merge
            }
        }

        return saved;
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — tanpa parameter)
    public List<MergeRecord> getMerges() {
        return mergeRecordRepository.findAll();
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 1 parameter)
    public List<MergeRecord> getMerges(String reportId) {
        List<MergeRecord> primaryMerges = mergeRecordRepository.findByParentReportReportId(reportId);
        List<MergeRecord> childMerges = mergeRecordRepository.findByChildReportReportId(reportId);
        List<MergeRecord> all = new ArrayList<>(primaryMerges);
        all.addAll(childMerges);
        return all;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public void cancelMerge(String mergeId) {
        MergeRecord record = mergeRecordRepository.findById(mergeId)
                .orElseThrow(() -> new RuntimeException("Merge record tidak ditemukan: " + mergeId));
        record.setIsActive(false);

        Report child = record.getChildReport();

        // Kembalikan child ke status sebelum merge
        String prevStatus = record.getPreviousChildStatus();
        if (prevStatus != null && !prevStatus.isBlank()) {
            try {
                Report.ReportStatus restoredStatus = Report.ReportStatus.valueOf(prevStatus);
                Report.ReportStatus oldStatus = child.getStatus();
                child.setStatus(restoredStatus);
                reportService.addReportRevision(child, oldStatus, restoredStatus,
                    "Laporan dipisahkan dari merge (unmerge), status dikembalikan", "SYSTEM");
            } catch (IllegalArgumentException e) {
                // fallback: jika status tidak valid, biarkan TERGABUNG atau ke MENUNGGU_VERIFIKASI
                child.setStatus(Report.ReportStatus.MENUNGGU_VERIFIKASI);
            }
        } else {
            child.setStatus(Report.ReportStatus.MENUNGGU_VERIFIKASI);
        }

        child.setParentReport(null);
        reportRepository.save(child);

        mergeRecordRepository.save(record);
    }

    @Override
    public Optional<MergeRecord> getActiveMergeByChild(String childReportId) {
        return mergeRecordRepository.findByChildReportReportIdAndIsActiveTrue(childReportId);
    }

    @Override
    public List<MergeRecord> getActiveMergesByParent(String parentReportId) {
        return mergeRecordRepository.findByParentReportReportIdAndIsActiveTrue(parentReportId);
    }

    @Override
    public List<Report> getAllChildReportsForParent(String parentReportId) {
        return mergeRecordRepository.findByParentReportReportIdAndIsActiveTrue(parentReportId)
                .stream()
                .map(MergeRecord::getChildReport)
                .collect(Collectors.toList());
    }
}
