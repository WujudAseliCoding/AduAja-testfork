# PROMPT UNTUK ANGGOTA 2 — MODUL PELAPORAN & NOTIFIKASI
## Sistem AduAja — Spring Boot Monolithic Application

> File ini adalah prompt lengkap yang bisa diberikan ke AI lain
> untuk menyelesaikan SEMUA tugas Anggota 2 (Modul Pelaporan & Notifikasi).
> Format mengikuti template dari PROMPT_ANGGOTA_1_MODUL_AUTH.md.

---

## BAGIAN 1: CONTEXT & BACKGROUND

### 1.1 Tentang Proyek

```
Nama Proyek: AduAja — Sistem Informasi Pengaduan dan Aspirasi Masyarakat
Framework: Spring Boot 4.x + Spring Data JPA + Thymeleaf + Lombok + H2 Database
Arsitektur: MVC (Model-View-Controller) Monolithic
Jumlah Entity: 23 entities
Jumlah Developer: 4 orang (vertical slicing per modul)
Database: H2 file-based (./data/aduaja)
```

### 1.2 Struktur Folder Proyek

```
src/main/java/com/plr/aduaja/
├── AduAjaApplication.java          ← Main class
├── model/                          ← Entity (JPA) — WAJIB extends BaseEntity
├── repository/                     ← JPA Repository
├── service/                        ← Service Layer (Interface + Impl)
├── controller/                     ← Web Controller (MVC)
├── dto/                            ← Data Transfer Object (HARUS ADA)
└── config/                         ← Configuration

src/main/resources/
├── templates/                      ← Thymeleaf HTML
│   ├── layouts/master.html        ← Template utama
│   ├── warga/                      ← Halaman Warga
│   ├── admin/                      ← Halaman Admin
│   └── petugas/                    ← Halaman Petugas
└── application.properties          ← Konfigurasi
```

### 1.3 Database Configuration (sudah ada, jangan diubah)

```properties
spring.datasource.url=jdbc:h2:file:./data/aduaja
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### 1.4 Role dalam Sistem

```java
public enum Role {
    WARGA,       // Warga biasa
    ADMIN_PUSAT, // Admin pusat (validasi laporan)
    ADMIN_DINAS, // Admin dinas
    PETUGAS      // Petugas lapangan
}

public enum AccountStatus {
    PENDING,  // Belum aktif (harus OTP verify)
    ACTIVE,   // Aktif
    SUSPENDED // Ditangguhkan
}
```

### 1.5 Shared Entity yang SUDAH ADA (jangan diubah)

```java
// BaseEntity.java — INHERITANCE PARENT (dibuat Orang 1)
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

// User.java — SUDAH ADA oleh Orang 1 (jangan diubah, hanya baca)
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private String userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.PENDING;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile userProfile;

    public enum Role { WARGA, ADMIN_PUSAT, ADMIN_DINAS, PETUGAS }
    public enum AccountStatus { PENDING, ACTIVE, SUSPENDED }

    // Getter & Setter (private fields — ENCAPSULATION)
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }
}
```

---

## BAGIAN 2: TUGAS UTAMA ANGGOTA 2

### 2.1 Deskripsi Modul

**Modul Pelaporan & Notifikasi** menangani:
1. Warga membuat laporan baru (dengan foto, lokasi GPS, kategori)
2. Riwayat laporan warga (filter status, search)
3. Detail laporan warga (timeline status, info lengkap)
4. Dashboard warga (statistik laporan milik sendiri)
5. Notifikasi untuk warga (update status, pesan)
6. Admin validasi laporan masuk (terima/tolak/revisi)
7. Antrean laporan untuk admin (daftar + detail)

### 2.2 Entity yang Harus Dibuat (4 files)

| No | File | extends BaseEntity | Deskripsi |
|----|------|:-----------------:|-----------|
| 1 | `Report.java` | ✅ WAJIB | Data laporan warga |
| 2 | `ReportCategory.java` | ✅ WAJIB | Kategori laporan (jalan, lampu, dll) |
| 3 | `ReportRevision.java` | ✅ WAJIB | Riwayat revisi/perubahan laporan |
| 4 | `Notification.java` | ✅ WAJIB | Notifikasi untuk warga |

### 2.3 Repository yang Harus Dibuat (4 files)

| No | File | extends JpaRepository |
|----|------|:--------------------:|
| 1 | `ReportRepository.java` | ✅ JpaRepository<Report, String> |
| 2 | `ReportCategoryRepository.java` | ✅ JpaRepository<ReportCategory, String> |
| 3 | `ReportRevisionRepository.java` | ✅ JpaRepository<ReportRevision, String> |
| 4 | `NotificationRepository.java` | ✅ JpaRepository<Notification, String> |

### 2.4 Service yang Harus Dibuat (2 Interface + 2 Impl = 4 files)

| No | Interface | Implementation | Polymorphism |
|----|-----------|----------------|:------------:|
| 1 | `ReportService.java` | `ReportServiceImpl.java` | ✅ @Override + Overload |
| 2 | `NotificationService.java` | `NotificationServiceImpl.java` | ✅ @Override + Overload |

### 2.5 DTO yang Harus Dibuat (3 files)

| No | File | Untuk Form |
|----|------|-----------|
| 1 | `CreateReportDTO.java` | Form buat laporan baru |
| 2 | `ReportFilterDTO.java` | Form filter/search laporan |
| 3 | `NotificationDTO.java` | Data notifikasi |

### 2.6 View HTML yang Harus Dibuat/Diedit (5 files)

| No | File | Deskripsi |
|----|------|-----------|
| 1 | `templates/warga/create-report.html` | Halaman buat laporan baru |
| 2 | `templates/warga/report-history.html` | Daftar riwayat laporan |
| 3 | `templates/warga/report-detail.html` | Detail satu laporan |
| 4 | `templates/warga/notifications.html` | Daftar notifikasi warga |
| 5 | `templates/warga/dashboard.html` | Dashboard warga (statistik) |

### 2.7 Route/Endpoint yang Harus Ditambahkan

```
WARGA:
GET  /warga/create-report        → Form buat laporan baru
POST /warga/create-report        → Simpan laporan baru ke DB
GET  /warga/report-history       → Daftar semua laporan warga
GET  /warga/report-detail        → Detail satu laporan (by id)
GET  /warga/notifications        → Daftar notifikasi warga
POST /warga/notifications/mark-read → Tandai notifikasi sudah dibaca

ADMIN:
GET  /admin/validation           → Panel validasi laporan masuk
POST /admin/validation           → Proses validasi (terima/tolak/revisi)
GET  /admin/laporan-queue        → Antrean laporan
GET  /admin/queue-detail         → Detail laporan di antrean
```

---

## BAGIAN 3: INHERITANCE — extends BaseEntity (WAJIB)

### 3.1 BaseEntity (sudah dibuat Orang 1, tinggal extends)

Lihat Bagian 1.5 untuk kode lengkap `BaseEntity.java`.

### 3.2 Report.java extends BaseEntity

```java
@Entity
@Table(name = "reports")
public class Report extends BaseEntity {  // ← INHERITANCE

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id")
    private String reportId;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;  // HAS-A (Composition) ≠ Inheritance

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ReportCategory category;  // HAS-A (Association)

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "location_hint")
    private String locationHint;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "photo_base64", columnDefinition = "TEXT")
    private String photoBase64;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.MENUNGGU_VALIDASI;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportRevision> revisions = new ArrayList<>();  // HAS-A (Composition)

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public enum ReportStatus {
        MENUNGGU_VALIDASI, PERLU_REVISI, DITOLAK, DIVALIDASI,
        DIDISPOSISI, DITUGASKAN, SEDANG_DIKERJAKAN, TERTUNDA,
        MENUNGGU_KONFIRMASI, SELESAI, SENGKETA, DITUTUP
    }

    // Getter & Setter — ENCAPSULATION: semua field PRIVATE
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public ReportCategory getCategory() { return category; }
    public void setCategory(ReportCategory category) { this.category = category; }
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
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public List<ReportRevision> getRevisions() { return revisions; }
    public void setRevisions(List<ReportRevision> revisions) { this.revisions = revisions; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
```

### 3.3 ReportCategory.java extends BaseEntity

```java
@Entity
@Table(name = "report_categories")
public class ReportCategory extends BaseEntity {  // ← INHERITANCE

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id")
    private String categoryId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_name")
    private String iconName;

    @Column(nullable = false)
    private Boolean isActive = true;

    // Getter & Setter — ENCAPSULATION
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
```

### 3.4 ReportRevision.java extends BaseEntity

```java
@Entity
@Table(name = "report_revisions")
public class ReportRevision extends BaseEntity {  // ← INHERITANCE

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

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    // Getter & Setter — ENCAPSULATION
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
}
```

### 3.5 Notification.java extends BaseEntity

```java
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {  // ← INHERITANCE

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private String notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // HAS-A (ManyToOne)

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_type", length = 50)
    private String referenceType;  // "REPORT", "VALIDATION", etc.

    @Column(name = "reference_id")
    private String referenceId;  // ID laporan terkait

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    // Getter & Setter — ENCAPSULATION
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
```

---

## BAGIAN 4: POLYMORPHISM — @Override + Overloading (WAJIB)

### 4.1 ReportService.java (Interface)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.dto.CreateReportDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportService {

    // ===========================
    // OVERLOADING (Compile-time Polymorphism)
    // Method sama, parameter beda
    // ===========================

    Optional<Report> findById(String id);
    Optional<Report> findByTicketNumber(String ticketNumber);
    List<Report> getAllReports();
    List<Report> getReportsByStatus(Report.ReportStatus status);
    List<Report> getReportsByWarga(String wargaId);
    List<Report> getReportsByDateRange(LocalDate start, LocalDate end);        // OVERLOAD
    List<Report> getReportsByStatusAndDateRange(Report.ReportStatus status,    // OVERLOAD
                                                  LocalDate start, LocalDate end);

    // ===========================
    // METHOD LAIN
    // ===========================

    Report createReport(CreateReportDTO dto, String wargaId);
    Report updateStatus(String reportId, Report.ReportStatus newStatus, String notes, String changedBy);
    Report updateStatus(String reportId, Report.ReportStatus newStatus, String rejectionReason, String adminNotes, String changedBy);
    Report saveReportPhoto(String reportId, String photoBase64);
    long countByStatus(Report.ReportStatus status);
    String generateTicketNumber();
}
```

### 4.2 ReportServiceImpl.java (@Override)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.ReportRevision;
import com.plr.aduaja.model.ReportCategory;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.ReportCategoryRepository;
import com.plr.aduaja.repository.ReportRevisionRepository;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.dto.CreateReportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {  // ← Polymorphism

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportCategoryRepository categoryRepository;

    @Autowired
    private ReportRevisionRepository revisionRepository;

    @Autowired
    private UserRepository userRepository;

    // ===========================
    // @Override — Run-time Polymorphism
    // ===========================

    @Override  // ← POLYMORPHISM
    public Optional<Report> findById(String id) {
        return reportRepository.findById(id);
    }

    @Override  // ← POLYMORPHISM
    public Optional<Report> findByTicketNumber(String ticketNumber) {
        return reportRepository.findByTicketNumber(ticketNumber);
    }

    @Override  // ← POLYMORPHISM
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @Override  // ← POLYMORPHISM
    public List<Report> getReportsByStatus(Report.ReportStatus status) {
        return reportRepository.findByStatus(status);
    }

    @Override  // ← POLYMORPHISM
    public List<Report> getReportsByWarga(String wargaId) {
        User user = new User();
        user.setUserId(wargaId);
        return reportRepository.findByReporterOrderBySubmittedAtDesc(user);
    }

    @Override  // ← POLYMORPHISM (Overload: 2 parameter)
    public List<Report> getReportsByDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(LocalTime.MAX);
        return reportRepository.findBySubmittedAtBetween(startDt, endDt);
    }

    @Override  // ← POLYMORPHISM (Overload: 3 parameter beda)
    public List<Report> getReportsByStatusAndDateRange(Report.ReportStatus status,
                                                         LocalDate start,
                                                         LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(LocalTime.MAX);
        return reportRepository.findByStatusAndSubmittedAtBetween(status, startDt, endDt);
    }

    @Override  // ← POLYMORPHISM
    public Report createReport(CreateReportDTO dto, String wargaId) {
        User reporter = userRepository.findById(wargaId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        Report report = new Report();
        report.setTicketNumber(generateTicketNumber());
        report.setReporter(reporter);
        report.setDescription(dto.getDescription());
        report.setLocationHint(dto.getLocationHint());
        report.setLatitude(dto.getLatitude());
        report.setLongitude(dto.getLongitude());
        report.setPhotoBase64(dto.getPhotoBase64());
        report.setSubmittedAt(LocalDateTime.now());
        report.setStatus(Report.ReportStatus.MENUNGGU_VALIDASI);

        if (dto.getCategoryId() != null && !dto.getCategoryId().isBlank()) {
            categoryRepository.findById(dto.getCategoryId())
                .ifPresent(report::setCategory);
        }

        return reportRepository.save(report);
    }

    @Override  // ← POLYMORPHISM
    public Report updateStatus(String reportId, Report.ReportStatus newStatus,
                                String notes, String changedBy) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));

        Report.ReportStatus oldStatus = report.getStatus();

        if (notes != null) {
            report.setAdminNotes(notes);
        }
        report.setStatus(newStatus);

        Report saved = reportRepository.save(report);

        createRevision(saved, oldStatus, newStatus, notes, changedBy);

        return saved;
    }

    @Override  // ← POLYMORPHISM (Overload: 4 parameter)
    public Report updateStatus(String reportId, Report.ReportStatus newStatus,
                                String rejectionReason, String adminNotes,
                                String changedBy) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));

        Report.ReportStatus oldStatus = report.getStatus();
        report.setStatus(newStatus);
        report.setRejectionReason(rejectionReason);
        report.setAdminNotes(adminNotes);

        Report saved = reportRepository.save(report);

        createRevision(saved, oldStatus, newStatus, rejectionReason, changedBy);

        return saved;
    }

    @Override  // ← POLYMORPHISM
    public Report saveReportPhoto(String reportId, String photoBase64) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));
        report.setPhotoBase64(photoBase64);
        return reportRepository.save(report);
    }

    @Override  // ← POLYMORPHISM
    public long countByStatus(Report.ReportStatus status) {
        return reportRepository.countByStatus(status);
    }

    @Override  // ← POLYMORPHISM
    public String generateTicketNumber() {
        return "RPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ===========================
    // PRIVATE HELPER
    // ===========================

    private void createRevision(Report report, Report.ReportStatus oldStatus,
                                 Report.ReportStatus newStatus,
                                 String notes, String changedBy) {
        ReportRevision revision = new ReportRevision();
        revision.setReport(report);
        revision.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        revision.setNewStatus(newStatus.name());
        revision.setNotes(notes);
        revision.setChangedBy(changedBy);
        revision.setChangedAt(LocalDateTime.now());
        revisionRepository.save(revision);
    }
}
```

### 4.3 NotificationService.java (Interface)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.Notification;
import java.util.List;

public interface NotificationService {

    // OVERLOADING: method sama, parameter beda
    List<Notification> getNotificationsByUser(String userId);
    List<Notification> getUnreadNotificationsByUser(String userId);     // OVERLOAD
    List<Notification> getNotificationsByType(String userId, String referenceType);  // OVERLOAD

    Notification createNotification(String userId, String title, String message,
                                     String referenceType, String referenceId);
    Notification markAsRead(String notificationId);
    int markAllAsReadByUser(String userId);
    long countUnreadByUser(String userId);
}
```

### 4.4 NotificationServiceImpl.java (@Override)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.Notification;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.NotificationRepository;
import com.plr.aduaja.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {  // ← Polymorphism

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Override  // ← POLYMORPHISM
    public List<Notification> getNotificationsByUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        return notificationRepository.findByUserOrderBySentAtDesc(user);
    }

    @Override  // ← POLYMORPHISM (Overload)
    public List<Notification> getUnreadNotificationsByUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        return notificationRepository.findByUserAndIsReadOrderBySentAtDesc(user, false);
    }

    @Override  // ← POLYMORPHISM (Overload)
    public List<Notification> getNotificationsByType(String userId, String referenceType) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        return notificationRepository.findByUserAndReferenceTypeOrderBySentAtDesc(user, referenceType);
    }

    @Override  // ← POLYMORPHISM
    public Notification createNotification(String userId, String title, String message,
                                            String referenceType, String referenceId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        Notification notif = new Notification();
        notif.setUser(user);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setReferenceType(referenceType);
        notif.setReferenceId(referenceId);
        notif.setIsRead(false);
        notif.setSentAt(LocalDateTime.now());

        return notificationRepository.save(notif);
    }

    @Override  // ← POLYMORPHISM
    public Notification markAsRead(String notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notifikasi tidak ditemukan"));
        notif.setIsRead(true);
        return notificationRepository.save(notif);
    }

    @Override  // ← POLYMORPHISM
    public int markAllAsReadByUser(String userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override  // ← POLYMORPHISM
    public long countUnreadByUser(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }
}
```

---

## BAGIAN 5: ABSTRACTION — Interface sebagai Kontrak

### 5.1 Prinsip Abstraction

```
CONTROLLER (Pemanggil)
    ↓
SERVICE INTERFACE (Kontrak — mendefinisikan APA yang dilakukan)
    ↓
SERVICE IMPL (Implementasi — mendefinisikan BAGAIMANA dilakukan)
    ↓
REPOSITORY (Akses database)
```

### 5.2 Contoh Alur Abstraction di Controller

```java
// CONTROLLER — Tidak tahu implementasi
@Controller
public class WebController {

    @Autowired
    private ReportService reportService;  // ← Abstraction: hanya tahu interface

    @PostMapping("/warga/create-report")
    public String createReport(@ModelAttribute CreateReportDTO dto,
                                HttpSession session,
                                Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/warga/login";
        }

        try {
            // Abstraction: Controller tidak tahu:
            // - Bagaimana ticket number di-generate
            // - Bagaimana geolocation di-attach
            // - Bagaimana ReportRevision dibuat
            // - Bagaimana notification dikirim
            // Semua DISEMBUNYIKAN di ReportServiceImpl

            Report report = reportService.createReport(dto, userId);

            return "redirect:/warga/report-detail?id=" + report.getReportId();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "warga/create-report";
        }
    }
}
```

---

## BAGIAN 6: ENCAPSULATION — Private + DTO + Immutable

### 6.1 Prinsip Encapsulation

```
1. SEMUA field di Entity = PRIVATE
2. SEMUA form input = pakai DTO (bukan Entity langsung)
3. Objek sensitif = IMMUTABLE (tidak ada setter setelah creation)
```

### 6.2 CreateReportDTO.java

```java
package com.plr.aduaja.dto;

import java.math.BigDecimal;

// ENCAPSULATION: Semua field PRIVATE
public class CreateReportDTO {
    private String description;
    private String locationHint;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String photoBase64;
    private String categoryId;

    // ENCAPSULATION: Hanya getter & setter
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
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
}
```

### 6.3 ReportFilterDTO.java

```java
package com.plr.aduaja.dto;

// ENCAPSULATION: Semua field PRIVATE
public class ReportFilterDTO {
    private String status;
    private String searchQuery;
    private String categoryId;
    private String startDate;
    private String endDate;

    // ENCAPSULATION: Hanya getter & setter
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
```

### 6.4 NotificationDTO.java

```java
package com.plr.aduaja.dto;

import java.time.LocalDateTime;

// ENCAPSULATION: Semua field PRIVATE
public class NotificationDTO {
    private String notificationId;
    private String title;
    private String message;
    private String referenceType;
    private String referenceId;
    private Boolean isRead;
    private LocalDateTime sentAt;

    // ENCAPSULATION: Hanya getter & setter
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
```

---

## BAGIAN 7: REPOSITORY (4 files)

### 7.1 ReportRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {

    Optional<Report> findByTicketNumber(String ticketNumber);
    List<Report> findByReporterOrderBySubmittedAtDesc(User reporter);
    List<Report> findByStatus(Report.ReportStatus status);
    List<Report> findByStatusAndSubmittedAtBetween(Report.ReportStatus status,
                                                     LocalDateTime start, LocalDateTime end);
    List<Report> findBySubmittedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Report> findByDescriptionContainingIgnoreCaseOrLocationHintContainingIgnoreCase(
            String description, String locationHint);
    long countByStatus(Report.ReportStatus status);
    long countByReporter(User reporter);
}
```

### 7.2 ReportCategoryRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.ReportCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportCategoryRepository extends JpaRepository<ReportCategory, String> {

    Optional<ReportCategory> findByName(String name);
    List<ReportCategory> findByIsActiveTrue();
    boolean existsByName(String name);
}
```

### 7.3 ReportRevisionRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.ReportRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRevisionRepository extends JpaRepository<ReportRevision, String> {

    List<ReportRevision> findByReportOrderByChangedAtDesc(Report report);
    List<ReportRevision> findByChangedBy(String changedBy);
    long countByReport(Report report);
}
```

### 7.4 NotificationRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.Notification;
import com.plr.aduaja.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserOrderBySentAtDesc(User user);
    List<Notification> findByUserAndIsReadOrderBySentAtDesc(User user, Boolean isRead);
    List<Notification> findByUserAndReferenceTypeOrderBySentAtDesc(User user, String referenceType);
    long countByUserIdAndIsRead(String userId, Boolean isRead);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") String userId);
}
```

---

## BAGIAN 8: VIEW HTML (Thymeleaf)

### 8.1 warga/dashboard.html

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Dashboard Warga</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50">
    <!-- Header -->
    <header class="bg-white shadow-sm sticky top-0 z-10">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex justify-between items-center h-16">
                <div class="flex items-center gap-2">
                    <h1 class="text-2xl font-extrabold text-gray-900">AduAja</h1>
                </div>
                <div class="flex items-center gap-4">
                    <a th:href="@{/warga/notifications}" class="relative p-2 text-gray-600 hover:bg-gray-100 rounded-lg">
                        <i data-lucide="bell" class="w-5 h-5"></i>
                        <span th:if="${unreadCount > 0}"
                              class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center font-bold"
                              th:text="${unreadCount}">0</span>
                    </a>
                    <div class="h-8 w-px bg-gray-300"></div>
                    <a th:href="@{/warga/profile}"
                       class="flex items-center gap-2 hover:bg-gray-100 rounded-lg p-1">
                        <div class="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                            <i data-lucide="user" class="w-5 h-5 text-blue-600"></i>
                        </div>
                        <div class="text-left">
                            <p class="text-sm font-medium text-gray-900" th:text="${user.fullName}">Nama</p>
                            <p class="text-xs text-gray-500">Warga</p>
                        </div>
                    </a>
                </div>
            </div>
        </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <!-- Welcome -->
        <div class="mb-8">
            <h2 class="text-2xl sm:text-3xl font-bold text-gray-900 mb-2"
                th:text="|Selamat Datang, ${user.fullName}|">
                Selamat Datang
            </h2>
            <p class="text-gray-500">Pantau dan kelola laporan Anda</p>
        </div>

        <!-- Quick Action -->
        <a th:href="@{/warga/create-report}"
           class="block mb-8 p-6 bg-gradient-to-r from-blue-600 to-blue-700 rounded-xl text-white hover:shadow-lg transition-shadow">
            <div class="flex items-center gap-4">
                <div class="w-14 h-14 bg-white/20 rounded-full flex items-center justify-center">
                    <i data-lucide="plus" class="w-7 h-7"></i>
                </div>
                <div>
                    <h3 class="text-lg font-bold">Buat Laporan Baru</h3>
                    <p class="text-blue-100 text-sm">Laporkan kerusakan infrastruktur di sekitar Anda</p>
                </div>
            </div>
        </a>

        <!-- Stats -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
            <div th:each="stat : ${stats}" class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
                <p class="text-2xl font-bold text-gray-900" th:text="${stat.count}">0</p>
                <p class="text-sm text-gray-500" th:text="${stat.label}">Label</p>
            </div>
        </div>

        <!-- Recent Reports -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <div class="flex items-center justify-between mb-4">
                <h3 class="font-semibold text-gray-900">Laporan Terbaru</h3>
                <a th:href="@{/warga/report-history}" class="text-sm text-blue-600 hover:underline">Lihat Semua</a>
            </div>

            <div th:if="${#lists.isEmpty(recentReports)}" class="text-center py-8 text-gray-400">
                <i data-lucide="file-text" class="w-12 h-12 mx-auto mb-3"></i>
                <p class="text-sm">Belum ada laporan</p>
            </div>

            <div th:if="${!#lists.isEmpty(recentReports)}" class="space-y-3">
                <div th:each="report : ${recentReports}"
                     class="flex items-center justify-between p-4 border border-gray-100 rounded-lg hover:bg-gray-50">
                    <div class="min-w-0 flex-1">
                        <div class="flex items-center gap-2 mb-1">
                            <span class="font-mono text-xs font-semibold text-blue-600"
                                  th:text="${report.ticketNumber}">RPT-001</span>
                            <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                                  th:class="${report.statusColor}"
                                  th:text="${report.statusDisplay}">Status</span>
                        </div>
                        <p class="text-sm font-medium text-gray-900 truncate"
                           th:text="${report.descriptionShort}">Deskripsi</p>
                        <p class="text-xs text-gray-500 mt-1"
                           th:text="${report.dateFormatted}">Tanggal</p>
                    </div>
                    <a th:href="@{|/warga/report-detail?id=${report.reportId}|}"
                       class="flex-shrink-0 ml-4 text-blue-600 hover:text-blue-900">
                        <i data-lucide="chevron-right" class="w-5 h-5"></i>
                    </a>
                </div>
            </div>
        </div>
    </main>
</div>
```

### 8.2 warga/create-report.html (UPDATE dari existing)

Update form existing dengan binding `th:object` dan method post ke `/warga/create-report`:

```html
<!-- Bagian form yang perlu diupdate -->
<form th:action="@{/warga/create-report}"
      th:object="${createReportDTO}"
      method="post"
      enctype="multipart/form-data"
      class="space-y-6">

    <!-- Category -->
    <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <label class="block text-sm font-medium text-gray-900 mb-2">
            Kategori Laporan <span class="text-red-500">*</span>
        </label>
        <select th:field="*{categoryId}" class="..." required>
            <option value="">Pilih kategori</option>
            <option th:each="cat : ${categories}"
                    th:value="${cat.categoryId}"
                    th:text="${cat.name}">Kategori</option>
        </select>
    </div>

    <!-- Description -->
    <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <label class="block text-sm font-medium text-gray-900 mb-2">
            Deskripsi Kerusakan <span class="text-red-500">*</span>
        </label>
        <textarea th:field="*{description}" rows="5" class="..."
                  placeholder="Jelaskan detail kerusakan (minimal 20 karakter)" required></textarea>
    </div>

    <!-- Location -->
    <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <label class="block text-sm font-medium text-gray-900 mb-2">
            Lokasi <span class="text-red-500">*</span>
        </label>
        <div class="mb-4">
            <input type="text" th:field="*{locationHint}" class="..."
                   placeholder="Contoh: Dekat Indomaret" />
        </div>
        <div class="grid grid-cols-2 gap-3">
            <div>
                <p class="text-gray-600 text-sm">Latitude:</p>
                <input type="text" th:field="*{latitude}" class="..." readonly />
            </div>
            <div>
                <p class="text-gray-600 text-sm">Longitude:</p>
                <input type="text" th:field="*{longitude}" class="..." readonly />
            </div>
        </div>
    </div>

    <!-- Hidden photo data -->
    <input type="hidden" th:field="*{photoBase64}" />

    <button type="submit" class="w-full bg-blue-600 text-white py-4 rounded-lg font-medium hover:bg-blue-700">
        Kirim Laporan
    </button>
</form>
```

### 8.3 warga/report-history.html

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Riwayat Laporan</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50">
    <header class="bg-white shadow-sm sticky top-0 z-10">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex items-center gap-3 h-16">
                <a th:href="@{/warga/dashboard}" class="p-2 hover:bg-gray-100 rounded-lg">
                    <i data-lucide="arrow-left" class="w-5 h-5 text-gray-600"></i>
                </a>
                <div>
                    <h1 class="font-bold text-gray-900">Riwayat Laporan</h1>
                    <p class="text-xs text-gray-500">Semua laporan yang pernah Anda buat</p>
                </div>
            </div>
        </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <!-- Filter & Search -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-6">
            <form method="get" class="flex flex-wrap gap-3">
                <input type="text" name="q" th:value="${searchQuery}"
                       class="flex-1 min-w-[200px] px-4 py-2 border border-gray-300 rounded-lg"
                       placeholder="Cari berdasarkan ID atau judul..." />
                <select name="status" class="px-4 py-2 border border-gray-300 rounded-lg">
                    <option value="">Semua Status</option>
                    <option th:each="s : ${statusOptions}"
                            th:value="${s}"
                            th:selected="${s == filterStatus}"
                            th:text="${s}">Status</option>
                </select>
                <button type="submit" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                    Cari
                </button>
            </form>
        </div>

        <!-- Status Counts -->
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
            <div th:each="entry : ${statusCounts}"
                 class="bg-white rounded-lg shadow-sm border border-gray-100 p-3 text-center">
                <p class="text-lg font-bold text-gray-900" th:text="${entry.value}">0</p>
                <p class="text-xs text-gray-500" th:text="${entry.key}">Label</p>
            </div>
        </div>

        <!-- Report List -->
        <div th:if="${#lists.isEmpty(reports)}" class="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
            <i data-lucide="inbox" class="w-16 h-16 mx-auto mb-4 text-gray-300"></i>
            <p class="text-gray-500">Belum ada laporan</p>
            <a th:href="@{/warga/create-report}" class="mt-4 inline-block text-blue-600 hover:underline">Buat laporan sekarang</a>
        </div>

        <div th:if="${!#lists.isEmpty(reports)}" class="space-y-3">
            <div th:each="report : ${reports}"
                 class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 hover:shadow-md transition-shadow">
                <a th:href="@{|/warga/report-detail?id=${report.reportId}|}" class="block">
                    <div class="flex items-start justify-between gap-4">
                        <div class="min-w-0 flex-1">
                            <div class="flex items-center gap-2 mb-1">
                                <span class="font-mono text-sm font-semibold text-blue-600"
                                      th:text="${report.ticketNumber}">RPT-001</span>
                                <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                                      th:class="${report.statusColor}"
                                      th:text="${report.statusDisplay}">Status</span>
                            </div>
                            <p class="font-medium text-gray-900 line-clamp-2"
                               th:text="${report.descriptionShort}">Deskripsi</p>
                            <p class="text-xs text-gray-500 mt-1"
                               th:text="${report.dateFormatted}">Tanggal</p>
                        </div>
                        <i data-lucide="chevron-right" class="w-5 h-5 text-gray-400 flex-shrink-0 mt-2"></i>
                    </div>
                </a>
            </div>
        </div>
    </main>
</div>
```

### 8.4 warga/report-detail.html

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Detail Laporan</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50">
    <header class="bg-white shadow-sm sticky top-0 z-10">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex items-center gap-3 h-16">
                <a th:href="@{/warga/report-history}" class="p-2 hover:bg-gray-100 rounded-lg">
                    <i data-lucide="arrow-left" class="w-5 h-5 text-gray-600"></i>
                </a>
                <div>
                    <h1 class="font-bold text-gray-900">Detail Laporan</h1>
                    <p class="text-xs text-gray-500" th:text="${report.ticketNumber}">RPT-001</p>
                </div>
            </div>
        </div>
    </header>

    <main class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        <!-- Status Badge -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <div class="flex items-center justify-between mb-4">
                <div>
                    <span class="text-sm px-3 py-1 rounded-full font-semibold"
                          th:class="${report.statusColor}"
                          th:text="${report.statusDisplay}">Status</span>
                </div>
                <span class="text-sm text-gray-500" th:text="${report.dateFormatted}">Tanggal</span>
            </div>
            <h2 class="text-xl font-bold text-gray-900" th:text="${report.descriptionShort}">Judul</h2>
            <p class="text-gray-600 mt-2" th:text="${report.description}">Detail deskripsi</p>
        </div>

        <!-- Location -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 class="font-semibold text-gray-900 mb-3">Lokasi</h3>
            <p class="text-gray-600" th:text="${report.locationHint}">Lokasi</p>
            <div class="grid grid-cols-2 gap-4 mt-3">
                <div>
                    <p class="text-xs text-gray-500">Latitude</p>
                    <p class="font-mono text-sm" th:text="${report.latitude}">0.0</p>
                </div>
                <div>
                    <p class="text-xs text-gray-500">Longitude</p>
                    <p class="font-mono text-sm" th:text="${report.longitude}">0.0</p>
                </div>
            </div>
        </div>

        <!-- Photo -->
        <div th:if="${report.photoBase64 != null}" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 class="font-semibold text-gray-900 mb-3">Foto Bukti</h3>
            <img th:src="${report.photoBase64}" alt="Foto laporan" class="w-full max-w-md rounded-lg" />
        </div>

        <!-- Admin Notes -->
        <div th:if="${report.adminNotes != null}" class="bg-blue-50 border border-blue-200 rounded-xl p-6">
            <h3 class="font-semibold text-blue-900 mb-2">Catatan Admin</h3>
            <p class="text-blue-800" th:text="${report.adminNotes}">Catatan</p>
        </div>

        <!-- Rejection Reason -->
        <div th:if="${report.rejectionReason != null}" class="bg-red-50 border border-red-200 rounded-xl p-6">
            <h3 class="font-semibold text-red-900 mb-2">Alasan Penolakan</h3>
            <p class="text-red-800" th:text="${report.rejectionReason}">Alasan</p>
        </div>

        <!-- Timeline -->
        <div th:if="${!#lists.isEmpty(report.revisions)}" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 class="font-semibold text-gray-900 mb-4">Riwayat Perubahan Status</h3>
            <div class="space-y-4">
                <div th:each="rev : ${report.revisions}" class="flex gap-3">
                    <div class="w-2 h-2 mt-2 bg-blue-600 rounded-full flex-shrink-0"></div>
                    <div>
                        <p class="text-sm font-medium text-gray-900">
                            <span th:text="${rev.oldStatus}">Lama</span>
                            → <span th:text="${rev.newStatus}">Baru</span>
                        </p>
                        <p class="text-xs text-gray-500" th:text="${rev.changedAt}">Tanggal</p>
                        <p th:if="${rev.notes != null}" class="text-sm text-gray-600" th:text="${rev.notes}">Catatan</p>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
```

### 8.5 warga/notifications.html

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Notifikasi</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50">
    <header class="bg-white shadow-sm sticky top-0 z-10">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex items-center justify-between h-16">
                <div class="flex items-center gap-3">
                    <a th:href="@{/warga/dashboard}" class="p-2 hover:bg-gray-100 rounded-lg">
                        <i data-lucide="arrow-left" class="w-5 h-5 text-gray-600"></i>
                    </a>
                    <div>
                        <h1 class="font-bold text-gray-900">Notifikasi</h1>
                        <p class="text-xs text-gray-500">
                            <span th:text="${unreadCount}"></span> belum dibaca
                        </p>
                    </div>
                </div>
                <form th:action="@{/warga/notifications/mark-read}" method="post"
                      th:if="${unreadCount > 0}">
                    <button type="submit" class="text-sm text-blue-600 hover:underline">
                        Tandai Semua Dibaca
                    </button>
                </form>
            </div>
        </div>
    </header>

    <main class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div th:if="${#lists.isEmpty(notifications)}" class="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
            <i data-lucide="bell-off" class="w-16 h-16 mx-auto mb-4 text-gray-300"></i>
            <p class="text-gray-500">Tidak ada notifikasi</p>
        </div>

        <div th:if="${!#lists.isEmpty(notifications)}" class="space-y-2">
            <div th:each="notif : ${notifications}"
                 class="bg-white rounded-xl shadow-sm border p-4 hover:shadow-md transition-shadow"
                 th:classappend="${notif.isRead} ? 'border-gray-100' : 'border-blue-200 bg-blue-50'">
                <a th:href="@{|/warga/report-detail?id=${notif.referenceId}|}" class="block">
                    <div class="flex items-start justify-between gap-3">
                        <div class="min-w-0 flex-1">
                            <p class="font-medium text-gray-900" th:text="${notif.title}">Judul</p>
                            <p class="text-sm text-gray-600 mt-1" th:text="${notif.message}">Pesan</p>
                            <p class="text-xs text-gray-400 mt-2" th:text="${notif.sentAt}">Tanggal</p>
                        </div>
                        <div th:if="${!notif.isRead}" class="w-2 h-2 bg-blue-600 rounded-full flex-shrink-0 mt-2"></div>
                    </div>
                </a>
            </div>
        </div>
    </main>
</div>
```

### 8.6 Admin Validation Panel (Tambahan)

Buat folder `templates/admin/` dan file `validation-panel.html`:

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Validasi Laporan</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50">
    <header class="bg-white shadow-sm sticky top-0 z-10">
        <div class="px-6 py-5">
            <h1 class="text-2xl font-bold text-gray-900">Validasi Laporan Masuk</h1>
            <p class="text-sm text-gray-500">
                <span th:text="${reportsCount}"></span> laporan menunggu validasi
            </p>
        </div>
    </header>

    <main class="p-6 max-w-[1400px] mx-auto space-y-4">
        <div th:each="report : ${reports}" class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <div class="flex items-start justify-between mb-4">
                <div>
                    <span class="font-mono font-semibold text-blue-600" th:text="${report.ticketNumber}">RPT-001</span>
                    <h3 class="font-medium text-gray-900 mt-1" th:text="${report.descriptionShort}">Judul</h3>
                    <p class="text-xs text-gray-500 mt-1" th:text="${report.dateFormatted}">Tanggal</p>
                </div>
            </div>

            <div class="mb-4 p-4 bg-gray-50 rounded-lg">
                <p class="text-sm text-gray-700" th:text="${report.description}">Deskripsi</p>
                <p class="text-xs text-gray-500 mt-2">
                    Pelapor: <span th:text="${report.reporterName}">Nama</span>
                    | Lokasi: <span th:text="${report.locationHint}">Lokasi</span>
                </p>
            </div>

            <form th:action="@{/admin/validation}" method="post" class="space-y-3">
                <input type="hidden" name="reportId" th:value="${report.reportId}" />
                <textarea name="notes" rows="2" class="w-full px-4 py-2 border rounded-lg"
                          placeholder="Catatan untuk warga (opsional)"></textarea>
                <div class="flex gap-2">
                    <button type="submit" name="action" value="accept"
                            class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700">
                        Terima
                    </button>
                    <button type="submit" name="action" value="revision"
                            class="px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700">
                        Minta Revisi
                    </button>
                    <textarea name="rejectionReason" rows="1" class="flex-1 px-4 py-2 border rounded-lg"
                              placeholder="Alasan penolakan (wajib jika tolak)"></textarea>
                    <button type="submit" name="action" value="reject"
                            class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700">
                        Tolak
                    </button>
                </div>
            </form>
        </div>

        <div th:if="${#lists.isEmpty(reports)}"
             class="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
            <i data-lucide="check-circle" class="w-16 h-16 mx-auto mb-4 text-gray-300"></i>
            <p class="text-gray-500">Tidak ada laporan yang menunggu validasi</p>
        </div>
    </main>
</div>
```

---

## BAGIAN 9: CONTROLLER (Route Baru)

Tambahkan method-method berikut ke `WebController.java` yang sudah ada:

```java
// ==========================================
// WARGA — PELAPORAN
// ==========================================

@Autowired
private ReportService reportService;

@Autowired
private NotificationService notificationService;

@Autowired
private ReportCategoryRepository categoryRepository;

/** GET /warga/create-report — form buat laporan baru */
@GetMapping("/warga/create-report")
public String wargaCreateReport(Model model, HttpSession session) {
    String userId = (String) session.getAttribute("userId");
    if (userId == null) return "redirect:/warga/login";

    model.addAttribute("createReportDTO", new CreateReportDTO());
    model.addAttribute("categories", categoryRepository.findByIsActiveTrue());
    return "warga/create-report";
}

/** POST /warga/create-report — simpan laporan baru */
@PostMapping("/warga/create-report")
public String wargaCreateReportPost(@ModelAttribute CreateReportDTO dto,
                                     HttpSession session,
                                     Model model) {
    String userId = (String) session.getAttribute("userId");
    if (userId == null) return "redirect:/warga/login";

    try {
        Report report = reportService.createReport(dto, userId);

        // Kirim notifikasi
        notificationService.createNotification(userId,
            "Laporan Diterima",
            "Laporan Anda dengan nomor tiket " + report.getTicketNumber() + " telah diterima dan menunggu validasi.",
            "REPORT", report.getReportId());

        return "redirect:/warga/report-detail?id=" + report.getReportId();
    } catch (Exception e) {
        model.addAttribute("error", e.getMessage());
        model.addAttribute("categories", categoryRepository.findByIsActiveTrue());
        return "warga/create-report";
    }
}

/** GET /warga/report-history — riwayat laporan warga */
@GetMapping("/warga/report-history")
public String wargaReportHistory(HttpSession session, Model model,
                                  @RequestParam(defaultValue = "") String status,
                                  @RequestParam(defaultValue = "") String q) {
    String userId = (String) session.getAttribute("userId");
    if (userId == null) return "redirect:/warga/login";

    List<Report> reports = reportService.getReportsByWarga(userId);

    // Filter & search logic
    if (!status.isBlank()) {
        // filter by status
    }
    if (!q.isBlank()) {
        // search logic
    }

    model.addAttribute("reports", reports);
    // ... add filter attributes
    return "warga/report-history";
}

/** GET /warga/report-detail — detail satu laporan */
@GetMapping("/warga/report-detail")
public String wargaReportDetail(@RequestParam String id, Model model) {
    Report report = reportService.findById(id).orElse(null);
    if (report == null) {
        return "redirect:/warga/report-history";
    }
    model.addAttribute("report", report);
    return "warga/report-detail";
}

/** GET /warga/notifications — daftar notifikasi */
@GetMapping("/warga/notifications")
public String wargaNotifications(HttpSession session, Model model) {
    String userId = (String) session.getAttribute("userId");
    if (userId == null) return "redirect:/warga/login";

    model.addAttribute("notifications", notificationService.getNotificationsByUser(userId));
    model.addAttribute("unreadCount", notificationService.countUnreadByUser(userId));
    return "warga/notifications";
}

/** POST /warga/notifications/mark-read — tandai semua sudah dibaca */
@PostMapping("/warga/notifications/mark-read")
public String wargaNotificationsMarkRead(HttpSession session) {
    String userId = (String) session.getAttribute("userId");
    if (userId == null) return "redirect:/warga/login";
    notificationService.markAllAsReadByUser(userId);
    return "redirect:/warga/notifications";
}

// ==========================================
// ADMIN — VALIDASI
// ==========================================

/** GET /admin/validation — panel validasi laporan masuk */
@GetMapping("/admin/validation")
public String adminValidation(Model model) {
    List<Report> reports = reportService.getReportsByStatus(Report.ReportStatus.MENUNGGU_VALIDASI);
    model.addAttribute("reports", reports);
    model.addAttribute("reportsCount", reports.size());
    return "admin/validation-panel";
}

/** POST /admin/validation — proses validasi */
@PostMapping("/admin/validation")
public String adminValidationPost(@RequestParam String reportId,
                                   @RequestParam String action,
                                   @RequestParam(required = false) String notes,
                                   @RequestParam(required = false) String rejectionReason) {
    String adminId = "SYSTEM"; // In production, get from session

    switch (action) {
        case "accept" -> {
            reportService.updateStatus(reportId, Report.ReportStatus.DIVALIDASI, notes, adminId);
            Report r = reportService.findById(reportId).orElse(null);
            if (r != null) {
                notificationService.createNotification(r.getReporter().getUserId(),
                    "Laporan Divalidasi",
                    "Laporan Anda telah divalidasi oleh Admin Pusat.",
                    "VALIDATION", reportId);
            }
        }
        case "revision" -> {
            reportService.updateStatus(reportId, Report.ReportStatus.PERLU_REVISI, notes, adminId);
        }
        case "reject" -> {
            reportService.updateStatus(reportId, Report.ReportStatus.DITOLAK,
                rejectionReason, notes, adminId);
        }
    }

    return "redirect:/admin/validation";
}
```

---

## BAGIAN 10: PBO EVIDENCE CHECKLIST

Sebelum commit, pastikan semua 4 pilar PBO terpenuhi:

### ✅ INHERITANCE (extends BaseEntity)
- [ ] `Report.java` extends `BaseEntity`
- [ ] `ReportCategory.java` extends `BaseEntity`
- [ ] `ReportRevision.java` extends `BaseEntity`
- [ ] `Notification.java` extends `BaseEntity`

### ✅ POLYMORPHISM (@Override + Overloading)
- [ ] `ReportServiceImpl implements ReportService` dengan `@Override`
- [ ] Method overloading: `getReportsByStatus()`, `getReportsByDateRange()`, `getReportsByStatusAndDateRange()`
- [ ] Overloading: `updateStatus()` dengan 3 parameter dan 4 parameter
- [ ] `NotificationServiceImpl implements NotificationService` dengan `@Override`
- [ ] Method overloading: `getNotificationsByUser()`, `getUnreadNotificationsByUser()`, `getNotificationsByType()`

### ✅ ABSTRACTION (Interface)
- [ ] Controller hanya inject interface (`ReportService`, `NotificationService`)
- [ ] Tidak ada akses langsung ke `ReportRepository` dari Controller
- [ ] `ReportService.java` dan `NotificationService.java` sebagai kontrak

### ✅ ENCAPSULATION (Private + DTO)
- [ ] Semua field entity = PRIVATE
- [ ] Semua form input = DTO (`CreateReportDTO`, `ReportFilterDTO`, `NotificationDTO`)
- [ ] Tidak ada entity yang langsung dipakai sebagai form backing bean

---

## BAGIAN 11: GIT WORKFLOW

```bash
# 1. Clone repository
git clone <repo-url>
cd AduAja

# 2. Buat branch untuk modul 2
git checkout -b fitur/modul-2-reporting

# 3. Buat semua file:
#    - 4 Entity di model/
#    - 4 Repository di repository/
#    - 3 DTO di dto/
#    - 2 Interface + 2 Implementation di service/
#    - 5 View HTML di templates/warga/
#    - 1 View HTML di templates/admin/
#    - Update WebController.java dengan route baru

# 4. Commit dalam beberapa tahap
git add src/main/java/com/plr/aduaja/model/Report.java
git commit -m "feat: Report entity with inheritance extends BaseEntity"

git add src/main/java/com/plr/aduaja/model/ReportCategory.java
git commit -m "feat: ReportCategory entity extends BaseEntity"

git add src/main/java/com/plr/aduaja/model/ReportRevision.java
git commit -m "feat: ReportRevision entity extends BaseEntity"

git add src/main/java/com/plr/aduaja/model/Notification.java
git commit -m "feat: Notification entity extends BaseEntity"

# Repository layer
git add src/main/java/com/plr/aduaja/repository/
git commit -m "feat: Repository layer for modul pelaporan"

# DTO layer
git add src/main/java/com/plr/aduaja/dto/CreateReportDTO.java
git add src/main/java/com/plr/aduaja/dto/ReportFilterDTO.java
git add src/main/java/com/plr/aduaja/dto/NotificationDTO.java
git commit -m "feat: DTO for report and notification forms"

# Service layer (Interface + Impl)
git add src/main/java/com/plr/aduaja/service/ReportService.java
git add src/main/java/com/plr/aduaja/service/ReportServiceImpl.java
git add src/main/java/com/plr/aduaja/service/NotificationService.java
git add src/main/java/com/plr/aduaja/service/NotificationServiceImpl.java
git commit -m "feat: Service layer with polymorphism (override + overloading)"

# Views
git add src/main/resources/templates/warga/
git add src/main/resources/templates/admin/validation-panel.html
git commit -m "feat: View HTML for warga reports and admin validation"

# Controller
git add src/main/java/com/plr/aduaja/controller/WebController.java
git commit -m "feat: Controller routes for pelaporan modul"

# 5. Push ke remote
git push origin fitur/modul-2-reporting

# 6. Create Pull Request di GitHub
```
