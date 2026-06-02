# Dokumentasi 4 Pilar PBO — AduAja

> **Proyek:** Sistem Pelaporan dan Pengaduan Masyarakat (AduAja)
> **Framework:** Spring Boot 4.0.6 + Spring Data JPA + Thymeleaf + H2
> **Tim:** 4 Orang
> **Tanggal:** 21 Mei 2026

---

## Daftar Isi

1. [Definisi 4 Pilar PBO](#1-definisi-4-pilar-pbo)
2. [Inheritance (Pewarisan)](#2-inheritance)
3. [Polymorphism (Polimorfisme)](#3-polymorphism)
4. [Abstraction (Abstraksi)](#4-abstraction)
5. [Encapsulation (Enkapsulasi)](#5-encapsulation)
6. [Matriks Penerapan Per Anggota](#6-matriks-penerapan-per-anggota)

---

## 1. Definisi 4 Pilar PBO

### 1.1 Inheritance (Pewarisan)

Hubungan **IS-A** antara parent class dan child class menggunakan keyword `extends`.

```java
// Parent class
@MappedSuperclass
public abstract class BaseEntity {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Child class — IS-A BaseEntity (TRUE INHERITANCE)
public class User extends BaseEntity { ... }
```

> **Catatan:** `@ManyToOne`, `@OneToMany`, `@OneToOne` adalah **HAS-A** (Composition/Aggregation), **BUKAN** Inheritance.

### 1.2 Polymorphism (Polimorfisme)

Dua jenis:

| Jenis | Keyword | Waktu | Contoh |
|-------|---------|-------|--------|
| **Overloading** | Nama method sama, parameter berbeda | Compile-time | `log(4 param)` vs `log(5 param)` |
| **Overriding** | `@Override` | Run-time | `UserServiceImpl implements UserService` |

> **Catatan:** Switch-case pada enum adalah **Procedural Code**, BUKAN Polymorphism.

### 1.3 Abstraction (Abstraksi)

Menyembunyikan detail implementasi di belakang **interface**.

```java
// Controller hanya tahu interface, tidak tahu implementasi
@Autowired
private UserService userService;  // ← Interface, bukan class impl
```

### 1.4 Encapsulation (Enkapsulasi)

Menyembunyikan data internal dengan:
1. **Field `private`** — tidak bisa diakses langsung dari luar
2. **DTO** — memisahkan form input dari entity
3. **Package-private setter** — membatasi siapa yang bisa mengubah nilai

---

## 2. Inheritance

### 2.1 BaseEntity (Parent Class)

Dibuat oleh **Orang 1**, digunakan oleh semua anggota.

```java
// src/main/java/com/plr/aduaja/model/BaseEntity.java
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### 2.2 Inheritance per Anggota

#### Orang 1 — Auth & Profil (5 entity)

```java
// User.java
@Entity @Table(name = "users")
public class User extends BaseEntity { ... }

// UserProfile.java
@Entity @Table(name = "user_profiles")
public class UserProfile extends BaseEntity { ... }

// LoginAttempt.java
@Entity @Table(name = "login_attempts")
public class LoginAttempt extends BaseEntity { ... }

// OtpVerification.java
@Entity @Table(name = "otp_verifications")
public class OtpVerification extends BaseEntity { ... }

// ActiveSession.java
@Entity @Table(name = "active_sessions")
public class ActiveSession extends BaseEntity { ... }
```

#### Orang 2 — Pelaporan & Notifikasi (4 entity)

```java
// Report.java
@Entity @Table(name = "reports")
public class Report extends BaseEntity { ... }

// ReportCategory.java
@Entity @Table(name = "report_categories")
public class ReportCategory extends BaseEntity { ... }

// ReportRevision.java
@Entity @Table(name = "report_revisions")
public class ReportRevision extends BaseEntity { ... }

// Notification.java
@Entity @Table(name = "notifications")
public class Notification extends BaseEntity { ... }
```

#### Orang 3 — Disposisi & Eksekusi Lapangan (6 entity)

```java
// FieldTask.java
@Entity @Table(name = "field_tasks")
public class FieldTask extends BaseEntity { ... }

// OfficerAttendance.java
@Entity @Table(name = "officer_attendance")
public class OfficerAttendance { ... }        // ❌ BELUM extends BaseEntity

// Disposition.java
@Entity @Table(name = "dispositions")
public class Disposition extends BaseEntity { ... }

// Agency.java
@Entity @Table(name = "agencies")
public class Agency extends BaseEntity { ... }

// TaskEvidence.java
@Entity @Table(name = "task_evidences")
public class TaskEvidence extends BaseEntity { ... }

// TaskPostponement.java
@Entity @Table(name = "task_postponements")
public class TaskPostponement extends BaseEntity { ... }
```

> **Catatan:** `OfficerAttendance` **belum** extends `BaseEntity` — tidak memiliki `createdAt`/`updatedAt`.

#### Orang 4 — SLA, Sengketa & Audit (7 entity) ✅

```java
// SlaRecord.java
@Entity @Table(name = "sla_records")
public class SlaRecord extends BaseEntity { ... }

// SlaPauseLog.java
@Entity @Table(name = "sla_pause_logs")
public class SlaPauseLog extends BaseEntity { ... }

// ConfirmationRequest.java
@Entity @Table(name = "confirmation_requests")
public class ConfirmationRequest extends BaseEntity { ... }

// DisputeRecord.java
@Entity @Table(name = "dispute_records")
public class DisputeRecord extends BaseEntity { ... }

// MergeRecord.java
@Entity @Table(name = "merge_records")
public class MergeRecord extends BaseEntity { ... }

// ValidationDecision.java
@Entity @Table(name = "validation_decisions")
public class ValidationDecision extends BaseEntity { ... }

// AuditLog.java
@Entity @Table(name = "audit_logs")
public class AuditLog extends BaseEntity { ... }
```

**Total Inheritance: 22 entity extends BaseEntity** ✅

---

## 3. Polymorphism

### 3.1 Overriding (Run-time Polymorphism)

Semua anggota menerapkan `@Override` pada service implementation:

```java
public interface UserService {
    Optional<User> findByEmail(String email);
}

@Service
public class UserServiceImpl implements UserService {
    @Override  // ← Run-time Polymorphism
    public Optional<User> findByEmail(String email) { ... }
}
```

### 3.2 Overloading per Anggota

#### Orang 1 — Auth & Profil

```java
// UserService.java — 5 overloads
public interface UserService {
    Optional<User> findById(String id);                    // 1 param
    Optional<User> findByEmail(String email);              // 1 param (beda nama field)
    Optional<User> findByPhoneNumber(String phoneNumber);  // 1 param (beda nama field)
    List<User> findByRole(User.Role role);                 // 1 param
    List<User> findByRoleAndStatus(User.Role role,         // 2 param — OVERLOAD
                                    User.AccountStatus status);
}
```

#### Orang 2 — Pelaporan & Notifikasi

```java
// ReportService.java — 2 overloads
public interface ReportService {
    List<Report> getReportsByStatus(ReportStatus status);              // 1 param
    List<Report> getReportsByDateRange(LocalDate start,                // 2 param — OVERLOAD
                                        LocalDate end);
    List<Report> getReportsByStatusAndDateRange(ReportStatus status,   // 3 param — OVERLOAD
                                                  LocalDate start, LocalDate end);
}
```

#### Orang 3 — Disposisi & Eksekusi Lapangan

```java
// FieldTaskService.java — 2 overloads
public interface FieldTaskService {
    List<FieldTask> getTasksByStatus(FieldTask.TaskStatus status);               // 1 param
    List<FieldTask> getTasksByStatusAndOfficer(FieldTask.TaskStatus status,      // 2 param — OVERLOAD
                                                 String officerId);
    List<FieldTask> getTasksByDateRange(LocalDateTime start,                     // 2 param — OVERLOAD
                                         LocalDateTime end);
}
```

#### Orang 4 — SLA, Sengketa & Audit ✅

```java
// AuditLogService.java — 3 overloads (terbanyak!)
public interface AuditLogService {
    // Overload 1: 4 parameter (basic)
    AuditLog log(User actor, String actionType, String oldVal, String newVal);

    // Overload 2: 5 parameter (dengan Report) — OVERLOAD
    AuditLog log(User actor, Report report, String action,
                 String oldVal, String newVal);

    // Overload 3: 8 parameter (lengkap dengan IP + device) — OVERLOAD
    AuditLog log(User actor, String targetType, String targetId, String action,
                 String oldVal, String newVal, String ipAddress, String deviceInfo);
}

// SlaRecordService.java — 2 overloads
public interface SlaRecordService {
    List<SlaRecord> getRecords(SlaStatus status);                    // 1 param
    List<SlaRecord> getRecords(LocalDateTime start,                  // 2 param — OVERLOAD
                                LocalDateTime end);
}
```

### 3.3 Diagram Overloading

```
AuditLogService.log()
├── log(actor, actionType, oldVal, newVal)                          ← 4 param
├── log(actor, report, action, oldVal, newVal)                      ← 5 param (OVERLOAD)
└── log(actor, targetType, targetId, action, oldVal, newVal,
         ipAddress, deviceInfo)                                      ← 8 param (OVERLOAD)

SlaRecordService.getRecords()
├── getRecords(SlaStatus status)                                     ← 1 param
└── getRecords(LocalDateTime start, LocalDateTime end)               ← 2 param (OVERLOAD)

UserService.findBy...()
├── findById(String id)                                              ← 1 param
├── findByEmail(String email)                                        ← 1 param
├── findByPhoneNumber(String phone)                                  ← 1 param
├── findByRole(User.Role role)                                       ← 1 param
└── findByRoleAndStatus(User.Role role, AccountStatus status)        ← 2 param (OVERLOAD)
```

---

## 4. Abstraction

### 4.1 Interface per Anggota

#### Orang 1 — 2 Interface

```java
// Controller hanya inject Interface
@Controller
public class WebController {
    @Autowired private UserService userService;          // ← Interface
    @Autowired private OtpService otpService;            // ← Interface
    // Tidak pernah inject UserRepository atau OtpRepository langsung
}
```

```java
public interface UserService {
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndStatus(User.Role role, User.AccountStatus status);
    User createUser(RegisterDTO dto);
    User updateProfile(String userId, ProfileDTO dto);
    boolean verifyPassword(String rawPassword, String hashedPassword);
}

public interface OtpService {
    OtpVerification generateOtp(String userId, String email);
    boolean verifyOtp(String otpCode, String email);
}
```

#### Orang 2 — 2 Interface

```java
@Controller
public class WebController {
    @Autowired private ReportService reportService;             // ← Interface
    @Autowired private NotificationService notificationService; // ← Interface
}
```

```java
public interface ReportService {
    Optional<Report> findById(String id);
    Optional<Report> findByTicketNumber(String ticketNumber);
    List<Report> getAllReports();
    List<Report> getReportsByStatus(ReportStatus status);
    List<Report> getReportsByWarga(String wargaId);
    List<Report> getReportsByDateRange(LocalDate start, LocalDate end);
    List<Report> getReportsByStatusAndDateRange(ReportStatus status,
                                                  LocalDate start, LocalDate end);
    Report createReport(CreateReportDTO dto, String wargaId);
    Report updateStatus(String reportId, ReportStatus newStatus, String notes, String changedBy);
    Report updateStatus(String reportId, ReportStatus newStatus,
                        String rejectionReason, String adminNotes, String changedBy);
    Report saveReportPhoto(String reportId, String photoBase64);
    long countByStatus(ReportStatus status);
    String generateTicketNumber();
}

public interface NotificationService {
    List<Notification> getNotificationsByUser(String userId);
    List<Notification> getUnreadNotificationsByUser(String userId);
    Notification createNotification(String userId, String title, String message,
                                     String referenceType, String referenceId);
    // + default methods untuk backward compatibility
}
```

#### Orang 3 — 2 Interface (seharusnya 4)

```java
@Controller
public class WebController {
    @Autowired private FieldTaskService fieldTaskService;   // ← Interface
    @Autowired private DispositionService dispositionService; // ← Interface
}
```

> **Catatan:** `AttendanceService` masih berupa **class langsung** (`@Service`), belum di-refactor ke interface + impl. Ini **pelanggaran Abstraction**. Seharusnya ada `AttendanceService` (interface) dan `AttendanceServiceImpl`.

#### Orang 4 — 7 Interface ✅ (terbanyak)

```java
@Controller @RequestMapping("/admin")
public class AdminControllerModul4 {
    @Autowired private DisputeService disputeService;                // ← Interface
    @Autowired private MergeRecordService mergeRecordService;        // ← Interface
    @Autowired private SlaRecordService slaRecordService;            // ← Interface
    @Autowired private SlaMonitoringService slaMonitoringService;    // ← Interface
    @Autowired private AuditLogService auditLogService;              // ← Interface
    @Autowired private ConfirmationService confirmationService;      // ← Interface
    // Semua interface!
}
```

### 4.2 Manfaat Abstraction

| Tanpa Abstraction | Dengan Abstraction |
|-------------------|-------------------|
| `@Autowired UserRepository repo;` | `@Autowired UserService service;` |
| Controller tahu detail database | Controller hanya tahu kontrak |
| Ganti ORM = ganti controller | Ganti ORM = ganti impl saja |
| Testing susah (musti mock repo) | Testing mudah (mock interface) |

---

## 5. Encapsulation

### 5.1 Private Fields (Semua Anggota)

Semua entity di seluruh modul menggunakan field **private**:

```java
@Entity
public class User extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private String userId;          // ← PRIVATE

    @Column(nullable = false, unique = true)
    private String email;           // ← PRIVATE

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;    // ← PRIVATE — tidak boleh ada getter!

    // Public getter & setter
    public String getUserId() { return userId; }
    public void setUserId(String id) { this.userId = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    // Tidak ada getPasswordHash() — ENCAPSULATION!
}
```

### 5.2 DTO (Data Transfer Object) per Anggota

#### Orang 1 — 3 DTO

```java
// LoginDTO.java — memisahkan form login dari entity User
public class LoginDTO {
    private String email;       // ← private
    private String password;    // ← private
    // public getter & setter
}

// RegisterDTO.java — data registrasi terpisah
public class RegisterDTO {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String password;
    private String nik;
}

// ProfileDTO.java — data edit profil
public class ProfileDTO {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String nik;
    private String alamatLengkap;
}
```

#### Orang 2 — 3 DTO

```java
// CreateReportDTO.java
public class CreateReportDTO {
    private String description;         // ← private
    private String locationHint;        // ← private
    private BigDecimal latitude;        // ← private
    private BigDecimal longitude;       // ← private
    private String categoryId;          // ← private
    // public getter & setter
}

// ReportFilterDTO.java
public class ReportFilterDTO {
    private String status;
    private String startDate;
    private String endDate;
    private String keyword;
}

// NotificationDTO.java
public class NotificationDTO {
    private String notificationId;
    private String title;
    private String message;
    private String referenceType;
    private String referenceId;
    private Boolean isRead;
    private LocalDateTime sentAt;
}
```

#### Orang 3 — 2 DTO (seharusnya 3)

```java
// DispositionDTO.java
public class DispositionDTO {
    private String reportId;
    private String agencyId;
    private String notes;
    private String priority;
}

// TaskExecutionDTO.java
public class TaskExecutionDTO {
    private String taskId;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
}
```

> **Catatan:** `AttendanceDTO` belum dibuat — REST API menggunakan entity `OfficerAttendance` langsung di response.

#### Orang 4 — 4 DTO ✅

```java
// DisputeDTO.java
public class DisputeDTO {
    private String reportId;
    private String disputedBy;
    private String reason;
    private String evidenceDescription;
}

// MergeDTO.java
public class MergeDTO {
    private String primaryReportId;
    private String secondaryReportId;
    private String reason;
}

// SlaStatusDTO.java
public class SlaStatusDTO {
    private String reportId;
    private String status;
    private String notes;
}

// AuditLogDTO.java
public class AuditLogDTO {
    private String actionType;    // ← private
    private String reportId;      // ← private
    private String startDate;     // ← private
    private String endDate;       // ← private
    // public getter & setter
}
```

### 5.3 Package-Private Setter (Encapsulation Lanjutan)

Diterapkan oleh **Orang 4** pada `AuditLog`:

```java
// AuditLog.java — ENCAPSULATION tingkat lanjut
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String actionType;     // ← PRIVATE

    // Getter: PUBLIC — semua orang bisa baca
    public String getActionType() { return actionType; }

    // Setter: PACKAGE-PRIVATE (tanpa 'public') — hanya bisa diakses dari package model
    void setActionType(String actionType) { this.actionType = actionType; }
    // BUKAN: public void setActionType(...)

    // Semua setter lainnya juga package-private:
    void setLogId(String logId) { this.logId = logId; }
    void setActor(User actor) { this.actor = actor; }
    void setTargetType(String targetType) { this.targetType = targetType; }
    void setTargetId(String targetId) { this.targetId = targetId; }
    void setOldValue(String oldValue) { this.oldValue = oldValue; }
    void setNewValue(String newValue) { this.newValue = newValue; }
    void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}
```

Dengan Factory class di package yang sama:

```java
// AuditLogFactory.java — satu package dengan AuditLog, bisa akses package-private setter
package com.plr.aduaja.model;  // ← SAME package as AuditLog

public class AuditLogFactory {
    public static AuditLog create(User actor, String actionType, String oldVal, String newVal) {
        AuditLog log = new AuditLog();
        log.setActor(actor);            // ← Bisa akses package-private setter
        log.setActionType(actionType);  // ← Bisa akses package-private setter
        log.setOldValue(oldVal);
        log.setNewValue(newVal);
        log.setIpAddress("0.0.0.0");
        log.setDeviceInfo("System");
        log.setLoggedAt(LocalDateTime.now());
        return log;
    }
    // createWithReport(), createFull() ...
}
```

Alur akses:

```
Service (package: service)          ← TIDAK bisa akses setter langsung
    ↓ delegasi ke Factory
Factory (package: model)            ← BISA akses package-private setter
    ↓ buat object
AuditLog                            ← Object siap pakai
    ↓
Repository.save(auditLog)           ← Simpan ke database
```

---

## 6. Matriks Penerapan Per Anggota

### 6.1 Tabel Lengkap

| Pilar | Indikator | Orang 1 | Orang 2 | Orang 3 | Orang 4 |
|-------|-----------|:-------:|:-------:|:-------:|:-------:|
| **Inheritance** | Jumlah entity extends BaseEntity | **5** | **4** | **5/6** ❌ | **7** ✅ |
| **Polymorphism** | Service interface dengan `@Override` | ✅ 2 impl | ✅ 2 impl | ✅ 2/4 impl | ✅ **7 impl** |
| **Polymorphism** | Overloading (method sama, parameter beda) | `findByRole` (2 versi) | `getReportsBy` (3 versi) | `getTasksBy` (3 versi) | `log()` **(3 versi)**, `getRecords()` **(2 versi)** |
| **Abstraction** | Jumlah Interface | **2** | **2** | **2/4** ❌ | **7** ✅ |
| **Encapsulation** | Jumlah DTO | **3** | **3** | **2/3** ❌ | **4** |
| **Encapsulation** | Package-private setter | ❌ | ❌ | ❌ | ✅ **AuditLog** |

### 6.2 Catatan

| Temuan | Status | Dampak |
|--------|--------|--------|
| `OfficerAttendance` belum extends `BaseEntity` | ❌ Orang 3 | Tidak memiliki `createdAt`/`updatedAt` |
| `AttendanceService` belum interface + impl | ❌ Orang 3 | Controller inject class langsung, violates Abstraction |
| `AttendanceDTO` belum dibuat | ❌ Orang 3 | REST API expose entity langsung ke response |
| `AuditLog` package-private setter | ✅ **Orang 4** | Encapsulation tingkat lanjut — hanya factory yang bisa buat object |
| 7 service interfaces + 7 impls | ✅ **Orang 4** | Abstraction penuh — semua controller inject interface |
| 3 overloads `log()` + 2 overloads `getRecords()` | ✅ **Orang 4** | Polymorphism overloading terbanyak |

### 6.3 Total Keseluruhan

| Pilar | Total |
|-------|:-----:|
| **Inheritance** | 22 entity extends BaseEntity |
| **Polymorphism (Overriding)** | 13 service implementations with `@Override` |
| **Polymorphism (Overloading)** | 10+ overloaded methods across all modules |
| **Abstraction** | 13 service interfaces |
| **Encapsulation (DTO)** | 12 DTOs across all modules |
| **Encapsulation (Package-Private)** | 1 entity (AuditLog) |

---

## Lampiran: File Referensi

### Inheritance — extends BaseEntity

```
src/main/java/com/plr/aduaja/model/
├── BaseEntity.java                        ← Parent (Orang 1)
├── User.java                    extends   ← Orang 1
├── UserProfile.java            extends   ← Orang 1
├── LoginAttempt.java           extends   ← Orang 1
├── OtpVerification.java        extends   ← Orang 1
├── ActiveSession.java          extends   ← Orang 1
├── Report.java                 extends   ← Orang 2
├── ReportCategory.java         extends   ← Orang 2
├── ReportRevision.java         extends   ← Orang 2
├── Notification.java           extends   ← Orang 2
├── Disposition.java            extends   ← Orang 3
├── Agency.java                 extends   ← Orang 3
├── FieldTask.java              extends   ← Orang 3
├── TaskEvidence.java           extends   ← Orang 3
├── TaskPostponement.java       extends   ← Orang 3
├── OfficerAttendance.java      BELUM     ← ❌ Orang 3
├── SlaRecord.java              extends   ← Orang 4
├── SlaPauseLog.java            extends   ← Orang 4
├── ConfirmationRequest.java    extends   ← Orang 4
├── DisputeRecord.java          extends   ← Orang 4
├── MergeRecord.java            extends   ← Orang 4
├── ValidationDecision.java     extends   ← Orang 4
└── AuditLog.java               extends   ← Orang 4
```

### Polymorphism — Service Override

```
src/main/java/com/plr/aduaja/service/
├── UserServiceImpl       implements UserService         ← Orang 1
├── OtpServiceImpl        implements OtpService          ← Orang 1
├── ReportServiceImpl     implements ReportService       ← Orang 2
├── NotificationServiceImpl implements NotificationService ← Orang 2
├── FieldTaskServiceImpl  implements FieldTaskService    ← Orang 3
├── DispositionServiceImpl implements DispositionService ← Orang 3
├── SlaRecordServiceImpl  implements SlaRecordService    ← Orang 4
├── SlaMonitoringServiceImpl implements SlaMonitoringService ← Orang 4
├── DisputeServiceImpl    implements DisputeService      ← Orang 4
├── MergeRecordServiceImpl implements MergeRecordService ← Orang 4
├── ConfirmationServiceImpl implements ConfirmationService ← Orang 4
├── ValidationDecisionServiceImpl implements ValidationDecisionService ← Orang 4
└── AuditLogServiceImpl   implements AuditLogService     ← Orang 4
```

### Abstraction — Interface

```
src/main/java/com/plr/aduaja/service/
├── UserService.java              ← Orang 1
├── OtpService.java               ← Orang 1
├── ReportService.java            ← Orang 2
├── NotificationService.java      ← Orang 2
├── FieldTaskService.java         ← Orang 3
├── DispositionService.java       ← Orang 3
├── SlaRecordService.java         ← Orang 4
├── SlaMonitoringService.java     ← Orang 4
├── DisputeService.java           ← Orang 4
├── MergeRecordService.java       ← Orang 4
├── ConfirmationService.java      ← Orang 4
├── ValidationDecisionService.java ← Orang 4
└── AuditLogService.java          ← Orang 4
```

### Encapsulation — DTO

```
src/main/java/com/plr/aduaja/dto/
├── LoginDTO.java                  ← Orang 1
├── RegisterDTO.java               ← Orang 1
├── ProfileDTO.java                ← Orang 1
├── CreateReportDTO.java           ← Orang 2
├── ReportFilterDTO.java           ← Orang 2
├── NotificationDTO.java           ← Orang 2
├── DispositionDTO.java            ← Orang 3
├── TaskExecutionDTO.java          ← Orang 3
├── DisputeDTO.java                ← Orang 4
├── MergeDTO.java                  ← Orang 4
├── SlaStatusDTO.java              ← Orang 4
└── AuditLogDTO.java               ← Orang 4
```
