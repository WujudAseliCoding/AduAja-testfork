package com.plr.aduaja.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// ============================================================
// INHERITANCE (Pewarisan): ReportRevision extends BaseEntity
// Mendapatkan createdAt dan updatedAt otomatis dari parent
// ============================================================
@Entity
@Table(name = "report_revisions")
@JsonIgnoreProperties({"report"})
public class ReportRevision extends BaseEntity {  // ← INHERITANCE sejati

    // ENKAPSULASI: semua field PRIVATE
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "revision_id")
    private String revisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;  // HAS-A (Composition)

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    // === Fields lama untuk backward compatibility ===
    @Column(name = "revised_description", columnDefinition = "TEXT")
    private String revisedDescription;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // ENKAPSULASI: Hanya getter & setter
    public String getRevisionId() { return revisionId; }
    public void setRevisionId(String revisionId) { this.revisionId = revisionId; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getRevisedDescription() { return revisedDescription; }
    public void setRevisedDescription(String revisedDescription) { this.revisedDescription = revisedDescription; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
