# PROMPT AGENT — Antigravity

**Module:** 4 — SLA, Sengketa & Audit
**Branch:** `fitur/modul-4-sla-dispute`
**Role:** Anggota 4 Tim AduAja

---

Anda adalah AI developer senior Spring Boot. Anda akan membuat **Module 4: SLA, Sengketa & Audit** dari proyek AduAja. Ikuti instruksi berikut dengan TEPAT.

## KONTEKS PROYEK

- **Framework**: Spring Boot 4.0.6 + Spring Data JPA + Thymeleaf + H2 (dev) + PostgreSQL (prod)
- **Java**: 25, **Maven**, **Lombok** (optional), **Spring Security** (csrf disable, permitAll sementara)
- **Package**: `com.plr.aduaja`
- **Database**: H2 in-memory `jdbc:h2:mem:aduaja` (dev), ddl-auto=update
- **Layout**: Thymeleaf dengan layout-dialect
- **Existing entities**: `User` (userId, fullName, email, passwordHash, role, accountStatus, phoneNumber), `Report` (reportId, ticketNumber, description, status, reporter), `Region`
- **Existing services**: UserService, ReportService, NotificationService (masing-masing sudah Interface + Impl)
- **Existing template master**: `layouts/master.html`
- **BaseEntity** SUDAH dibuat oleh Orang 1: `@MappedSuperclass` dengan `createdAt`, `updatedAt`, `@PrePersist`, `@PreUpdate`
- **Branch**: `fitur/modul-4-sla-dispute` (dibuat dari `main` setelah BaseEntity di-merge)

## ATURAN KRUSIAL — 4 PILAR PBO YANG BENAR

### 1. Inheritance = extends BaseEntity (`@MappedSuperclass`)

```java
// SALAH: @OneToOne, @ManyToOne disebut inheritance
// BENAR: extends BaseEntity
@Entity
@Table(name = "sla_records")
public class SlaRecord extends BaseEntity { ... }
```

### 2. Polymorphism = @Override + Overloading pada ServiceImpl

```java
// BENAR — Overloading: NAMA METHOD SAMA, parameter BERBEDA
public interface SlaRecordService {
    List<SlaRecord> getRecords(SlaStatus status);                   // 1 parameter
    List<SlaRecord> getRecords(LocalDateTime start, LocalDateTime end); // 2 parameter (OVERLOAD)
}

@Service
public class SlaRecordServiceImpl implements SlaRecordService {
    @Override
    public List<SlaRecord> getRecords(SlaStatus status) {           // @Override
        return slaRecordRepository.findByCurrentStatus(status);
    }

    @Override                                                       // @Override (OVERLOAD)
    public List<SlaRecord> getRecords(LocalDateTime start, LocalDateTime end) {
        return slaRecordRepository.findByDateRange(start, end);
    }
}
```

```java
// SALAH — ini BUKAN overloading (nama method berbeda):
public interface SlaRecordService {
    List<SlaRecord> getSlaRecordsByStatus(SlaStatus status);        // nama: getSlaRecordsByStatus
    List<SlaRecord> getOverdueSlaRecords();                          // nama: getOverdueSlaRecords (BEDA!)
    List<SlaRecord> getSlaRecordsByDateRange(...);                  // nama: getSlaRecordsByDateRange (BEDA!)
}
// TIGA method di atas memiliki NAMA BERBEDA → BUKAN POLYMORPHISM
```

**Aturan Overloading:**
- Nama method **HARUS SAMA PERSIS**
- Parameter **HARUS BERBEDA** (jumlah atau tipe)
- Contoh benar: `getRecords(X)`, `getRecords(X, Y)` → nama sama `getRecords`, parameter berbeda

### 3. Abstraction = Controller hanya inject Interface

```java
@Autowired
private SlaRecordService slaRecordService; // Interface!

// JANGAN: @Autowired private SlaRecordServiceImpl
```

### 4. Encapsulation = Private fields + DTO + Getter/Setter saja

```java
private String slaId; // PRIVATE
// Getter/Setter publik
// DTO untuk form input (bukan entity langsung)
```

## CHECKLIST WAJIB SEBELUM COMMIT

```
[ ] Entity extends BaseEntity
[ ] Ada Interface Service (XxxService.java)
[ ] Ada Impl Service dengan @Override (XxxServiceImpl.java)
[ ] Minimal 1 method OVERLOAD per Service interface
[ ] OVERLOAD = nama method SAMA, parameter BERBEDA (jumlah atau tipe)
[ ] Ada XxxDTO.java untuk setiap form input
[ ] Semua field entity = private
[ ] AuditLog = package-private setter (immutable)
[ ] Repository extends JpaRepository
```

---

# FILE YANG HARUS DIBUAT

---

## STEP 1: ENTITIES — 7 file

**Lokasi:** `src/main/java/com/plr/aduaja/model/`

### 1. SlaRecord.java

```java
package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sla_records")
public class SlaRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sla_id")
    private String slaId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @Column(name = "sla_start_at", nullable = false)
    private LocalDateTime slaStartAt;

    @Column(name = "sla_deadline_at", nullable = false)
    private LocalDateTime slaDeadlineAt;

    @Column(name = "total_paused_minutes", nullable = false)
    private Integer totalPausedMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private SlaStatus currentStatus = SlaStatus.BERJALAN;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum SlaStatus {
        BERJALAN, TERTUNDA, TERLAMBAT, SELESAI
    }

    public String getSlaId() { return slaId; }
    public void setSlaId(String slaId) { this.slaId = slaId; }
    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }
    public LocalDateTime getSlaStartAt() { return slaStartAt; }
    public void setSlaStartAt(LocalDateTime slaStartAt) { this.slaStartAt = slaStartAt; }
    public LocalDateTime getSlaDeadlineAt() { return slaDeadlineAt; }
    public void setSlaDeadlineAt(LocalDateTime slaDeadlineAt) { this.slaDeadlineAt = slaDeadlineAt; }
    public Integer getTotalPausedMinutes() { return totalPausedMinutes; }
    public void setTotalPausedMinutes(Integer totalPausedMinutes) { this.totalPausedMinutes = totalPausedMinutes; }
    public SlaStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(SlaStatus currentStatus) { this.currentStatus = currentStatus; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
```

### 2. SlaPauseLog.java

```java
package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sla_pause_logs")
public class SlaPauseLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pause_id")
    private String pauseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_id", nullable = false)
    private SlaRecord slaRecord;

    @Column(name = "paused_at", nullable = false)
    private LocalDateTime pausedAt;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    public String getPauseId() { return pauseId; }
    public void setPauseId(String pauseId) { this.pauseId = pauseId; }
    public SlaRecord getSlaRecord() { return slaRecord; }
    public void setSlaRecord(SlaRecord slaRecord) { this.slaRecord = slaRecord; }
    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }
    public LocalDateTime getResumedAt() { return resumedAt; }
    public void setResumedAt(LocalDateTime resumedAt) { this.resumedAt = resumedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

### 3. ConfirmationRequest.java

```java
package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "confirmation_requests")
public class ConfirmationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id")
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.MENUNGGU;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public enum RequestType { PENYELESAIAN, VALIDASI }
    public enum RequestStatus { MENUNGGU, DISETUJUI, DITOLAK }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }
    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }
    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
```

### 4. DisputeRecord.java

```java
package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_records")
public class DisputeRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "dispute_id")
    private String disputeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disputant_id", nullable = false)
    private User disputant;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status = DisputeStatus.MENUNGGU;

    @Column(name = "admin_decision", columnDefinition = "TEXT")
    private String adminDecision;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    public enum DisputeStatus { MENUNGGU, DIPROSES, DISETUJUI, DITOLAK }

    public String getDisputeId() { return disputeId; }
    public void setDisputeId(String disputeId) { this.disputeId = disputeId; }
    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }
    public User getDisputant() { return disputant; }
    public void setDisputant(User disputant) { this.disputant = disputant; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }
    public String getAdminDecision() { return adminDecision; }
    public void setAdminDecision(String adminDecision) { this.adminDecision = adminDecision; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public User getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(User resolvedBy) { this.resolvedBy = resolvedBy; }
}
```

### 5. MergeRecord.java

```java
package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "merge_records")
public class MergeRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "merge_id")
    private String mergeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_report_id", nullable = false)
    private Report primaryReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_report_id", nullable = false)
    private Report mergedReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_by", nullable = false)
    private User mergedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "similarity_score")
    private Integer similarityScore;

    @Column(name = "merged_at", nullable = false)
    private LocalDateTime mergedAt;

    public String getMergeId() { return mergeId; }
    public void setMergeId(String mergeId) { this.mergeId = mergeId; }
    public Report getPrimaryReport() { return primaryReport; }
    public void setPrimaryReport(Report primaryReport) { this.primaryReport = primaryReport; }
    public Report getMergedReport() { return mergedReport; }
    public void setMergedReport(Report mergedReport) { this.mergedReport = mergedReport; }
    public User getMergedBy() { return mergedBy; }
    public void setMergedBy(User mergedBy) { this.mergedBy = mergedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Integer similarityScore) { this.similarityScore = similarityScore; }
    public LocalDateTime getMergedAt() { return mergedAt; }
    public void setMergedAt(LocalDateTime mergedAt) { this.mergedAt = mergedAt; }
}
```

### 6. ValidationDecision.java

```java
package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "validation_decisions")
public class ValidationDecision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "decision_id")
    private String decisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by", nullable = false)
    private User decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    public enum Decision { DITERIMA, DITOLAK, PERLU_REVISI }

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }
    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }
    public User getDecidedBy() { return decidedBy; }
    public void setDecidedBy(User decidedBy) { this.decidedBy = decidedBy; }
    public Decision getDecision() { return decision; }
    public void setDecision(Decision decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
```

### 7. AuditLog.java — IMMUTABLE (package-private setter)

```java
package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private String logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Report report;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    // ============ GETTER (PUBLIC) ============ //
    public String getLogId() { return logId; }
    public User getActor() { return actor; }
    public Report getReport() { return report; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getActionType() { return actionType; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getIpAddress() { return ipAddress; }
    public String getDeviceInfo() { return deviceInfo; }
    public LocalDateTime getLoggedAt() { return loggedAt; }

    // ============ PACKAGE-PRIVATE SETTER (IMMUTABLE — ENCAPSULATION) ============ //
    void setLogId(String logId) { this.logId = logId; }
    void setActor(User actor) { this.actor = actor; }
    void setReport(Report report) { this.report = report; }
    void setTargetType(String targetType) { this.targetType = targetType; }
    void setTargetId(String targetId) { this.targetId = targetId; }
    void setActionType(String actionType) { this.actionType = actionType; }
    void setOldValue(String oldValue) { this.oldValue = oldValue; }
    void setNewValue(String newValue) { this.newValue = newValue; }
    void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}
```

---

## STEP 2: REPOSITORIES — 7 file

**Lokasi:** `src/main/java/com/plr/aduaja/repository/`

### 2.1 SlaRecordRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.SlaRecord;
import com.plr.aduaja.model.SlaRecord.SlaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlaRecordRepository extends JpaRepository<SlaRecord, String> {

    Optional<SlaRecord> findByReportId(String reportId);

    List<SlaRecord> findByCurrentStatus(SlaStatus status);

    @Query("SELECT s FROM SlaRecord s WHERE s.currentStatus = 'BERJALAN' AND s.slaDeadlineAt < :now")
    List<SlaRecord> findOverdue(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM SlaRecord s WHERE s.createdAt BETWEEN :start AND :end")
    List<SlaRecord> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByCurrentStatus(SlaStatus status);
}
```

### 2.2 SlaPauseLogRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.SlaPauseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlaPauseLogRepository extends JpaRepository<SlaPauseLog, String> {

    List<SlaPauseLog> findBySlaRecordId(String slaId);
}
```

### 2.3 ConfirmationRequestRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.ConfirmationRequest;
import com.plr.aduaja.model.ConfirmationRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfirmationRequestRepository extends JpaRepository<ConfirmationRequest, String> {

    List<ConfirmationRequest> findByReportId(String reportId);

    List<ConfirmationRequest> findByStatus(RequestStatus status);

    long countByStatus(RequestStatus status);
}
```

### 2.4 DisputeRecordRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.DisputeRecord;
import com.plr.aduaja.model.DisputeRecord.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRecordRepository extends JpaRepository<DisputeRecord, String> {

    List<DisputeRecord> findByReportId(String reportId);

    List<DisputeRecord> findByStatus(DisputeStatus status);

    long countByStatus(DisputeStatus status);
}
```

### 2.5 MergeRecordRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.MergeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MergeRecordRepository extends JpaRepository<MergeRecord, String> {

    List<MergeRecord> findByPrimaryReportId(String reportId);

    List<MergeRecord> findByMergedReportId(String reportId);
}
```

### 2.6 ValidationDecisionRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.ValidationDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValidationDecisionRepository extends JpaRepository<ValidationDecision, String> {

    List<ValidationDecision> findByReportId(String reportId);

    List<ValidationDecision> findByDecidedById(String adminId);
}
```

### 2.7 AuditLogRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByReportId(String reportId);

    List<AuditLog> findByActorId(String actorId);

    List<AuditLog> findByTargetTypeAndTargetId(String targetType, String targetId);
}
```

---

## STEP 3: DTOS — 4 file

**Lokasi:** `src/main/java/com/plr/aduaja/dto/`

### 3.1 SlaStatusDTO.java

```java
package com.plr.aduaja.dto;

public class SlaStatusDTO {

    private String reportId;
    private String slaDeadlineAt;
    private String currentStatus;
    private Integer totalPausedMinutes;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getSlaDeadlineAt() { return slaDeadlineAt; }
    public void setSlaDeadlineAt(String slaDeadlineAt) { this.slaDeadlineAt = slaDeadlineAt; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
    public Integer getTotalPausedMinutes() { return totalPausedMinutes; }
    public void setTotalPausedMinutes(Integer totalPausedMinutes) { this.totalPausedMinutes = totalPausedMinutes; }
}
```

### 3.2 DisputeDTO.java

```java
package com.plr.aduaja.dto;

public class DisputeDTO {

    private String reportId;
    private String reason;
    private String adminDecision;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getAdminDecision() { return adminDecision; }
    public void setAdminDecision(String adminDecision) { this.adminDecision = adminDecision; }
}
```

### 3.3 MergeDTO.java

```java
package com.plr.aduaja.dto;

public class MergeDTO {

    private String primaryReportId;
    private String mergedReportId;
    private String reason;
    private Integer similarityScore;

    public String getPrimaryReportId() { return primaryReportId; }
    public void setPrimaryReportId(String primaryReportId) { this.primaryReportId = primaryReportId; }
    public String getMergedReportId() { return mergedReportId; }
    public void setMergedReportId(String mergedReportId) { this.mergedReportId = mergedReportId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Integer similarityScore) { this.similarityScore = similarityScore; }
}
```

### 3.4 AuditLogDTO.java

```java
package com.plr.aduaja.dto;

public class AuditLogDTO {

    private String actionType;
    private String reportId;
    private String startDate;
    private String endDate;

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
```

---

## STEP 4: SERVICE INTERFACES + IMPLEMENTATIONS — 7 pasang (14 file)

**Lokasi Interface:** `src/main/java/com/plr/aduaja/service/`
**Lokasi Impl:** `src/main/java/com/plr/aduaja/service/`

### 4.1 SlaRecordService + SlaRecordServiceImpl

**SlaRecordService.java** — Interface (Abstraction)
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.SlaRecord;
import com.plr.aduaja.model.SlaRecord.SlaStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlaRecordService {

    Optional<SlaRecord> findById(String id);
    Optional<SlaRecord> findByReportId(String reportId);
    List<SlaRecord> getAllRecords();

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    List<SlaRecord> getRecords(SlaStatus status);                            // 1 parameter
    List<SlaRecord> getRecords(LocalDateTime start, LocalDateTime end);      // 2 parameter — OVERLOAD

    SlaRecord createSlaRecord(String reportId, Integer durationHours);
    SlaRecord pauseSla(String slaId, String reason);
    SlaRecord resumeSla(String slaId);
    SlaRecord completeSla(String slaId);
    void checkAndUpdateOverdueSla();
}
```

**SlaRecordServiceImpl.java** — @Override (Run-time Polymorphism)
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.SlaRecord;
import com.plr.aduaja.model.SlaRecord.SlaStatus;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.SlaRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SlaRecordServiceImpl implements SlaRecordService {

    @Autowired
    private SlaRecordRepository slaRecordRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Override
    public Optional<SlaRecord> findById(String id) {
        return slaRecordRepository.findById(id);
    }

    @Override
    public Optional<SlaRecord> findByReportId(String reportId) {
        return slaRecordRepository.findByReportId(reportId);
    }

    @Override
    public List<SlaRecord> getAllRecords() {
        return slaRecordRepository.findAll();
    }

    @Override
    public List<SlaRecord> getRecords(SlaStatus status) {
        return slaRecordRepository.findByCurrentStatus(status);
    }

    @Override
    public List<SlaRecord> getRecords(LocalDateTime start, LocalDateTime end) {
        return slaRecordRepository.findByDateRange(start, end);
    }

    @Override
    @Transactional
    public SlaRecord createSlaRecord(String reportId, Integer durationHours) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));

        SlaRecord sla = new SlaRecord();
        sla.setReport(report);
        sla.setSlaStartAt(LocalDateTime.now());
        sla.setSlaDeadlineAt(LocalDateTime.now().plusHours(durationHours));
        sla.setTotalPausedMinutes(0);
        sla.setCurrentStatus(SlaStatus.BERJALAN);

        return slaRecordRepository.save(sla);
    }

    @Override
    @Transactional
    public SlaRecord pauseSla(String slaId, String reason) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
                .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan"));
        sla.setCurrentStatus(SlaStatus.TERTUNDA);
        return slaRecordRepository.save(sla);
    }

    @Override
    @Transactional
    public SlaRecord resumeSla(String slaId) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
                .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan"));
        sla.setCurrentStatus(SlaStatus.BERJALAN);
        return slaRecordRepository.save(sla);
    }

    @Override
    @Transactional
    public SlaRecord completeSla(String slaId) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
                .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan"));
        sla.setCurrentStatus(SlaStatus.SELESAI);
        sla.setCompletedAt(LocalDateTime.now());
        return slaRecordRepository.save(sla);
    }

    @Override
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
```

### 4.2 SlaMonitoringService + SlaMonitoringServiceImpl

**SlaMonitoringService.java:**
```java
package com.plr.aduaja.service;

import java.util.List;
import java.util.Map;

public interface SlaMonitoringService {

    Map<String, Object> getSlaStatistics();
    List<Map<String, Object>> getLateItems();
    Map<String, Object> getReportSlaStatus(String reportId);

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    List<Map<String, Object>> getSlaSummary();                    // tanpa parameter
    List<Map<String, Object>> getSlaSummary(String dinasId);     // 1 parameter String — OVERLOAD
}
```

**SlaMonitoringServiceImpl.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.SlaRecord;
import com.plr.aduaja.model.SlaRecord.SlaStatus;
import com.plr.aduaja.repository.SlaRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SlaMonitoringServiceImpl implements SlaMonitoringService {

    @Autowired
    private SlaRecordRepository slaRecordRepository;

    @Override
    public Map<String, Object> getSlaStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", slaRecordRepository.count());
        stats.put("berjalan", slaRecordRepository.countByCurrentStatus(SlaStatus.BERJALAN));
        stats.put("tertunda", slaRecordRepository.countByCurrentStatus(SlaStatus.TERTUNDA));
        stats.put("terlambat", slaRecordRepository.countByCurrentStatus(SlaStatus.TERLAMBAT));
        stats.put("selesai", slaRecordRepository.countByCurrentStatus(SlaStatus.SELESAI));
        return stats;
    }

    @Override
    public List<Map<String, Object>> getLateItems() {
        List<SlaRecord> overdue = slaRecordRepository.findOverdue(LocalDateTime.now());
        List<Map<String, Object>> items = new ArrayList<>();
        for (SlaRecord sla : overdue) {
            Map<String, Object> item = new HashMap<>();
            item.put("slaId", sla.getSlaId());
            item.put("reportId", sla.getReport() != null ? sla.getReport().getId() : null);
            item.put("deadline", sla.getSlaDeadlineAt());
            item.put("minutesLate", Duration.between(sla.getSlaDeadlineAt(), LocalDateTime.now()).toMinutes());
            items.add(item);
        }
        return items;
    }

    @Override
    public Map<String, Object> getReportSlaStatus(String reportId) {
        Optional<SlaRecord> slaOpt = slaRecordRepository.findByReportId(reportId);
        Map<String, Object> result = new HashMap<>();
        if (slaOpt.isPresent()) {
            SlaRecord sla = slaOpt.get();
            result.put("slaId", sla.getSlaId());
            result.put("status", sla.getCurrentStatus());
            result.put("deadline", sla.getSlaDeadlineAt());
            result.put("pausedMinutes", sla.getTotalPausedMinutes());
        } else {
            result.put("status", "TIDAK_ADA");
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getSlaSummary() {
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getSlaSummary(String dinasId) {
        return new ArrayList<>();
    }
}
```

### 4.3 DisputeService + DisputeServiceImpl

**DisputeService.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.dto.DisputeDTO;
import com.plr.aduaja.model.DisputeRecord;
import com.plr.aduaja.model.DisputeRecord.DisputeStatus;

import java.util.List;
import java.util.Optional;

public interface DisputeService {

    DisputeRecord createDispute(DisputeDTO dto, String disputantId);
    DisputeRecord resolveDispute(String disputeId, DisputeStatus decision, String adminId);
    Optional<DisputeRecord> getDisputeById(String disputeId);

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    List<DisputeRecord> getDisputes(String reportId);             // 1 parameter String (reportId)
    List<DisputeRecord> getDisputes(DisputeStatus status);       // 1 parameter DisputeStatus — OVERLOAD

    List<DisputeRecord> getAllDisputes();
}
```

**DisputeServiceImpl.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.dto.DisputeDTO;
import com.plr.aduaja.model.DisputeRecord;
import com.plr.aduaja.model.DisputeRecord.DisputeStatus;
import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.DisputeRecordRepository;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DisputeServiceImpl implements DisputeService {

    @Autowired
    private DisputeRecordRepository disputeRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public DisputeRecord createDispute(DisputeDTO dto, String disputantId) {
        Report report = reportRepository.findById(dto.getReportId())
                .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));
        User disputant = userRepository.findById(disputantId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        DisputeRecord dispute = new DisputeRecord();
        dispute.setReport(report);
        dispute.setDisputant(disputant);
        dispute.setReason(dto.getReason());
        dispute.setStatus(DisputeStatus.MENUNGGU);
        return disputeRepository.save(dispute);
    }

    @Override
    @Transactional
    public DisputeRecord resolveDispute(String disputeId, DisputeStatus decision, String adminId) {
        DisputeRecord dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Sengketa tidak ditemukan"));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

        dispute.setStatus(decision);
        dispute.setResolvedBy(admin);
        dispute.setResolvedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }

    @Override
    public Optional<DisputeRecord> getDisputeById(String disputeId) {
        return disputeRepository.findById(disputeId);
    }

    @Override
    public List<DisputeRecord> getDisputes(String reportId) {
        return disputeRepository.findByReportId(reportId);
    }

    @Override
    public List<DisputeRecord> getDisputes(DisputeStatus status) {
        return disputeRepository.findByStatus(status);
    }

    @Override
    public List<DisputeRecord> getAllDisputes() {
        return disputeRepository.findAll();
    }
}
```

### 4.4 MergeRecordService + MergeRecordServiceImpl

**MergeRecordService.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.dto.MergeDTO;
import com.plr.aduaja.model.MergeRecord;

import java.util.List;

public interface MergeRecordService {

    MergeRecord createMerge(MergeDTO dto, String userId);

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    List<MergeRecord> getMerges();                                // tanpa parameter
    List<MergeRecord> getMerges(String reportId);                 // 1 parameter String — OVERLOAD

    void cancelMerge(String mergeId);
}
```

**MergeRecordServiceImpl.java:**
```java
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
import java.util.List;

@Service
public class MergeRecordServiceImpl implements MergeRecordService {

    @Autowired
    private MergeRecordRepository mergeRecordRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public MergeRecord createMerge(MergeDTO dto, String userId) {
        Report primary = reportRepository.findById(dto.getPrimaryReportId())
                .orElseThrow(() -> new RuntimeException("Primary report tidak ditemukan"));
        Report merged = reportRepository.findById(dto.getMergedReportId())
                .orElseThrow(() -> new RuntimeException("Merged report tidak ditemukan"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        MergeRecord merge = new MergeRecord();
        merge.setPrimaryReport(primary);
        merge.setMergedReport(merged);
        merge.setMergedBy(user);
        merge.setReason(dto.getReason());
        merge.setSimilarityScore(dto.getSimilarityScore());
        merge.setMergedAt(LocalDateTime.now());
        return mergeRecordRepository.save(merge);
    }

    @Override
    public List<MergeRecord> getMerges() {
        return mergeRecordRepository.findAll();
    }

    @Override
    public List<MergeRecord> getMerges(String reportId) {
        List<MergeRecord> primaryMerges = mergeRecordRepository.findByPrimaryReportId(reportId);
        List<MergeRecord> mergedMerges = mergeRecordRepository.findByMergedReportId(reportId);
        primaryMerges.addAll(mergedMerges);
        return primaryMerges;
    }

    @Override
    @Transactional
    public void cancelMerge(String mergeId) {
        mergeRecordRepository.deleteById(mergeId);
    }
}
```

### 4.5 ConfirmationService + ConfirmationServiceImpl

**ConfirmationService.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.ConfirmationRequest;
import com.plr.aduaja.model.ConfirmationRequest.RequestStatus;
import com.plr.aduaja.model.ConfirmationRequest.RequestType;

import java.util.List;

public interface ConfirmationService {

    ConfirmationRequest createRequest(String reportId, String requesterId, RequestType type);
    ConfirmationRequest respondToRequest(String requestId, RequestStatus status, String notes);

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    List<ConfirmationRequest> getRequests(RequestStatus status);    // 1 parameter RequestStatus
    List<ConfirmationRequest> getRequests(String reportId);         // 1 parameter String — OVERLOAD

    List<ConfirmationRequest> getAllRequests();
}
```

**ConfirmationServiceImpl.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.ConfirmationRequest;
import com.plr.aduaja.model.ConfirmationRequest.RequestStatus;
import com.plr.aduaja.model.ConfirmationRequest.RequestType;
import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.ConfirmationRequestRepository;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConfirmationServiceImpl implements ConfirmationService {

    @Autowired
    private ConfirmationRequestRepository confirmationRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public ConfirmationRequest createRequest(String reportId, String requesterId, RequestType type) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        ConfirmationRequest request = new ConfirmationRequest();
        request.setReport(report);
        request.setRequester(requester);
        request.setRequestType(type);
        request.setStatus(RequestStatus.MENUNGGU);
        return confirmationRepository.save(request);
    }

    @Override
    @Transactional
    public ConfirmationRequest respondToRequest(String requestId, RequestStatus status, String notes) {
        ConfirmationRequest request = confirmationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request tidak ditemukan"));
        request.setStatus(status);
        request.setNotes(notes);
        request.setRespondedAt(LocalDateTime.now());
        return confirmationRepository.save(request);
    }

    @Override
    public List<ConfirmationRequest> getRequests(RequestStatus status) {
        return confirmationRepository.findByStatus(status);
    }

    @Override
    public List<ConfirmationRequest> getRequests(String reportId) {
        return confirmationRepository.findByReportId(reportId);
    }

    @Override
    public List<ConfirmationRequest> getAllRequests() {
        return confirmationRepository.findAll();
    }
}
```

### 4.6 ValidationDecisionService + ValidationDecisionServiceImpl

**ValidationDecisionService.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.ValidationDecision;
import com.plr.aduaja.model.ValidationDecision.Decision;

import java.util.List;

public interface ValidationDecisionService {

    ValidationDecision createDecision(String reportId, String adminId, Decision decision, String reason);

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    List<ValidationDecision> getDecisions();                          // tanpa parameter
    List<ValidationDecision> getDecisions(String reportOrAdminId);   // 1 parameter String — OVERLOAD
}
```

**ValidationDecisionServiceImpl.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.User;
import com.plr.aduaja.model.ValidationDecision;
import com.plr.aduaja.model.ValidationDecision.Decision;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.repository.ValidationDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ValidationDecisionServiceImpl implements ValidationDecisionService {

    @Autowired
    private ValidationDecisionRepository decisionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public ValidationDecision createDecision(String reportId, String adminId, Decision decision, String reason) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

        ValidationDecision vd = new ValidationDecision();
        vd.setReport(report);
        vd.setDecidedBy(admin);
        vd.setDecision(decision);
        vd.setReason(reason);
        vd.setDecidedAt(LocalDateTime.now());
        return decisionRepository.save(vd);
    }

    @Override
    public List<ValidationDecision> getDecisions() {
        return decisionRepository.findAll();
    }

    @Override
    public List<ValidationDecision> getDecisions(String reportOrAdminId) {
        List<ValidationDecision> byReport = decisionRepository.findByReportId(reportOrAdminId);
        if (!byReport.isEmpty()) return byReport;
        return decisionRepository.findByDecidedById(reportOrAdminId);
    }
}
```

### 4.7 AuditLogService + AuditLogServiceImpl

**AuditLogService.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.AuditLog;
import com.plr.aduaja.model.User;

import java.util.List;

public interface AuditLogService {

    // ============ OVERLOADING: nama method SAMA, parameter BERBEDA ============ //
    AuditLog log(User actor, String actionType, String oldVal, String newVal);                                          // 4 parameter
    AuditLog log(User actor, String reportId, String action, String oldVal, String newVal);                              // 5 parameter — OVERLOAD
    AuditLog log(User actor, String targetType, String targetId, String action, String oldVal, String newVal);           // 6 parameter — OVERLOAD

    List<AuditLog> getLogsByReport(String reportId);
    List<AuditLog> getLogsByActor(String actorId);
    List<AuditLog> getAllLogs();
}
```

**AuditLogServiceImpl.java:**
```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.AuditLog;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public AuditLog log(User actor, String actionType, String oldVal, String newVal) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setActionType(actionType);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);
        log.setIpAddress("0.0.0.0");
        log.setDeviceInfo("System");
        log.setLoggedAt(LocalDateTime.now());
        return auditLogRepository.save(log);
    }

    @Override
    @Transactional
    public AuditLog log(User actor, String reportId, String action, String oldVal, String newVal) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setReport(reportId);
        log.setTargetType("REPORT");
        log.setTargetId(reportId);
        log.setActionType(action);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);
        log.setIpAddress("0.0.0.0");
        log.setDeviceInfo("System");
        log.setLoggedAt(LocalDateTime.now());
        return auditLogRepository.save(log);
    }

    @Override
    @Transactional
    public AuditLog log(User actor, String targetType, String targetId, String action, String oldVal, String newVal) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setActionType(action);
        log.setOldValue(oldVal);
        log.setNewValue(newVal);
        log.setIpAddress("0.0.0.0");
        log.setDeviceInfo("System");
        log.setLoggedAt(LocalDateTime.now());
        return auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> getLogsByReport(String reportId) {
        return auditLogRepository.findByReportId(reportId);
    }

    @Override
    public List<AuditLog> getLogsByActor(String actorId) {
        return auditLogRepository.findByActorId(actorId);
    }

    @Override
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }
}
```

---

## STEP 5: CONTROLLER — Thymeleaf Views

**Lokasi:** `src/main/java/com/plr/aduaja/controller/`

**AdminControllerModul4.java:** (Abstraction: hanya inject Interface)
```java
package com.plr.aduaja.controller;

import com.plr.aduaja.dto.DisputeDTO;
import com.plr.aduaja.dto.MergeDTO;
import com.plr.aduaja.model.DisputeRecord;
import com.plr.aduaja.model.ValidationDecision.Decision;
import com.plr.aduaja.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminControllerModul4 {

    @Autowired
    private DisputeService disputeService;              // ← INTERFACE (Abstraction)

    @Autowired
    private MergeRecordService mergeRecordService;       // ← INTERFACE (Abstraction)

    @Autowired
    private ValidationDecisionService validationDecisionService; // ← INTERFACE (Abstraction)

    @Autowired
    private ConfirmationService confirmationService;      // ← INTERFACE (Abstraction)

    @Autowired
    private AuditLogService auditLogService;              // ← INTERFACE (Abstraction)


    // ============ SENGKETA ============ //

    @GetMapping("/sengketa")
    public String sengketaPanel(Model model) {
        List<DisputeRecord> disputes = disputeService.getAllDisputes();
        model.addAttribute("disputes", disputes);
        model.addAttribute("disputeDTO", new DisputeDTO());
        return "admin/sengketa-panel";
    }

    @PostMapping("/sengketa")
    public String prosesSengketa(@RequestParam String disputeId,
                                  @RequestParam String action,
                                  @RequestParam(required = false) String adminId) {
        DisputeRecord.DisputeStatus status = "terima".equals(action)
                ? DisputeRecord.DisputeStatus.DISETUJUI
                : DisputeRecord.DisputeStatus.DITOLAK;

        if (adminId != null) {
            disputeService.resolveDispute(disputeId, status, adminId);
        }
        return "redirect:/admin/sengketa";
    }

    // ============ MERGE TICKET ============ //

    @GetMapping("/merge")
    public String mergePanel(Model model) {
        model.addAttribute("merges", mergeRecordService.getMerges());
        model.addAttribute("mergeDTO", new MergeDTO());
        return "admin/merge-ticket-panel";
    }

    @PostMapping("/merge")
    public String prosesMerge(@ModelAttribute MergeDTO mergeDTO,
                               @RequestParam String userId) {
        mergeRecordService.createMerge(mergeDTO, userId);
        return "redirect:/admin/merge";
    }

    // ============ VALIDATION ============ //

    @GetMapping("/validation")
    public String validationPanel(Model model) {
        model.addAttribute("decisions", validationDecisionService.getDecisions());
        return "admin/validation-panel";
    }

    @PostMapping("/validation")
    public String prosesValidasi(@RequestParam String reportId,
                                  @RequestParam String adminId,
                                  @RequestParam Decision decision,
                                  @RequestParam(required = false) String reason) {
        validationDecisionService.createDecision(reportId, adminId, decision, reason);
        return "redirect:/admin/validation";
    }

    // ============ DINAS SENGKETA ============ //

    @GetMapping("/dinas/sengketa")
    public String sengketaDinas(Model model) {
        model.addAttribute("disputes", disputeService.getAllDisputes());
        return "admin/dinas/sengketa-dinas";
    }
}
```

---

## STEP 6: THYMELEAF VIEWS — 5 file

**Lokasi:** `src/main/resources/templates/`

### 6.1 admin/sengketa-panel.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">
<head>
    <title>Sengketa Panel - Admin</title>
</head>
<body>
<div layout:fragment="content">
    <div class="container-fluid p-4">
        <h1 class="h3 mb-4">Panel Sengketa</h1>

        <div class="card shadow-sm rounded">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead class="table-light">
                            <tr>
                                <th>ID Sengketa</th>
                                <th>Pelapor</th>
                                <th>Alasan</th>
                                <th>Status</th>
                                <th>Aksi</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr th:each="dispute : ${disputes}">
                                <td th:text="${dispute.disputeId}">ID</td>
                                <td th:text="${dispute.disputant?.fullName}">Nama</td>
                                <td th:text="${dispute.reason}">Alasan</td>
                                <td>
                                    <span class="badge"
                                          th:classappend="${dispute.status.name() == 'MENUNGGU' ? 'badge-warning' :
                                              dispute.status.name() == 'DISETUJUI' ? 'badge-success' : 'badge-danger'}">
                                        [[${dispute.status}]]
                                    </span>
                                </td>
                                <td>
                                    <form th:action="@{/admin/sengketa}" method="post" style="display:inline;">
                                        <input type="hidden" name="disputeId" th:value="${dispute.disputeId}" />
                                        <input type="hidden" name="action" value="terima" />
                                        <button type="submit" class="btn btn-success btn-sm"
                                                th:disabled="${dispute.status.name() != 'MENUNGGU'}">Setujui</button>
                                    </form>
                                    <form th:action="@{/admin/sengketa}" method="post" style="display:inline;">
                                        <input type="hidden" name="disputeId" th:value="${dispute.disputeId}" />
                                        <input type="hidden" name="action" value="tolak" />
                                        <button type="submit" class="btn btn-danger btn-sm"
                                                th:disabled="${dispute.status.name() != 'MENUNGGU'}">Tolak</button>
                                    </form>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

### 6.2 admin/merge-ticket-panel.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">
<head>
    <title>Merge Ticket - Admin</title>
</head>
<body>
<div layout:fragment="content">
    <div class="container-fluid p-4">
        <h1 class="h3 mb-4">Merge Laporan Ganda</h1>

        <div class="row">
            <div class="col-md-5">
                <div class="card shadow-sm rounded mb-4">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0">Form Merge</h5>
                    </div>
                    <div class="card-body">
                        <form th:action="@{/admin/merge}" method="post">
                            <div class="mb-3">
                                <label class="form-label">Primary Report ID</label>
                                <input type="text" class="form-control" name="primaryReportId" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Merged Report ID</label>
                                <input type="text" class="form-control" name="mergedReportId" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Alasan</label>
                                <textarea class="form-control" name="reason" rows="2"></textarea>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Similarity Score</label>
                                <input type="number" class="form-control" name="similarityScore" min="0" max="100" />
                            </div>
                            <input type="hidden" name="userId" value="system" />
                            <button type="submit" class="btn btn-primary w-100">Merge</button>
                        </form>
                    </div>
                </div>
            </div>
            <div class="col-md-7">
                <div class="card shadow-sm rounded">
                    <div class="card-header bg-info text-white">
                        <h5 class="mb-0">Riwayat Merge</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead class="table-light">
                                    <tr>
                                        <th>Primary</th>
                                        <th>Merged</th>
                                        <th>Score</th>
                                        <th>Tanggal</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr th:each="merge : ${merges}">
                                        <td th:text="${merge.primaryReport?.reportId}">Primary</td>
                                        <td th:text="${merge.mergedReport?.reportId}">Merged</td>
                                        <td th:text="${merge.similarityScore}">Score</td>
                                        <td th:text="${merge.mergedAt}">Tanggal</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

### 6.3 admin/validation-panel.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">
<head>
    <title>Validation Panel - Admin</title>
</head>
<body>
<div layout:fragment="content">
    <div class="container-fluid p-4">
        <h1 class="h3 mb-4">Panel Keputusan Validasi</h1>

        <div class="row">
            <div class="col-md-5">
                <div class="card shadow-sm rounded mb-4">
                    <div class="card-header bg-success text-white">
                        <h5 class="mb-0">Buat Keputusan</h5>
                    </div>
                    <div class="card-body">
                        <form th:action="@{/admin/validation}" method="post">
                            <div class="mb-3">
                                <label class="form-label">Report ID</label>
                                <input type="text" class="form-control" name="reportId" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Admin ID</label>
                                <input type="text" class="form-control" name="adminId" required />
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Keputusan</label>
                                <select class="form-control" name="decision" required>
                                    <option value="DITERIMA">Diterima</option>
                                    <option value="DITOLAK">Ditolak</option>
                                    <option value="PERLU_REVISI">Perlu Revisi</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Alasan</label>
                                <textarea class="form-control" name="reason" rows="2"></textarea>
                            </div>
                            <button type="submit" class="btn btn-success w-100">Simpan Keputusan</button>
                        </form>
                    </div>
                </div>
            </div>
            <div class="col-md-7">
                <div class="card shadow-sm rounded">
                    <div class="card-header bg-secondary text-white">
                        <h5 class="mb-0">Riwayat Keputusan</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead class="table-light">
                                    <tr>
                                        <th>Report</th>
                                        <th>Decision</th>
                                        <th>Alasan</th>
                                        <th>Tanggal</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr th:each="d : ${decisions}">
                                        <td th:text="${d.report?.reportId}">Report</td>
                                        <td>
                                            <span class="badge"
                                                  th:classappend="${d.decision.name() == 'DITERIMA' ? 'badge-success' :
                                                      d.decision.name() == 'DITOLAK' ? 'badge-danger' : 'badge-warning'}">
                                                [[${d.decision}]]
                                            </span>
                                        </td>
                                        <td th:text="${d.reason}">Alasan</td>
                                        <td th:text="${d.decidedAt}">Tanggal</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

### 6.4 admin/dinas/sengketa-dinas.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">
<head>
    <title>Sengketa Dinas - Admin</title>
</head>
<body>
<div layout:fragment="content">
    <div class="container-fluid p-4">
        <h1 class="h3 mb-4">Sengketa Level Dinas</h1>

        <div class="card shadow-sm rounded">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead class="table-light">
                            <tr>
                                <th>ID Sengketa</th>
                                <th>Report ID</th>
                                <th>Alasan</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr th:each="dispute : ${disputes}">
                                <td th:text="${dispute.disputeId}">ID</td>
                                <td th:text="${dispute.report?.reportId}">Report</td>
                                <td th:text="${dispute.reason}">Alasan</td>
                                <td>
                                    <span class="badge"
                                          th:classappend="${dispute.status.name() == 'MENUNGGU' ? 'badge-warning' :
                                              dispute.status.name() == 'DISETUJUI' ? 'badge-success' : 'badge-danger'}">
                                        [[${dispute.status}]]
                                    </span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

### 6.5 warga/report-detail.html (bagian sengketa warga)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">
<head>
    <title>Detail Laporan - Warga</title>
</head>
<body>
<div layout:fragment="content">
    <div class="container-fluid p-4">
        <div class="card shadow-sm rounded mb-4">
            <div class="card-header bg-primary text-white">
                <h5 class="mb-0">Detail Laporan</h5>
            </div>
            <div class="card-body">
                <h4>#[[${report.ticketNumber}]]</h4>
                <p class="text-muted">Status:
                    <span class="badge" th:text="${report.status}">Status</span>
                </p>
                <hr />

                <!-- ============ BAGIAN SENGKETA WARGA (ENCAPSULATION via DTO) ============ -->
                <div class="mt-4">
                    <h5 class="text-danger">Ajukan Sengketa</h5>
                    <p class="text-muted">Jika Anda tidak puas dengan hasil laporan, ajukan sengketa di sini.</p>

                    <form th:action="@{/warga/sengketa}" method="post">
                        <input type="hidden" name="reportId" th:value="${report.reportId}" />
                        <div class="mb-3">
                            <label class="form-label">Alasan Sengketa</label>
                            <textarea class="form-control" name="reason" rows="3" required
                                      placeholder="Jelaskan alasan Anda mengajukan sengketa..."></textarea>
                        </div>
                        <button type="submit" class="btn btn-danger">Ajukan Sengketa</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

---

## PBO EVIDENCE — RINGKASAN PER SERVICE

### SlaRecordService — Contoh Overloading PALING LENGKAP

```java
// INTERFACE — muat method overloading
public interface SlaRecordService {
    List<SlaRecord> getRecords(SlaStatus status);                        // ← method: getRecords
    List<SlaRecord> getRecords(LocalDateTime start, LocalDateTime end);  // ← method: getRecords (sama! OVERLOAD)
}

// IMPL — @Override untuk keduanya
@Service
public class SlaRecordServiceImpl implements SlaRecordService {
    @Override
    public List<SlaRecord> getRecords(SlaStatus status) { ... }                       // @Override
    @Override
    public List<SlaRecord> getRecords(LocalDateTime start, LocalDateTime end) { ... } // @Override (OVERLOAD)
}
```

### DisputeService — Contoh Overloading

```java
public interface DisputeService {
    List<DisputeRecord> getDisputes(String reportId);           // method: getDisputes, param: String
    List<DisputeRecord> getDisputes(DisputeStatus status);      // method: getDisputes, param: DisputeStatus (OVERLOAD)
}
```

### ConfirmationService — Contoh Overloading

```java
public interface ConfirmationService {
    List<ConfirmationRequest> getRequests(RequestStatus status);  // method: getRequests, param: RequestStatus
    List<ConfirmationRequest> getRequests(String reportId);       // method: getRequests, param: String (OVERLOAD)
}
```

### AuditLogService — Contoh Overloading (jumlah parameter berbeda)

```java
public interface AuditLogService {
    AuditLog log(User actor, String actionType, String oldVal, String newVal);                                        // 4 parameter
    AuditLog log(User actor, String reportId, String action, String oldVal, String newVal);                            // 5 parameter (OVERLOAD)
    AuditLog log(User actor, String targetType, String targetId, String action, String oldVal, String newVal);         // 6 parameter (OVERLOAD)
}
```

---

## VERIFIKASI KEBERHASILAN

1. **Maven compile sukses** — `.\mvnw.cmd compile` tidak ada error
2. **Aplikasi bisa jalan** — `.\mvnw.cmd spring-boot:run` tidak ada error
3. **Halaman admin** bisa diakses via browser:
   - `http://localhost:8080/admin/sengketa`
   - `http://localhost:8080/admin/merge`
   - `http://localhost:8080/admin/validation`
4. **Data masuk ke database** — cek via H2 Console `jdbc:h2:mem:aduaja`
5. **4 Pilar PBO terpenuhi:**
   - ✅ **Inheritance**: semua entity `extends BaseEntity`
   - ✅ **Polymorphism — @Override**: semua Impl pakai `@Override`
   - ✅ **Polymorphism — Overloading**: setiap Service memiliki minimal 1 method dengan nama SAMA, parameter BERBEDA
   - ✅ **Abstraction**: Controller hanya inject Interface, bukan Impl
   - ✅ **Encapsulation**: semua field `private`, ada DTO terpisah, AuditLog pakai package-private setter

## CHECKLIST COMMIT

```
[ ] Entity extends BaseEntity
[ ] Ada Interface Service (XxxService.java)
[ ] Ada Impl Service dengan @Override (XxxServiceImpl.java)
[ ] Minimal 1 method OVERLOAD per Service interface
[ ] OVERLOAD = nama method SAMA, parameter BERBEDA (jumlah atau tipe)
[ ] Ada XxxDTO.java untuk setiap form input
[ ] Semua field entity = private
[ ] AuditLog = package-private setter (immutable)
[ ] Repository extends JpaRepository
[ ] Controller hanya inject Interface, bukan Impl
[ ] `mvn compile` sukses
```
