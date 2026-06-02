package com.plr.aduaja.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ============================================================
// INHERITANCE (Pewarisan): Report extends BaseEntity
// Mendapatkan createdAt dan updatedAt otomatis dari parent
// ============================================================
@Entity
@Table(name = "reports")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "parentReport"})
public class Report extends BaseEntity {  // ← INHERITANCE sejati

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id")
    private String reportId;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 20)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ReportCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "location_hint", length = 255)
    private String locationHint;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "photo_base64", columnDefinition = "TEXT")
    private String photoBase64;

    @Column(name = "photo_taken_at")
    private LocalDateTime photoTakenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.MENUNGGU_VERIFIKASI;

    // ENKAPSULASI: field catatan admin (private)
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "priority", length = 20)
    private String priority;  // Rendah / Sedang / Tinggi / Kritis — diisi admin pusat saat disposisi

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_report_id")
    private Report parentReport;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    // Flag untuk mencegah koreksi koordinat lebih dari 1 kali
    @Column(name = "coordinate_corrected", nullable = false, columnDefinition = "boolean default false")
    private boolean coordinateCorrected = false;


    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    private List<ReportRevision> revisions = new ArrayList<>();

    @OneToOne(mappedBy = "report", cascade = CascadeType.ALL)
    private SlaRecord slaRecord;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    private List<ValidationDecision> validationDecisions = new ArrayList<>();

    @OneToMany(mappedBy = "parentReport")
    private List<MergeRecord> childMergeRecords = new ArrayList<>();

    @OneToMany(mappedBy = "childReport")
    private List<MergeRecord> parentMergeRecords = new ArrayList<>();

    @OneToOne(mappedBy = "report", cascade = CascadeType.ALL)
    private Disposition disposition;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    private List<FieldTask> fieldTasks = new ArrayList<>();

    @OneToOne(mappedBy = "report", cascade = CascadeType.ALL)
    private ConfirmationRequest confirmationRequest;

    @OneToOne(mappedBy = "report", cascade = CascadeType.ALL)
    private DisputeRecord disputeRecord;

    public enum ReportStatus {
        MENUNGGU_VERIFIKASI, DITOLAK, MENUNGGU_REVISI, DITERIMA, TERGABUNG,
        DALAM_PENINJAUAN, DITUGASKAN, SEDANG_BERJALAN, TERTUNDA, TERLAMBAT,
        MENUNGGU_VALIDASI, SENGKETA, DALAM_EVALUASI_SENGKETA, SELESAI_OTOMATIS, SELESAI
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public ReportCategory getCategory() { return category; }
    public void setCategory(ReportCategory category) { this.category = category; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocationHint() { return locationHint; }
    public void setLocationHint(String locationHint) { this.locationHint = locationHint; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public LocalDateTime getPhotoTakenAt() { return photoTakenAt; }
    public void setPhotoTakenAt(LocalDateTime photoTakenAt) { this.photoTakenAt = photoTakenAt; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Report getParentReport() { return parentReport; }
    public void setParentReport(Report parentReport) { this.parentReport = parentReport; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public boolean isCoordinateCorrected() { return coordinateCorrected; }
    public void setCoordinateCorrected(boolean coordinateCorrected) { this.coordinateCorrected = coordinateCorrected; }

    public List<ReportRevision> getRevisions() { return revisions; }
    public void setRevisions(List<ReportRevision> revisions) { this.revisions = revisions; }

    public SlaRecord getSlaRecord() { return slaRecord; }
    public void setSlaRecord(SlaRecord slaRecord) { this.slaRecord = slaRecord; }

    public List<ValidationDecision> getValidationDecisions() { return validationDecisions; }
    public void setValidationDecisions(List<ValidationDecision> validationDecisions) { this.validationDecisions = validationDecisions; }

    public List<MergeRecord> getChildMergeRecords() { return childMergeRecords; }
    public void setChildMergeRecords(List<MergeRecord> childMergeRecords) { this.childMergeRecords = childMergeRecords; }

    public List<MergeRecord> getParentMergeRecords() { return parentMergeRecords; }
    public void setParentMergeRecords(List<MergeRecord> parentMergeRecords) { this.parentMergeRecords = parentMergeRecords; }

    public Disposition getDisposition() { return disposition; }
    public void setDisposition(Disposition disposition) { this.disposition = disposition; }

    public List<FieldTask> getFieldTasks() { return fieldTasks; }
    public void setFieldTasks(List<FieldTask> fieldTasks) { this.fieldTasks = fieldTasks; }

    public ConfirmationRequest getConfirmationRequest() { return confirmationRequest; }
    public void setConfirmationRequest(ConfirmationRequest confirmationRequest) { this.confirmationRequest = confirmationRequest; }

    public DisputeRecord getDisputeRecord() { return disputeRecord; }
    public void setDisputeRecord(DisputeRecord disputeRecord) { this.disputeRecord = disputeRecord; }
}
