package com.plr.aduaja.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@JsonIgnoreProperties({"report"})
@Table(name = "field_tasks")
public class FieldTask extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "task_id")
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private User officer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_id")
    private SlaRecord slaRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false)
    private TaskStatus taskStatus = TaskStatus.BARU;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "officer_latitude", precision = 10, scale = 8)
    private BigDecimal officerLatitude;

    @Column(name = "officer_longitude", precision = 11, scale = 8)
    private BigDecimal officerLongitude;

    public enum TaskStatus {
        BARU, SEDANG_DIKERJAKAN, TERTUNDA, SELESAI, DITUGASKAN_ULANG
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public User getOfficer() { return officer; }
    public void setOfficer(User officer) { this.officer = officer; }

    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }

    public SlaRecord getSlaRecord() { return slaRecord; }
    public void setSlaRecord(SlaRecord slaRecord) { this.slaRecord = slaRecord; }

    public TaskStatus getTaskStatus() { return taskStatus; }
    public void setTaskStatus(TaskStatus taskStatus) { this.taskStatus = taskStatus; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public BigDecimal getOfficerLatitude() { return officerLatitude; }
    public void setOfficerLatitude(BigDecimal officerLatitude) { this.officerLatitude = officerLatitude; }

    public BigDecimal getOfficerLongitude() { return officerLongitude; }
    public void setOfficerLongitude(BigDecimal officerLongitude) { this.officerLongitude = officerLongitude; }
}
