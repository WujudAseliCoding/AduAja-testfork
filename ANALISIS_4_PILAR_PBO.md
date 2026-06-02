# ANALISIS 4 PILAR PBO PADA APLIKASI AduAja

---

## FITUR 1: Registrasi & Login Warga

**File utama:** `WargaAuthController.java`, `AuthService.java` / `AuthServiceImpl.java`, `UserService.java` / `UserServiceImpl.java`, `LoginAttempt.java`, `User.java`

### Inheritance

```java
// User.java:14 — User mewarisi BaseEntity
public class User extends BaseEntity { ... }

// LoginAttempt.java:13 — Catatan percobaan login juga extends BaseEntity
public class LoginAttempt extends BaseEntity { ... }
```
**Penjelasan:** `User` dan `LoginAttempt` mendapat `createdAt`/`updatedAt` otomatis dari `BaseEntity`. Fitur login otomatis mencatat kapan user login terakhir tanpa perlu coding manual.

### Encapsulation

**Ada 3 level enkapsulasi di fitur ini:**

**1. Enkapsulasi data User** — `User.java:20-101`
```java
private String passwordHash;  // PRIVATE: tidak bisa dibaca dari luar
// Hanya bisa diakses via:
public String getPasswordHash() { return passwordHash; }
public void setPasswordHash(String hash) { this.passwordHash = hash; }
```

**2. Enkapsulasi password** — `UserServiceImpl.java:83`
```java
user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
// Logika hashing (BCrypt) DIKURUNG di dalam createUser()
// Controller tidak tahu password di-hash bagaimana
```

**3. Enkapsulasi konstanta keamanan** — `AuthServiceImpl.java:29-30`
```java
private static final int MAX_ATTEMPTS = 5;       // PRIVATE: tidak bisa diubah luar
private static final int LOCKOUT_MINUTES = 30;   // PRIVATE
// Hanya AuthServiceImpl yang tahu kapan akun di-lock
```

### Polymorphism

**1. Overloading login** — `AuthService.java:23-25`
```java
public interface AuthService {
    Optional<User> login(LoginDTO dto, String ipAddress);           // Login via DTO
    Optional<User> loginByEmail(String email, String password, String ip); // Overload: langsung email
    Optional<User> loginByPhone(String phone, String password, String ip); // Overload: langsung HP
}
```
**Penjelasan:** Tiga method dengan nama mirip, parameter berbeda — Java memilih yang mana akan dipanggil saat kompilasi (compile-time polymorphism).

**2. Overriding** — `AuthServiceImpl.java:27`
```java
public class AuthServiceImpl implements AuthService {
    @Override  // Runtime Polymorphism
    public Optional<User> login(LoginDTO dto, String ipAddress) { ... }
}
```

### Abstraction

```java
// WargaAuthController.java:31-37 — Controller hanya tahu INTERFACE
@Autowired
private UserService userService;   // Interface, bukan UserServiceImpl
@Autowired
private AuthService authService;   // Interface, bukan AuthServiceImpl
@Autowired
private OtpService otpService;     // Interface, bukan OtpServiceImpl

// WargaAuthController.java:129 — Panggil method tanpa tahu implementasi
User user = userService.createUser(dto);
// Controller tidak tahu: ada hashing password, ada validasi duplikat,
// ada pembuatan UserProfile — semua DIABSTRAKSI
```

---

## FITUR 2: Edit Profile Warga

**File utama:** `WargaAuthController.java` (method `updateProfile`), `UserService.java` / `UserServiceImpl.java`, `ProfileDTO.java`, `UserProfile.java`, `User.java`

### Inheritance

```java
// User.java:14 — User extends BaseEntity
// UserProfile.java:12 — UserProfile extends BaseEntity
public class UserProfile extends BaseEntity {
    // Otomatis punya createdAt dan updatedAt dari BaseEntity
    // Berguna untuk track kapan terakhir profil diubah
}
```

### Encapsulation

**1. DTO pattern** — `ProfileDTO.java`
```java
// Data dari form edit profile dikurung dalam DTO (Data Transfer Object)
public class ProfileDTO {
    private String fullName;      // PRIVATE
    private String email;         // PRIVATE
    private String phoneNumber;   // PRIVATE
    private String nik;           // PRIVATE
    private String alamatLengkap; // PRIVATE

    public String getFullName() { return fullName; }        // Getter publik
    public void setFullName(String n) { this.fullName = n; }  // Setter publik
}
```
**Penjelasan:** Entity `User` tidak pernah terekspos langsung ke view/html. Data dari form ditangkap oleh `ProfileDTO` — ini enkapsulasi data antar lapisan.

**2. Enkapsulasi logika update** — `UserServiceImpl.java:109-150`
```java
@Override
public User updateProfile(String userId, ProfileDTO dto) {
    // Semua validasi disembunyikan di sini:
    // - Cek duplikat email
    // - Cek duplikat NIK
    // - Update User + UserProfile dalam 1 method transaksional
    // Controller hanya panggil: userService.updateProfile(id, dto);
}
```

### Polymorphism

**Overriding** — `UserServiceImpl.java:109`
```java
@Override  // Runtime Polymorphism: override dari UserService interface
public User updateProfile(String userId, ProfileDTO dto) { ... }
```
**Penjelasan:** Saat `WargaAuthController` memanggil `userService.updateProfile()`, yang dijalankan adalah `UserServiceImpl.updateProfile()` — Spring menentukan implementasi saat runtime.

### Abstraction

```java
// WargaAuthController — method updateProfile milik controller
userService.updateProfile(userId, dto);
// Controller tidak tahu:
// 1. Bahwa method ini juga mengupdate UserProfile
// 2. Bagaimana validasi duplikat email/NIK
// 3. Bahwa ada transaksi database di dalamnya
// Semua DIABSTRAKSI oleh interface UserService
```

---

## FITUR 3: Buat Laporan (Create Report)

**File utama:** `WebController.java` (method `wargaCreateReportPost`), `Report.java`, `ReportService.java` / `ReportServiceImpl.java`, `CreateReportDTO.java`, `ReportCategory.java`

### Inheritance

```java
// Report.java:15 — Report extends BaseEntity
public class Report extends BaseEntity {
    // Mendapat createdAt (kapan laporan dibuat)
    // Mendapat updatedAt (kapan laporan diubah)
}

// ReportCategory.java:11 — Kategori juga entity
public class ReportCategory extends BaseEntity { ... }
```

### Encapsulation

```java
// Report.java:20-105 — 24 field PRIVATE
private String description;       // PRIVATE
private String photoBase64;       // PRIVATE — foto warga
private BigDecimal latitude;      // PRIVATE — lokasi
private String adminNotes;        // PRIVATE — catatan internal admin
private String rejectionReason;   // PRIVATE — alasan penolakan

// Semua hanya bisa diakses via getter publik
public String getDescription() { return description; }
public String getAdminNotes() { return adminNotes; }
```
**Penjelasan:** Data sensitif laporan (foto, lokasi, catatan admin) dikurung dalam `private` field. Catatan admin tidak bisa dibaca oleh warga karena tidak ada getter yang diekspos ke view warga.

### Polymorphism

**Enum switch-case** — `WebController.java:1325-1331`
```java
switch (r.getStatus()) {
    case MENUNGGU_VALIDASI:  // Label: "Menunggu Validasi"
    case DIVALIDASI:         // Label: "Tervalidasi"
    case DIDISPOSISI:        // Label: "Didisposisi"
    case SEDANG_DIKERJAKAN:  // Label: "Sedang Dikerjakan"
    case SELESAI:            // Label: "Selesai"
    case DITOLAK:            // Label: "Ditolak"
    // ... 12 status total
}
```
**Penjelasan:** Satu struktur switch menangani 12 status laporan berbeda. Setiap `ReportStatus` enum menghasilkan label dan tampilan berbeda — ini **polymorphism via enum**.

### Abstraction

```java
// WebController.java:37 — hanya tahu interface
@Autowired
private ReportService reportService;  // Interface

// Di ReportServiceImpl, logika lengkap pembuatan laporan:
// - Validasi user
// - Set status MENUNGGU_VALIDASI
// - Simpan relasi ke User (reporter)
// - Kirim notifikasi ke admin
// Controller hanya panggil:
reportService.createReport(dto, reporterId);
```

---

## FITUR 4: Validasi Laporan oleh Admin

**File utama:** `WebController.java` (method `adminValidationPost`), `ReportService.java` / `ReportServiceImpl.java`, `ValidationDecision.java`, `ReportRevision.java`, `Notification.java`

### Inheritance

```java
// ReportRevision.java:12 — extends BaseEntity (mencatat revisi)
public class ReportRevision extends BaseEntity { ... }

// ValidationDecision.java:8 — extends BaseEntity (keputusan validasi)
public class ValidationDecision extends BaseEntity { ... }

// Notification.java:12 — extends BaseEntity
public class Notification extends BaseEntity { ... }
```

### Encapsulation

**Logika validasi dikurung di service:**
```java
// ReportServiceImpl — method validasi internal
// Method PRIVATE untuk logging internal
private void createRevision(Report report, String changes, String changedBy) {
    // Hanya dipanggil di dalam class yang sama
    // Controller tidak bisa mengakses langsung
}
```

### Polymorphism

**Overloading updateStatus** — `ReportService.java:42-43`
```java
public interface ReportService {
    // Overload 1: 4 parameter
    Report updateStatus(String reportId, Report.ReportStatus newStatus, String notes, String changedBy);

    // Overload 2: 5 parameter — dengan rejectionReason + adminNotes
    Report updateStatus(String reportId, Report.ReportStatus newStatus,
                        String rejectionReason, String adminNotes, String changedBy);
}
```

### Abstraction

```java
// WebController: method adminValidationPost
reportService.updateStatus(ticketId, Report.ReportStatus.DIVALIDASI, note, adminId);
// Controller tidak tahu bahwa di dalamnya:
// 1. Report di-save ke database
// 2. ReportRevision dibuat otomatis
// 3. ValidationDecision dicatat
// 4. Notifikasi dikirim ke warga
```

---

## FITUR 5: Disposisi + SLA

**File utama:** `DispositionService.java`, `SlaRecordService.java`, `SlaMonitoringService.java`, `Disposition.java`, `SlaRecord.java`, `SlaPauseLog.java`, `Agency.java`

### Inheritance

Entity berikut **TIDAK** extends `BaseEntity` (berdiri sendiri):
```java
// Disposition.java:8 — entity berdiri sendiri
public class Disposition { ... }

// SlaRecord.java:8 — entity berdiri sendiri
public class SlaRecord { ... }

// SlaPauseLog.java:8 — entity berdiri sendiri
public class SlaPauseLog { ... }

// Agency.java:7 — entity berdiri sendiri
public class Agency { ... }
```

**Penjelasan:** Ini contoh bahwa Inheritance TIDAK WAJIB dipakai untuk semua class. Entity ini tidak membutuhkan `createdAt`/`updatedAt` karena datanya sudah punya timestamp sendiri (`dispatchedAt`, `slaStartAt`, `pausedAt`).

### Encapsulation

```java
// DispositionService.java:47-77 — Logika disposisi + SLA dikurung dalam 1 method
public Disposition createDisposition(String reportId, ...) {
    // Enkapsulasi: perhitungan deadline SLA disembunyikan
    if (report.getCategory() != null && report.getCategory().getSlaDurationHours() != null) {
        sla.setSlaDeadlineAt(LocalDateTime.now().plusHours(report.getCategory().getSlaDurationHours()));
    } else {
        sla.setSlaDeadlineAt(LocalDateTime.now().plusHours(48));  // Default dikurung di sini
    }
}
```

### Polymorphism

**Method variasi** — `DispositionService.java:35-44`
```java
public Optional<Disposition> getDispositionById(String id) { ... }        // By primary key
public Optional<Disposition> getDispositionByReportId(String reportId) { ... }  // By report
public List<Disposition> getDispositionsByAgency(String agencyId) { ... }       // By agency
// Method berbeda dengan tujuan berbeda — variasi polymorphic
```

### Abstraction

```java
// DisposisiApiController.java:20
@Autowired
private DispositionService dispositionService;
// DispositionService adalah class konkret (tanpa interface terpisah)
// Tapi tetap abstraksi karena method-methodnya menyembunyikan detail:
// - Bagaimana SLA dibuat bersamaan dengan disposisi
// - Bagaimana deadline dihitung
```

**Scheduled task menyembunyikan jadwal:**
```java
// SlaMonitoringService.java:32 — Abstraksi waktu
@Scheduled(fixedRate = 3600000)  // Setiap 1 jam — DIABSTRAKSI
public void checkSlaViolations() {
    // Method ini berjalan otomatis, tidak perlu dipanggil manual
}
```

---

## FITUR 6: Merge Ticket

**File utama:** `MergeRecordService.java`, `MergeRecord.java`, `MergeRecordRepository.java`, `WebController.java`

### Inheritance

```java
// MergeRecord.java:8 — entity berdiri sendiri
public class MergeRecord { ... }  // TIDAK extends BaseEntity
```
**Penjelasan:** MergeRecord punya field `mergedAt` sendiri sehingga tidak perlu inheritance.

### Encapsulation

```java
// MergeRecordService.java:59-69 — Logika undo dikurung rapat
public MergeRecord undoMerge(String mergeId) {
    // Enkapsulasi: operasi kompleks dalam 1 method
    // 1. Cari record
    // 2. Soft delete (isActive = false)
    // 3. Lepas child dari parent
    // 4. Simpan perubahan
    // Semua detail ini DIKURUNG, pemanggil hanya lihat: undoMerge(id)
}
```

### Polymorphism

**Overriding** — `MergeRecordService.java` (method-method dari JpaRepository):
```java
// Runtime: mergeRecordRepository.findAll() memanggil implementasi Spring Data JPA
// Runtime: mergeRecordRepository.save(record) — implementasi dari SimpleJpaRepository
```

### Abstraction

```java
// WebController.java:58
@Autowired
private MergeRecordService mergeRecordService;
// WebController tidak tahu bahwa MergeRecordService:
// - Mengakses 3 repository berbeda (MergeRecord, Report, User)
// - Melakukan soft delete (isActive = false)
// - Mengupdate childReport.parentReport
```

---

## FITUR 7: Notifikasi

**File utama:** `Notification.java`, `NotificationService.java` / `NotificationServiceImpl.java`

### Inheritance

```java
// Notification.java:12 — extends BaseEntity
public class Notification extends BaseEntity {
    // createdAt otomatis mencatat kapan notifikasi dikirim
}
```

### Encapsulation

```java
// Notification.java:14-55 — 9 field PRIVATE
private String messageText;     // PRIVATE
private Boolean isRead = false; // PRIVATE
// isRead tidak bisa diubah langsung dari luar
// Harus via markAsRead() yang juga mengupdate di database
```

### Polymorphism

**Overloading** — `NotificationService.java:18-20`
```java
public interface NotificationService {
    List<Notification> getNotificationsByUser(String userId);               // Semua notif
    List<Notification> getUnreadNotificationsByUser(String userId);         // Hanya belum dibaca
    List<Notification> getNotificationsByType(String userId, String type);  // Filter tipe
}
```

**Default method (Java 8+)** — `NotificationService.java:43-48`
```java
default Notification createNotification(String userId, String message,
                                         Notification.NotificationType type) {
    return createNotification(userId,
            type != null ? type.name() : "NOTIFIKASI",
            message, "SYSTEM", null);
    // Default method: overloading untuk backward compatibility
    // Bisa dioverride oleh implementasi jika perlu
}
```

### Abstraction

```java
// WebController.java:55
@Autowired
private NotificationService notificationService;  // Interface!

// notificationService.createNotification(...)
// Controller tidak tahu bagaimana notifikasi disimpan
// Tidak tahu database apa yang digunakan
```

---

## RINGKASAN PETA FITUR → PILAR PBO

| Fitur | Inheritance | Encapsulation | Polymorphism | Abstraction |
|-------|:-----------:|:-------------:|:------------:|:-----------:|
| **Registrasi & Login** | `User extends BaseEntity`, `LoginAttempt extends BaseEntity` | Password di-hash, `MAX_ATTEMPTS` private, field `private` | Overloading `login()`/`loginByEmail()`/`loginByPhone()`, `@Override` di `AuthServiceImpl` | Controller pakai `AuthService` interface |
| **Edit Profile** | `User extends BaseEntity`, `UserProfile extends BaseEntity` | `ProfileDTO` murni private + getter/setter, logika validasi di `updateProfile()` | `@Override UserServiceImpl.updateProfile()` | Controller panggil `userService.updateProfile()` tanpa tahu detail |
| **Buat Laporan** | `Report extends BaseEntity` | 24 field private di Report, foto/lokasi dikurung | Switch-case 12 `ReportStatus` enum | Controller pakai `ReportService` interface |
| **Validasi Laporan** | `ReportRevision extends BaseEntity`, `ValidationDecision extends BaseEntity`, `Notification extends BaseEntity` | Method `private createRevision()` | Overloading `updateStatus()` 4 vs 5 parameter | Validasi + notifikasi + revisi dalam 1 panggil `updateStatus()` |
| **Disposisi + SLA** | (Entity berdiri sendiri — contoh Inheritance tidak selalu dipakai) | Deadline SLA dihitung internal di `createDisposition()` | Method variasi `getDisposisiBy...()` | `@Scheduled` abstraksi waktu |
| **Merge Ticket** | (Entity berdiri sendiri) | Logika undo merge (soft delete + unlink) dikurung di `undoMerge()` | Polymorphism via JpaRepository bawaan | `MergeRecordService` abstraksi 3 repository |
| **Notifikasi** | `Notification extends BaseEntity` | `isRead` private, harus via `markAsRead()` | Overloading `getNotificationsBy...()`, default method | Controller pakai `NotificationService` interface |

---

## CONTOH SPESIFIK: **Edit Profile** — Pilar Apa Saja?

```
EDIT PROFILE → menggunakan 4 PILAR sekaligus:

1. INHERITANCE   → User extends BaseEntity + UserProfile extends BaseEntity
                    (data profil memiliki timestamp otomatis)

2. ENCAPSULATION → ProfileDTO: semua field private
                    UserServiceImpl.updateProfile(): validasi duplikat email/NIK
                    dikurung dalam 1 method transaksional

3. POLYMORPHISM  → @Override public User updateProfile(String userId, ProfileDTO dto)
                    Runtime: Spring jalankan UserServiceImpl punya, bukan class lain

4. ABSTRACTION   → WargaAuthController hanya lihat UserService interface
                    Tidak tahu ada UserProfile, validasi duplikat, transaksi DB
```

---

## CONTOH SPESIFIK: **Disposisi + SLA** — Pilar Apa Saja?

```
DISPOSISI + SLA → menggunakan 4 PILAR:

1. INHERITANCE   → TIDAK ada — entity Disposition, SlaRecord, SlaPauseLog, Agency
                    berdiri sendiri (contoh Inheritance TIDAK WAJIB)

2. ENCAPSULATION → Perhitungan deadline SLA (48 jam default) dikurung di
                    DispositionService.createDisposition() dan SlaRecordService.createSlaRecord()

3. POLYMORPHISM  → Method variasi getDispositionById / getDispositionByReportId /
                    getDispositionsByAgency — nama berbeda, logika berbeda

4. ABSTRACTION   → @Scheduled(fixedRate = 3600000): SlaMonitoringService berjalan
                    otomatis setiap jam tanpa perlu dipanggil controller
```

---

## DIAGRAM HUBUNGAN 4 PILAR DI SELURUH APLIKASI

```
                    LAPISAN CONTROLLER (Abstraction)
                    ┌──────────────────────────────────┐
                    │  Hanya tahu Interface Service     │
                    │  @Autowired UserService           │
                    │  @Autowired ReportService         │
                    └──────────┬───────────────────────┘
                               │ Method calls via Interface
                    ┌──────────▼───────────────────────┐
                    │  LAPISAN SERVICE (Polymorphism)   │
                    │  UserServiceImpl implements UserService  │
                    │  @Override semua method          │
                    │  ReportServiceImpl implements ReportService │
                    └──────────┬───────────────────────┘
                               │ Mengakses Entity & Repository
              ┌────────────────┼────────────────────┐
              │                │                    │
    ┌─────────▼────────┐  ┌───▼────────┐   ┌───────▼───────┐
    │ LAPISAN ENTITY   │  │ ENKAPSULASI │   │ ENKAPSULASI   │
    │ (Inheritance)    │  │   Data      │   │   Logic       │
    │                  │  │             │   │               │
    │ BaseEntity       │  │ private     │   │ private       │
    │  ├─ User         │  │ fields      │   │ helper methods│
    │  ├─ Report       │  │ + getters   │   │ generateOtp() │
    │  └─ ...          │  │ + setters   │   │ createRev()   │
    └──────────────────┘  └─────────────┘   └──────────────┘
```
