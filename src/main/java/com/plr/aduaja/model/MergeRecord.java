package com.plr.aduaja.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@JsonIgnoreProperties({"parentReport", "childReport"})
@Table(name = "merge_records")
public class MergeRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "merge_id")
    private String mergeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_report_id", nullable = false)
    private Report parentReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_report_id", nullable = false)
    private Report childReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_by", nullable = false)
    private User mergedBy;

    @Column(name = "merge_reason", columnDefinition = "TEXT")
    private String mergeReason;

    @Column(name = "previous_child_status", length = 50)
    private String previousChildStatus;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "merged_at", nullable = false)
    private LocalDateTime mergedAt = LocalDateTime.now();

    public String getMergeId() { return mergeId; }
    public void setMergeId(String mergeId) { this.mergeId = mergeId; }

    public Report getParentReport() { return parentReport; }
    public void setParentReport(Report parentReport) { this.parentReport = parentReport; }

    public Report getChildReport() { return childReport; }
    public void setChildReport(Report childReport) { this.childReport = childReport; }

    public User getMergedBy() { return mergedBy; }
    public void setMergedBy(User mergedBy) { this.mergedBy = mergedBy; }

    public String getMergeReason() { return mergeReason; }
    public void setMergeReason(String mergeReason) { this.mergeReason = mergeReason; }

    public String getPreviousChildStatus() { return previousChildStatus; }
    public void setPreviousChildStatus(String previousChildStatus) { this.previousChildStatus = previousChildStatus; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getMergedAt() { return mergedAt; }
    public void setMergedAt(LocalDateTime mergedAt) { this.mergedAt = mergedAt; }
}
