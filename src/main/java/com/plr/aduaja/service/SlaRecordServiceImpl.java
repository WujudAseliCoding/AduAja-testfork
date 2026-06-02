package com.plr.aduaja.service;

import com.plr.aduaja.model.*;
import com.plr.aduaja.model.SlaRecord.SlaStatus;
import com.plr.aduaja.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ============================================================
// POLYMORPHISM — Run-time Polymorphism via @Override dari Interface
// ENCAPSULATION — semua field private, di-inject lewat Interface
// ============================================================
@Service
public class SlaRecordServiceImpl implements SlaRecordService {

    @Autowired
    private SlaRecordRepository slaRecordRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SlaPauseLogRepository slaPauseLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<SlaRecord> findById(String id) {
        return slaRecordRepository.findById(id);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<SlaRecord> findByReportId(String reportId) {
        return slaRecordRepository.findByReportReportId(reportId);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<SlaRecord> getAllRecords() {
        return slaRecordRepository.findAll();
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 1 parameter)
    public List<SlaRecord> getRecords(SlaStatus status) {
        return slaRecordRepository.findByCurrentStatus(status);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (OVERLOAD — 2 parameter)
    public List<SlaRecord> getRecords(LocalDateTime start, LocalDateTime end) {
        return slaRecordRepository.findByCreatedAtBetween(start, end);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public SlaRecord createSlaRecord(String reportId, Integer durationHours) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report tidak ditemukan: " + reportId));

        SlaRecord sla = new SlaRecord();
        sla.setReport(report);
        sla.setSlaStartAt(LocalDateTime.now());

        // Coba ambil dari category jika durationHours null
        if (durationHours == null) {
            ReportCategory category = report.getCategory();
            if (category != null && category.getSlaDurationHours() != null) {
                durationHours = category.getSlaDurationHours();
            } else {
                durationHours = 48;
            }
        }
        sla.setSlaDeadlineAt(LocalDateTime.now().plusHours(durationHours));
        sla.setTotalPausedMinutes(0);
        sla.setCurrentStatus(SlaStatus.BERJALAN);

        return slaRecordRepository.save(sla);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public SlaRecord pauseSla(String slaId, String reason, String pausedByUserId) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
                .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan: " + slaId));
        sla.setCurrentStatus(SlaStatus.TERTUNDA);
        slaRecordRepository.save(sla);

        SlaPauseLog log = new SlaPauseLog();
        log.setSlaRecord(sla);
        User pausedBy = userRepository.findById(pausedByUserId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan: " + pausedByUserId));
        log.setPausedBy(pausedBy);
        log.setPauseReason(reason != null ? reason : "Jeda oleh admin dinas");
        log.setPausedAt(LocalDateTime.now());
        slaPauseLogRepository.save(log);

        return sla;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public SlaRecord resumeSla(String slaId) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
                .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan: " + slaId));

        List<SlaPauseLog> openLogs = slaPauseLogRepository.findBySlaRecordSlaId(slaId).stream()
                .filter(log -> log.getResumedAt() == null)
                .collect(java.util.stream.Collectors.toList());

        for (SlaPauseLog log : openLogs) {
            log.setResumedAt(LocalDateTime.now());
            long pausedMinutes = Duration.between(log.getPausedAt(), LocalDateTime.now()).toMinutes();
            log.setPausedDurationMinutes((int) pausedMinutes);
            slaPauseLogRepository.save(log);

            sla.setTotalPausedMinutes(sla.getTotalPausedMinutes() + (int) pausedMinutes);
            if (sla.getSlaDeadlineAt() != null) {
                sla.setSlaDeadlineAt(sla.getSlaDeadlineAt().plusMinutes(pausedMinutes));
            }
        }

        sla.setCurrentStatus(SlaStatus.BERJALAN);
        return slaRecordRepository.save(sla);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public SlaRecord completeSla(String slaId) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
                .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan: " + slaId));
        sla.setCurrentStatus(SlaStatus.SELESAI);
        sla.setCompletedAt(LocalDateTime.now());
        return slaRecordRepository.save(sla);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public SlaRecord markOverdueReviewed(String slaId, String notes) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
                .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan: " + slaId));
        sla.setOverdueReviewed(true);
        sla.setOverdueReviewNotes(notes != null ? notes.trim() : "Ditinjau oleh admin");
        sla.setOverdueReviewedAt(LocalDateTime.now());
        return slaRecordRepository.save(sla);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    @Transactional
    public void checkAndUpdateOverdueSla() {
        List<SlaRecord> activeSlas = slaRecordRepository.findByCurrentStatus(SlaStatus.BERJALAN);
        LocalDateTime now = LocalDateTime.now();
        for (SlaRecord sla : activeSlas) {
            if (now.isAfter(sla.getSlaDeadlineAt())) {
                sla.setCurrentStatus(SlaStatus.TERLAMBAT);
                slaRecordRepository.save(sla);
            }
        }
    }
}
