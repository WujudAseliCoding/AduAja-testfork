package com.plr.aduaja.service;

import com.plr.aduaja.model.FieldTask;
import com.plr.aduaja.model.FieldTask.TaskStatus;
import com.plr.aduaja.model.FieldTaskStatusRevision;
import com.plr.aduaja.model.TaskEvidence;
import com.plr.aduaja.model.TaskPostponement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FieldTaskService {

    List<FieldTask> getAllTasks();

    Optional<FieldTask> getTaskById(String taskId);

    List<FieldTask> getTasksByOfficer(String officerId);

    List<FieldTask> getTasksByOfficerAndStatus(String officerId, TaskStatus status);

    List<FieldTask> getTasksByStatus(TaskStatus status);

    List<FieldTask> getTasksByReport(String reportId);

    List<FieldTask> getTasksByDateRange(LocalDateTime start, LocalDateTime end);

    FieldTask createTask(String reportId, String officerId, String assignedById);

    FieldTask startTask(String taskId, BigDecimal latitude, BigDecimal longitude);

    FieldTask completeTask(String taskId);

    FieldTask completeTask(String taskId, String evidencePhotoUrl);

    /**
     * Langsung set tugas menjadi TERTUNDA (digunakan oleh admin).
     */
    FieldTask postponeTask(String taskId, String reason, String requestedById);

    /**
     * Ajukan permintaan penundaan oleh petugas (FR-PTG-27).
     * Status tugas TIDAK langsung berubah — tetap SEDANG_DIKERJAKAN.
     * TaskPostponement dibuat dengan ApprovalStatus.MENUNGGU.
     * Admin harus approve agar status tugas berubah ke TERTUNDA.
     */
    TaskPostponement requestPostpone(String taskId, String reason, String requestedById, LocalDateTime estimatedResumeAt);

    /**
     * Ubah status tugas menjadi SEDANG_DIKERJAKAN tanpa side effect.
     * Tidak mengubah startedAt atau koordinat petugas.
     */
    FieldTask resumeTask(String taskId);

    /**
     * Ubah status tugas menjadi SEDANG_DIKERJAKAN tanpa side effect.
     */
    FieldTask setTaskAsSedangDikerjakan(String taskId);

    /**
     * Ubah status tugas menjadi TERTUNDA tanpa membuat TaskPostponement baru.
     * Digunakan saat admin menyetujui penundaan yang sudah ada (approvePostponement).
     */
    FieldTask setTaskAsTertunda(String taskId);

    FieldTask reassignTask(String taskId, String newOfficerId);

    long countByStatus(TaskStatus status);

    Optional<TaskPostponement> getLatestPostponement(String taskId);

    void saveTaskEvidence(String taskId, String photoUrl, TaskEvidence.EvidenceType type);

    void saveTaskEvidenceDirect(String taskId, String photoUrl, TaskEvidence.EvidenceType type);

    List<TaskEvidence> getEvidencesByTaskAndType(String taskId, TaskEvidence.EvidenceType type);

    FieldTask closeTaskByAdmin(String taskId);

    List<FieldTaskStatusRevision> getTaskRevisions(String taskId);
}
