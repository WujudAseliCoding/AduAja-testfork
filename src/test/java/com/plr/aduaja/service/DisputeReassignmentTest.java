package com.plr.aduaja.service;

import com.plr.aduaja.model.*;
import com.plr.aduaja.model.FieldTask.TaskStatus;
import com.plr.aduaja.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DisputeReassignmentTest {

    @Autowired private FieldTaskService fieldTaskService;
    @Autowired private DisputeService disputeService;
    @Autowired private NotificationService notificationService;
    @Autowired private UserRepository userRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private DisputeRecordRepository disputeRecordRepository;
    @Autowired private FieldTaskRepository fieldTaskRepository;

    private Report report;
    private User petugasLama;
    private User petugasBaru;
    private User admin;
    private FieldTask task;

    @BeforeEach
    void setUp() {
        fieldTaskRepository.deleteAll();
        disputeRecordRepository.deleteAll();
        reportRepository.deleteAll();
        userRepository.deleteAll();

        admin = new User();
        admin.setEmail("admin.test@aduaja.go.id");
        admin.setFullName("Admin Test");
        admin.setRole(User.Role.ADMIN_DINAS);
        admin.setPasswordHash("pass");
        userRepository.save(admin);

        petugasLama = new User();
        petugasLama.setEmail("petugas.lama@aduaja.go.id");
        petugasLama.setFullName("Petugas Lama");
        petugasLama.setRole(User.Role.PETUGAS);
        petugasLama.setPasswordHash("pass");
        userRepository.save(petugasLama);

        petugasBaru = new User();
        petugasBaru.setEmail("petugas.baru@aduaja.go.id");
        petugasBaru.setFullName("Petugas Baru");
        petugasBaru.setRole(User.Role.PETUGAS);
        petugasBaru.setPasswordHash("pass");
        userRepository.save(petugasBaru);

        report = new Report();
        report.setTicketNumber("TKT-" + (System.currentTimeMillis() % 100000000));
        report.setDescription("Laporan Test Sengketa");
        report.setStatus(Report.ReportStatus.DITUGASKAN);
        report.setSubmittedAt(LocalDateTime.now());
        report.setReporter(petugasLama);
        reportRepository.save(report);

        task = new FieldTask();
        task.setReport(report);
        task.setOfficer(petugasLama);
        task.setAssignedBy(admin);
        task.setTaskStatus(TaskStatus.SELESAI);
        task.setStartedAt(LocalDateTime.now().minusHours(2));
        task.setCompletedAt(LocalDateTime.now());
        fieldTaskRepository.save(task);

        report.setStatus(Report.ReportStatus.MENUNGGU_VALIDASI);
        reportRepository.save(report);
    }

    @Test
    void testReassignTaskUpdatesOfficer() {
        FieldTask updated = fieldTaskService.reassignTask(task.getTaskId(), petugasBaru.getUserId());

        assertNotNull(updated);
        assertEquals(petugasBaru.getUserId(), updated.getOfficer().getUserId());
        assertEquals(TaskStatus.DITUGASKAN_ULANG, updated.getTaskStatus());
        assertNull(updated.getStartedAt());
        assertNull(updated.getCompletedAt());
    }

    @Test
    void testReassignTaskUpdatesReportStatus() {
        fieldTaskService.reassignTask(task.getTaskId(), petugasBaru.getUserId());

        Report refreshed = reportRepository.findById(report.getReportId()).orElseThrow();
        assertEquals(Report.ReportStatus.DITUGASKAN, refreshed.getStatus());
    }

    @Test
    void testTaskAppearsForNewOfficer() {
        fieldTaskService.reassignTask(task.getTaskId(), petugasBaru.getUserId());

        List<FieldTask> baruTasks = fieldTaskService.getTasksByOfficer(petugasBaru.getUserId());
        boolean found = baruTasks.stream()
                .anyMatch(t -> t.getTaskId().equals(task.getTaskId())
                        && t.getTaskStatus() == TaskStatus.DITUGASKAN_ULANG);

        assertTrue(found, "Reassigned task harus muncul di daftar tugas petugas baru");
    }

    @Test
    void testTaskDoesNotAppearForOldOfficer() {
        fieldTaskService.reassignTask(task.getTaskId(), petugasBaru.getUserId());

        List<FieldTask> oldTasks = fieldTaskService.getTasksByOfficer(petugasLama.getUserId());
        boolean found = oldTasks.stream()
                .anyMatch(t -> t.getTaskId().equals(task.getTaskId()));

        assertFalse(found, "Task yang sudah direassign tidak boleh muncul di petugas lama");
    }

    @Test
    void testResolveDisputeWithTugaskanKembali() {
        DisputeRecord dispute = new DisputeRecord();
        dispute.setReport(report);
        dispute.setFiledBy(petugasLama);
        dispute.setReasonText("Test sengketa");
        dispute.setEvidencePhotoUrl("http://example.com/photo.jpg");
        dispute.setFiledAt(LocalDateTime.now());
        disputeRecordRepository.save(dispute);

        String disputeId = dispute.getDisputeId();
        disputeService.resolveDispute(disputeId, DisputeRecord.ResolutionType.TUGASKAN_KEMBALI,
                admin.getUserId(), "Test catatan");

        DisputeRecord resolved = disputeService.getDisputeById(disputeId).orElseThrow();
        assertNotNull(resolved.getResolution());
        assertEquals(DisputeRecord.ResolutionType.TUGASKAN_KEMBALI, resolved.getResolution());
    }

    @Test
    void testFullSengketaFlow() {
        DisputeRecord dispute = new DisputeRecord();
        dispute.setReport(report);
        dispute.setFiledBy(petugasLama);
        dispute.setReasonText("Test sengketa full flow");
        dispute.setEvidencePhotoUrl("http://example.com/photo.jpg");
        dispute.setFiledAt(LocalDateTime.now());
        disputeRecordRepository.save(dispute);

        String disputeId = dispute.getDisputeId();
        disputeService.resolveDispute(disputeId, DisputeRecord.ResolutionType.TUGASKAN_KEMBALI,
                admin.getUserId(), "Catatan perbaikan");

        String reportId = report.getReportId();
        List<FieldTask> relatedTasks = fieldTaskService.getTasksByReport(reportId);
        assertFalse(relatedTasks.isEmpty(), "Report harus memiliki FieldTask");

        for (FieldTask t : relatedTasks) {
            FieldTask updated = fieldTaskService.reassignTask(t.getTaskId(), petugasBaru.getUserId());
            assertEquals(petugasBaru.getUserId(), updated.getOfficer().getUserId());
        }

        List<FieldTask> petugasBaruTasks = fieldTaskService.getTasksByOfficer(petugasBaru.getUserId());
        boolean found = petugasBaruTasks.stream()
                .anyMatch(t -> t.getTaskStatus() == TaskStatus.DITUGASKAN_ULANG);
        assertTrue(found, "Petugas baru harus melihat task dengan status DITUGASKAN_ULANG");
    }

    @Test
    void testResolveDisputeGuardPreventsReResolution() {
        DisputeRecord dispute = new DisputeRecord();
        dispute.setReport(report);
        dispute.setFiledBy(petugasLama);
        dispute.setReasonText("Test sengketa");
        dispute.setEvidencePhotoUrl("http://example.com/photo.jpg");
        dispute.setFiledAt(LocalDateTime.now());
        disputeRecordRepository.save(dispute);

        String disputeId = dispute.getDisputeId();
        disputeService.resolveDispute(disputeId, DisputeRecord.ResolutionType.TUGASKAN_KEMBALI,
                admin.getUserId(), "Pertama");

        assertThrows(IllegalStateException.class, () ->
            disputeService.resolveDispute(disputeId, DisputeRecord.ResolutionType.TUTUP_LAPORAN,
                    admin.getUserId(), "Kedua")
        );
    }
}
