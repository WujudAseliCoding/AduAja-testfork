# PEMBAGIAN TUGAS TIM — SISTEM ADUAJA
## 4 Pilar PBO (OOP) + Arsitektur MVC Spring Boot

> Dibuat: 12 Mei 2026
> Anggota Tim: 4 Orang
> Framework: Spring Boot + Spring Data JPA + Thymeleaf

---

## ⚠️ PERINGATAN KRUSIAL — OOP DEFINISI YANG BENAR

> Dokumen ini telah diperbaiki dari kesalahan akademis sebelumnya. Baca bagian ini SEBELUM mulai coding.

### Apa yang SALAH di Versi Lama:

| Salah | Seharusnya |
|-------|-----------|
| HAS-A (`@OneToOne`, `@ManyToOne`) disebut "Inheritance" | HAS-A = **Composition/Aggregation**, BUKAN Inheritance |
| Enum switch-case disebut "Polymorphism" | Switch-case = **Procedural Code**, BUKAN Polymorphism |
| Tidak ada `BaseEntity` wajib | **Inheritance sejati HARUS pakai `extends BaseEntity`** |

### Definisi 4 Pilar PBO yang BENAR:

#### 1. INHERITANCE (Pewarisan) — `extends` keyword
```java
// Parent class (HARUS ADA)
@MappedSuperclass
public abstract class BaseEntity {
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Child class — TRUE INHERITANCE
@Entity
@Table(name = "users")
public class User extends BaseEntity { /* ... */ }
```
**Ini INHERITANCE sejati.** Setiap entity WARAS turun dari `BaseEntity`, mendapatkan `createdAt` dan `updatedAt` otomatis.

#### 2. POLYMORPHISM (Polimorfisme) — 2 jenis
```java
// A. Compile-time Polymorphism: OVERLOADING (nama sama, parameter beda)
public interface UserService {
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);  // Overload: nama sama, parameter beda
}

// B. Run-time Polymorphism: OVERRIDING (@Override)
@Service
public class UserServiceImpl implements UserService {
    @Override  // OVERRIDE dari interface
    public Optional<User> findById(String id) { /* implementasi */ }

    @Override  // OVERRIDE dari interface
    public Optional<User> findByEmail(String email) { /* implementasi */ }
}
```

**CATATAN: Switch-case pada enum BUKAN polymorphism. Itu procedural code.**

#### 3. ABSTRACTION (Abstraksi) — Interface sebagai kontrak
```java
// Kontrak (Interface) — menyembunyikan detail
public interface UserService {
    User createUser(RegisterDTO dto);
    User updateProfile(String userId, ProfileDTO dto);
}

// Implementasi (rahasia, tidak perlu Controller tahu)
@Service
public class UserServiceImpl implements UserService {
    @Override
    public User createUser(RegisterDTO dto) {
        // Validasi email unik, hash password, create UserProfile, save
        // Semua logika kompleks DISEMBUNYIKAN dari Controller
    }
}
```

#### 4. ENCAPSULATION (Enkapsulasi) — Private + DTO
```java
// A. Field PRIVATE — tidak bisa diakses langsung
@Entity
public class User extends BaseEntity {
    @Column(name = "password_hash")
    private String passwordHash;  // PRIVATE — tidak ada akses langsung

    // B. Immutable object (AuditLog)
    public class AuditLog {
        private final String oldValue;  // FINAL — tidak bisa diubah
        private final String newValue;

        public AuditLog(String oldValue, String newValue) {
            this.oldValue = oldValue;  // Set di constructor saja
            this.newValue = newValue;
        }
        // TIDAK ADA SETTER
    }

    // C. DTO untuk form (pisahkan dari Entity)
    public class ProfileDTO {
        private String fullName;     // Form fields
        private String email;        // BUKAN Entity langsung
        private String nik;
    }
}
```

---

---

## ATURAN UTAMA (WAJIB DIBACA SEMUA ORANG)

### ⚠️ JANGAN SENTUH FILE INI — Merge ke main duluan oleh Orang 1

| File | Alasan |
|------|--------|
| `BaseEntity.java` | **WAJIB untuk semua entity** — dibuat Orang 1 |
| `SecurityConfig.java` | Konfigurasi keamanan, affect semua modul |
| `DataSeeder.java` | Sample data, affect semua modul |
| `layouts/master.html` | Template utama, affect semua halaman |
| `pom.xml` | Dependency, affect build semua orang |
| `Region.java` | Shared entity — semua modul pakai foreign key |
| **Semua Enum** | Shared, ubah satu = bentrok semua |

### ✅ WAJIB DIBIKIN OLEH SETIAP ORANG (Sesuai Modul Masing-Masing)

Setiap developer HARUS membuat file-file ini di modulnya:

```
Modul X/
├── dto/
│   ├── XxxDTO.java
│   └── XxxCreateDTO.java
├── service/
│   ├── XxxService.java         ← Interface (HARUS ADA)
│   └── XxxServiceImpl.java    ← Implementation (HARUS ADA)
├── repository/                  ← Bisa reuse existing
├── controller/                  ← Bisa tambah route
└── templates/                   ← View HTML sesuai modul
```

### ✅ BOLEH DIBACA (Read Only)

- `BaseEntity.java` — semua orang extends, tidak boleh ubah
- `Region.java` — semua orang baca foreign key, tidak boleh ubah
- `User.java` — semua orang inject & baca, TAPI update field sesuai modul:
  - **Orang 1** → `User.fullName`, `User.email`, `User.phoneNumber`
  - **Orang 2, 3, 4** → hanya baca

---

---

## RINGKASAN PEMBAGIAN GLOBAL

```
┌─────────────────────────────────────────────────────────────────────┐
│  ORANG 1 — MODUL AUTH & PROFIL                                      │
│  Fokus: Login, Register, OTP, Edit Profil, Session Management       │
│  Entities: 5  |  Services: 2  |  Views: 4                          │
├─────────────────────────────────────────────────────────────────────┤
│  ORANG 2 — MODUL PELAPORAN & NOTIFIKASI                             │
│  Fokus: Warga Buat Laporan, Riwayat, Validasi Admin, Notifikasi     │
│  Entities: 4  |  Services: 2  |  Views: 5                          │
├─────────────────────────────────────────────────────────────────────┤
│  ORANG 3 — MODUL DISPOSISI & EKSEKUSI LAPANGAN                      │
│  Fokus: Disposisi Admin, Penugasan Dinas, Task Petugas, Absensi    │
│  Entities: 6  |  Services: 4  |  Views: 11                          │
├─────────────────────────────────────────────────────────────────────┤
│  ORANG 4 — MODUL SLA, SENGKETA & AUDIT                              │
│  Fokus: Deadline Otomatis, Merge Ticket, Sengketa, Jejak Audit     │
│  Entities: 7  |  Services: 7  |  Views: 5                           │
└─────────────────────────────────────────────────────────────────────┘

Total: 22 Entities + 1 Shared (Region) + 1 BaseEntity = 24 files
```

---

---

## LANGKAH SEBELUM CODING (WAJIB DIKERJAKAN ORANG 1 TERLEBIH DAHULU)

> EXECUTE SEKUIPUN SEBELUM ORANG LAIN MULAI.

### Langkah 1: Buat BaseEntity.java (HARUS ADA)

```java
// src/main/java/com/plr/aduaja/model/BaseEntity.java
package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    // Getters only — fields are PRIVATE (Enkapsulasi)
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### Langkah 2: Modifikasi semua Entity untuk extends BaseEntity

Setiap developer mengubah entity-nya:

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {  // ← INHERITANCE sejati
    // Field: userId, fullName, email, passwordHash, role, accountStatus
    // BUKAN extends BaseEntity → TIDAK ADA createdAt/updatedAt
}

@Entity
@Table(name = "reports")
public class Report extends BaseEntity {  // ← INHERITANCE sejati
    // Field: reportId, ticketNumber, description, status
    // extends BaseEntity → ADA createdAt/updatedAt OTOMATIS
}
```

### Commit urutan:

```bash
# 1. Orang 1: Buat BaseEntity.java duluan
git add src/main/java/com/plr/aduaja/model/BaseEntity.java
git commit -m "feat: BaseEntity for all entities (inheritance)"
git push origin main

# 2. Orang 1: Modifikasi Region.java extends BaseEntity
git commit -m "feat: Region extends BaseEntity"
git push origin main

# 3. Semua orang pull, modifikasi entity masing-masing extends BaseEntity
git pull origin main
git checkout -b fitur/modul-1-auth-profile
# ... mulai coding
```

---

---

## ATURAN GITHUB (TEKNIS)

### Branch Naming Convention

```
fitur/modul-1-auth-profile
fitur/modul-2-reporting
fitur/modul-3-disposisi-task
fitur/modul-4-sla-dispute
```

### Langkah Kerja

```
1. git clone <repo>
2. git checkout -b fitur/modul-X-nama
3. Modifikasi entity -> extends BaseEntity
4. Buat DTO di folder dto/
5. Buat Service interface + ServiceImpl (WAJIB @Override)
6. Buat / modifikasi View HTML
7. Commit + Push
8. Pull Request -> Review -> Merge bareng-bareng
```

---

---

# DETAIL PER ORANG

---

## 👤 ORANG 1 — MODUL AUTH & PROFIL

**Branch:** `fitur/modul-1-auth-profile`
**Deskripsi:** Mengelola siapa bisa masuk sistem, data profil warga, OTP verifikasi, dan session management.

### 📁 FILE YANG DIKERJAKAN

#### BaseEntity (WAJIB extend semua entity)
```
src/main/java/com/plr/aduaja/model/
├── BaseEntity.java                ← INHERITANCE PARENT (1x, shared)
├── User.java                      ← extends BaseEntity
├── UserProfile.java              ← extends BaseEntity
├── LoginAttempt.java             ← extends BaseEntity
├── OtpVerification.java          ← extends BaseEntity
└── ActiveSession.java            ← extends BaseEntity
```

#### Repository (4 files)
```
src/main/java/com/plr/aduaja/repository/
├── UserRepository.java
├── UserProfileRepository.java
├── LoginAttemptRepository.java
├── OtpVerificationRepository.java
└── ActiveSessionRepository.java
```

#### Service + Interface (WAJIB: Interface + Impl + @Override + Overload)
```
src/main/java/com/plr/aduaja/service/
├── UserService.java               ← INTERFACE (Abstraction)
└── UserServiceImpl.java         ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── OtpService.java                ← INTERFACE (Abstraction)
└── OtpServiceImpl.java          ← @Override + Overloading (Polymorphism)
```

#### DTO (WAJIB untuk semua form input)
```
src/main/java/com/plr/aduaja/dto/
├── LoginDTO.java                  ← ENCAPSULATION: pisahkan dari Entity
├── RegisterDTO.java               ← ENCAPSULATION: pisahkan dari Entity
└── ProfileDTO.java              ← ENCAPSULATION: pisahkan dari Entity
```

#### View HTML (4 files)
```
src/main/resources/templates/
├── warga/
│   ├── login.html
│   └── profile.html
├── admin/
│   └── login.html
└── petugas/
    └── login.html
```

### 🛣️ ROUTE (Endpoint)

| Method | Path | Fungsi |
|--------|------|--------|
| GET | `/warga/login` | Halaman login warga |
| POST | `/warga/login` | Proses login warga + buat session |
| POST | `/warga/register` | Proses registrasi warga baru |
| GET | `/warga/profile` | Lihat profil (dari DB) |
| POST | `/warga/profile/edit` | Simpan perubahan profil |
| GET | `/admin/login` | Halaman login admin |
| POST | `/admin/login` | Proses login admin |
| POST | `/admin/logout` | Logout admin |
| GET | `/petugas/login` | Halaman login petugas |
| POST | `/petugas/login` | Proses login petugas |

### 🎯 PBO EVIDENCE (4 Pilar) — KODE YANG BENAR

#### 1. INHERITANCE — extends BaseEntity

```java
// BaseEntity.java (Parent — INHERITANCE)
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

    // Enkapsulasi: hanya getter, tidak ada setter public
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

// User.java — INHERITANCE sejati
@Entity
@Table(name = "users")
public class User extends BaseEntity {  // ← INHERITANCE
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private String userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;  // PRIVATE (Enkapsulasi)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile userProfile;  // HAS-A ≠ Inheritance

    public enum Role { WARGA, ADMIN_PUSAT, ADMIN_DINAS, PETUGAS }

    // Getters & Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }
}
```

#### 2. POLYMORPHISM — @Override + Overloading

```java
// UserService.java — Interface (Abstraction)
public interface UserService {
    // Overloading: method sama, parameter beda (Compile-time Polymorphism)
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndStatus(User.Role role, User.AccountStatus status);  // Overload

    User createUser(RegisterDTO dto);
    User updateProfile(String userId, ProfileDTO dto);
    boolean verifyPassword(String rawPassword, String hashedPassword);
}

// UserServiceImpl.java — @Override (Run-time Polymorphism)
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<User> findByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public List<User> findByRoleAndStatus(User.Role role, User.AccountStatus status) {
        return userRepository.findByRoleAndAccountStatus(role, status);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public User createUser(RegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }
        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new RuntimeException("Nomor HP sudah terdaftar");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.WARGA);
        user.setAccountStatus(User.AccountStatus.PENDING);

        return userRepository.save(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public User updateProfile(String userId, ProfileDTO dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber().isBlank() ? null : dto.getPhoneNumber());
        }

        return userRepository.save(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
```

#### 3. ABSTRACTION — Interface sebagai kontrak

```java
// Controller TIDAK PERNAH akses UserRepository langsung
// Controller hanya tahu Interface, IMPLEMENTASI disembunyikan

@Controller
public class WebController {

    @Autowired
    private UserService userService;  // ← Abstraction: hanya tahu interface

    @PostMapping("/warga/login")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        HttpSession session) {
        // Abstraction: Controller tidak tahu:
        // - Bagaimana hash password diverifikasi
        // - Bagaimana session dibuat
        // - Bagaimana LoginAttempt dicatat
        // Semua itu DIURUS di UserServiceImpl

        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (userService.verifyPassword(password, user.getPasswordHash())) {
                session.setAttribute("userId", user.getUserId());
                session.setAttribute("userName", user.getFullName());
                return "redirect:/warga/dashboard";
            }
        }
        return "redirect:/warga/login?error=true";
    }
}
```

#### 4. ENCAPSULATION — Private fields + DTO + Immutable

```java
// A. DTO — pisahkan data form dari Entity
public class LoginDTO {
    // Enkapsulasi: field private
    private String email;
    private String password;

    // Enkapsulasi: hanya getter & setter
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

// B. RegisterDTO
public class RegisterDTO {
    private String fullName;    // Private — Enkapsulasi
    private String email;       // Private — Enkapsulasi
    private String phoneNumber; // Private — Enkapsulasi
    private String password;   // Private — Enkapsulasi
    private String nik;        // Private — Enkapsulasi

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    // ... getter & setter untuk semua field
}

// C. ProfileDTO
public class ProfileDTO {
    private String fullName;     // Private — Enkapsulasi
    private String email;        // Private — Enkapsulasi
    private String phoneNumber;   // Private — Enkapsulasi
    private String nik;          // Private — Enkapsulasi
    private String alamatLengkap; // Private — Enkapsulasi

    // Getter & Setter
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    // ... getter & setter untuk semua field
}
```

---

## 👤 ORANG 2 — MODUL PELAPORAN & NOTIFIKASI

**Branch:** `fitur/modul-2-reporting`
**Deskripsi:** Warga membuat laporan, admin memvalidasi laporan masuk, warga menerima notifikasi.

### 📁 FILE YANG DIKERJAKAN

#### Entity — WAJIB extends BaseEntity
```
src/main/java/com/plr/aduaja/model/
├── Report.java                    ← extends BaseEntity
├── ReportCategory.java           ← extends BaseEntity
├── ReportRevision.java           ← extends BaseEntity
└── Notification.java             ← extends BaseEntity
```

#### Service + Interface (WAJIB: Interface + Impl + @Override + Overload)
```
src/main/java/com/plr/aduaja/service/
├── ReportService.java             ← INTERFACE (Abstraction)
└── ReportServiceImpl.java        ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── NotificationService.java       ← INTERFACE (Abstraction)
└── NotificationServiceImpl.java  ← @Override + Overloading (Polymorphism)
```

#### DTO (WAJIB — untuk semua form input)
```
src/main/java/com/plr/aduaja/dto/
├── CreateReportDTO.java           ← ENCAPSULATION: pisahkan dari Entity
├── ReportFilterDTO.java          ← ENCAPSULATION: pisahkan dari Entity
└── NotificationDTO.java          ← ENCAPSULATION: pisahkan dari Entity
```

#### View HTML (5 files)
```
src/main/resources/templates/
├── warga/
│   ├── create-report.html
│   ├── report-history.html
│   ├── report-detail.html
│   ├── notifications.html
│   └── dashboard.html
└── admin/
    ├── validation-panel.html
    └── queue-detail.html
```

### 🛣️ ROUTE (Endpoint)

| Method | Path | Fungsi |
|--------|------|--------|
| GET | `/warga/create-report` | Form buat laporan baru |
| POST | `/warga/create-report` | Simpan laporan baru |
| GET | `/warga/report-history` | Daftar semua laporan warga |
| GET | `/warga/report-detail` | Detail satu laporan |
| GET | `/warga/notifications` | Daftar notifikasi |
| POST | `/warga/notifications/mark-read` | Tandai sudah dibaca |
| GET | `/admin/validation` | Panel validasi laporan masuk |
| POST | `/admin/validation` | Proses validasi (terima/tolak) |
| GET | `/admin/laporan-queue` | Antrean laporan |
| GET | `/admin/queue-detail` | Detail laporan di antrean |

### 🎯 PBO EVIDENCE (4 Pilar) — KODE YANG BENAR

#### 1. INHERITANCE — extends BaseEntity

```java
// Report.java — INHERITANCE sejati
@Entity
@Table(name = "reports")
public class Report extends BaseEntity {  // ← INHERITANCE
    // extends BaseEntity → ADA createdAt & updatedAt OTOMATIS

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.MENUNGGU_VALIDASI;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
    private List<ReportRevision> revisions = new ArrayList<>();  // HAS-A (Composition)

    @OneToOne(mappedBy = "report", cascade = CascadeType.ALL)
    private SlaRecord slaRecord;  // HAS-A (Composition)

    public enum ReportStatus {
        MENUNGGU_VALIDASI, PERLU_REVISI, DITOLAK, DIVALIDASI,
        DIDISPOSISI, DITUGASKAN, SEDANG_DIKERJAKAN, TERTUNDA,
        MENUNGGU_KONFIRMASI, SELESAI, SENGKETA, DITUTUP
    }

    // Getter & Setter
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
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    // ... semua getter/setter
}
```

#### 2. POLYMORPHISM — @Override + Overloading

```java
// ReportService.java — Interface
public interface ReportService {
    // OVERLOADING: 4 method berbeda parameter
    Report createReport(CreateReportDTO dto, String wargaId);
    Optional<Report> findById(String id);
    Optional<Report> findByTicketNumber(String ticketNumber);
    List<Report> getAllReports();
    List<Report> getReportsByStatus(ReportStatus status);
    List<Report> getReportsByWarga(String wargaId);
    List<Report> getReportsByDateRange(LocalDate start, LocalDate end);  // OVERLOAD
    List<Report> getReportsByStatusAndDateRange(ReportStatus status,     // OVERLOAD
                                                  LocalDate start, LocalDate end);
    Report updateStatus(String reportId, ReportStatus newStatus);
}

// ReportServiceImpl.java — @Override (Run-time Polymorphism)
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportCategoryRepository categoryRepository;

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<Report> findById(String id) {
        return reportRepository.findById(id);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<Report> findByTicketNumber(String ticketNumber) {
        return reportRepository.findByTicketNumber(ticketNumber);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<Report> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatus(status);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<Report> getReportsByWarga(String wargaId) {
        User user = new User();
        user.setUserId(wargaId);
        return reportRepository.findByReporter(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public List<Report> getReportsByDateRange(LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);
        return reportRepository.findByDateRange(startDt, endDt);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public List<Report> getReportsByStatusAndDateRange(ReportStatus status,
                                                        LocalDate start,
                                                        LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.atTime(23, 59, 59);
        return reportRepository.findByStatusAndDateRange(status, startDt, endDt);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Report createReport(CreateReportDTO dto, String wargaId) {
        Report report = new Report();
        report.setTicketNumber(generateTicketNumber());
        report.setDescription(dto.getDescription());
        report.setLocationHint(dto.getLocationHint());
        report.setLatitude(dto.getLatitude());
        report.setLongitude(dto.getLongitude());

        // Set reporter (HAS-A relationship)
        User reporter = new User();
        reporter.setUserId(wargaId);
        report.setReporter(reporter);

        // Set category (HAS-A relationship)
        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId())
                .ifPresent(report::setCategory);
        }

        report.setStatus(ReportStatus.MENUNGGU_VALIDASI);
        report.setSubmittedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public Report updateStatus(String reportId, ReportStatus newStatus) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));
        report.setStatus(newStatus);
        report.setUpdatedAt(LocalDateTime.now());  // Trigger BaseEntity onUpdate
        return reportRepository.save(report);
    }

    private String generateTicketNumber() {
        return "RPT-" + System.currentTimeMillis();
    }
}
```

#### 3. ABSTRACTION — Interface menyembunyikan kompleksitas

```java
// Controller hanya tahu ReportService, tidak tahu:
// - Bagaimana ticket number di-generate
// - Bagaimana geolocation di-attach
// - Bagaimana SLA record dibuat setelah validasi
// - Bagaimana notification dikirim

@Controller
public class WebController {

    @Autowired
    private ReportService reportService;  // ← Abstraction

    @PostMapping("/warga/create-report")
    public String createReport(@ModelAttribute CreateReportDTO dto,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/warga/login";
        }

        // Abstraction: 1 baris, semua kompleksitas disembunyikan
        Report report = reportService.createReport(dto, userId);

        redirectAttributes.addFlashAttribute("success",
            "Laporan berhasil dibuat. Nomor tiket: " + report.getTicketNumber());
        return "redirect:/warga/report-history";
    }
}
```

#### 4. ENCAPSULATION — DTO + Private fields

```java
// CreateReportDTO.java — ENCAPSULATION
public class CreateReportDTO {
    // Enkapsulasi: semua field PRIVATE
    private String description;
    private String locationHint;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String categoryId;
    private MultipartFile photo;

    // Enkapsulasi: hanya getter/setter
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocationHint() { return locationHint; }
    public void setLocationHint(String locationHint) { this.locationHint = locationHint; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public MultipartFile getPhoto() { return photo; }
    public void setPhoto(MultipartFile photo) { this.photo = photo; }
}

// Thymeleaf form binding:
/*
<form th:action="@{/warga/create-report}" method="post" th:object="${createReportDTO}">
    <textarea th:field="*{description}"></textarea>
    <input type="text" th:field="*{locationHint}" />
    <input type="file" th:field="*{photo}" />
</form>
*/
```

---

## 👤 ORANG 3 — MODUL DISPOSISI & EKSEKUSI LAPANGAN

**Branch:** `fitur/modul-3-disposisi-task`
**Deskripsi:** Admin pusat mendisposisi laporan ke dinas, dinas assign ke petugas, petugas eksekusi tugas di lapangan.

### 📁 FILE YANG DIKERJAKAN

#### Entity — WAJIB extends BaseEntity
```
src/main/java/com/plr/aduaja/model/
├── Disposition.java               ← extends BaseEntity
├── Agency.java                   ← extends BaseEntity
├── FieldTask.java               ← extends BaseEntity
├── TaskEvidence.java            ← extends BaseEntity
├── TaskPostponement.java         ← extends BaseEntity
└── OfficerAttendance.java        ← extends BaseEntity
```

#### Service + Interface (WAJIB: Interface + Impl + @Override + Overload)
```
src/main/java/com/plr/aduaja/service/
├── DispositionService.java        ← INTERFACE (Abstraction)
└── DispositionServiceImpl.java   ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── FieldTaskService.java          ← INTERFACE (Abstraction)
└── FieldTaskServiceImpl.java    ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── AttendanceService.java         ← INTERFACE (Abstraction)
└── AttendanceServiceImpl.java   ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── AgencyService.java            ← INTERFACE (Abstraction)
└── AgencyServiceImpl.java       ← @Override + Overloading (Polymorphism)
```

#### DTO (WAJIB — untuk semua form input)
```
src/main/java/com/plr/aduaja/dto/
├── DispositionDTO.java            ← ENCAPSULATION: pisahkan dari Entity
├── TaskExecutionDTO.java          ← ENCAPSULATION: pisahkan dari Entity
└── AttendanceDTO.java            ← ENCAPSULATION: pisahkan dari Entity
```

#### View HTML (11 files)
```
src/main/resources/templates/
├── admin/
│   ├── disposisi-panel.html
│   ├── disposisi-detail.html
│   └── dinas/
│       ├── dinas-queue.html
│       ├── penugasan-petugas.html
│       ├── progress-update.html
│       ├── close-ticket.html
│       └── sengketa-dinas.html
└── petugas/
    ├── dashboard.html
    ├── tasks.html
    ├── task-detail.html
    ├── task-execution.html
    ├── history.html
    └── attendance-history.html
```

### 🛣️ ROUTE (Endpoint)

| Method | Path | Fungsi |
|--------|------|--------|
| GET | `/admin/disposisi` | Panel disposisi laporan |
| POST | `/admin/disposisi` | Kirim laporan ke dinas |
| GET | `/admin/disposisi-detail` | Detail disposisi |
| GET | `/admin/dinas/dashboard` | Dashboard admin dinas |
| GET | `/admin/dinas/queue` | Antrean tugas di dinas |
| GET | `/admin/dinas/penugasan` | Form assign ke petugas |
| POST | `/admin/dinas/penugasan` | Simpan assignment |
| POST | `/admin/dinas/progress` | Update progress |
| POST | `/admin/dinas/close` | Tutup tiket |
| GET | `/petugas/dashboard` | Dashboard petugas |
| GET | `/petugas/tasks` | Daftar tugas petugas |
| GET | `/petugas/task-detail` | Detail tugas |
| GET | `/petugas/task-execution` | Form eksekusi tugas |
| POST | `/petugas/task-execution` | Simpan bukti foto |
| POST | `/petugas/task-action` | Aksi (mulai/selesai/ditunda) |
| GET | `/petugas/history` | Riwayat tugas |
| GET | `/petugas/attendance-history` | Riwayat absensi |

### 🎯 PBO EVIDENCE (4 Pilar) — KODE YANG BENAR

#### 1. INHERITANCE — extends BaseEntity

```java
// FieldTask.java — INHERITANCE sejati
@Entity
@Table(name = "field_tasks")
public class FieldTask extends BaseEntity {  // ← INHERITANCE
    // extends BaseEntity → ADA createdAt & updatedAt OTOMATIS

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "task_id")
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;  // HAS-A (ManyToOne) ≠ Inheritance

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private User officer;  // HAS-A (ManyToOne) ≠ Inheritance

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;  // HAS-A (ManyToOne) ≠ Inheritance

    // ENCAPSULASI: GPS coordinates PRIVATE
    @Column(name = "officer_latitude", precision = 10, scale = 8)
    private BigDecimal officerLatitude;  // PRIVATE

    @Column(name = "officer_longitude", precision = 11, scale = 8)
    private BigDecimal officerLongitude;  // PRIVATE

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false)
    private TaskStatus taskStatus = TaskStatus.BARU;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum TaskStatus { BARU, SEDANG_DIKERJAKAN, TERTUNDA, SELESAI, DITUGASKAN_ULANG }

    // Getter & Setter
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }
    public User getOfficer() { return officer; }
    public void setOfficer(User officer) { this.officer = officer; }
    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }
    public BigDecimal getOfficerLatitude() { return officerLatitude; }
    public void setOfficerLatitude(BigDecimal officerLatitude) { this.officerLatitude = officerLatitude; }
    public BigDecimal getOfficerLongitude() { return officerLongitude; }
    public void setOfficerLongitude(BigDecimal officerLongitude) { this.officerLongitude = officerLongitude; }
    public TaskStatus getTaskStatus() { return taskStatus; }
    public void setTaskStatus(TaskStatus taskStatus) { this.taskStatus = taskStatus; }
    // ... semua getter/setter
}
```

#### 2. POLYMORPHISM — @Override + Overloading

```java
// FieldTaskService.java — Interface
public interface FieldTaskService {
    // OVERLOADING: method sama, parameter beda
    List<FieldTask> getAllTasks();
    List<FieldTask> getTasksByOfficer(String officerId);
    List<FieldTask> getTasksByStatus(FieldTask.TaskStatus status);
    List<FieldTask> getTasksByStatusAndOfficer(FieldTask.TaskStatus status, String officerId); // OVERLOAD
    List<FieldTask> getTasksByDateRange(LocalDateTime start, LocalDateTime end);  // OVERLOAD

    Optional<FieldTask> findById(String taskId);
    FieldTask assignTask(String reportId, String officerId, String assignedById);
    FieldTask startTask(String taskId, BigDecimal latitude, BigDecimal longitude);
    FieldTask completeTask(String taskId, String evidencePhotoUrl);
    FieldTask postponeTask(String taskId, String reason);
    FieldTask reassignTask(String taskId, String newOfficerId);
}

// FieldTaskServiceImpl.java — @Override (Run-time Polymorphism)
@Service
public class FieldTaskServiceImpl implements FieldTaskService {

    @Autowired
    private FieldTaskRepository fieldTaskRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<FieldTask> getAllTasks() {
        return fieldTaskRepository.findAll();
    }

    @Override
    public List<FieldTask> getTasksByOfficer(String officerId) {
        User officer = userRepository.findById(officerId)
            .orElseThrow(() -> new RuntimeException("Officer tidak ditemukan"));
        return fieldTaskRepository.findByOfficer(officer);
    }

    @Override
    public List<FieldTask> getTasksByStatus(FieldTask.TaskStatus status) {
        return fieldTaskRepository.findByTaskStatus(status);
    }

    @Override  // ← OVERLOAD: 2 parameter
    public List<FieldTask> getTasksByStatusAndOfficer(FieldTask.TaskStatus status, String officerId) {
        User officer = userRepository.findById(officerId)
            .orElseThrow(() -> new RuntimeException("Officer tidak ditemukan"));
        return fieldTaskRepository.findByTaskStatusAndOfficer(status, officer);
    }

    @Override  // ← OVERLOAD: 1 parameter date range
    public List<FieldTask> getTasksByDateRange(LocalDateTime start, LocalDateTime end) {
        return fieldTaskRepository.findByDateRange(start, end);
    }

    @Override
    public Optional<FieldTask> findById(String taskId) {
        return fieldTaskRepository.findById(taskId);
    }

    @Override
    public FieldTask assignTask(String reportId, String officerId, String assignedById) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report tidak ditemukan"));
        User officer = userRepository.findById(officerId)
            .orElseThrow(() -> new RuntimeException("Officer tidak ditemukan"));
        User assignedBy = userRepository.findById(assignedById)
            .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

        FieldTask task = new FieldTask();
        task.setReport(report);
        task.setOfficer(officer);
        task.setAssignedBy(assignedBy);
        task.setTaskStatus(FieldTask.TaskStatus.BARU);

        return fieldTaskRepository.save(task);
    }

    @Override
    public FieldTask startTask(String taskId, BigDecimal latitude, BigDecimal longitude) {
        FieldTask task = fieldTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));

        task.setTaskStatus(FieldTask.TaskStatus.SEDANG_DIKERJAKAN);
        task.setStartedAt(LocalDateTime.now());
        task.setOfficerLatitude(latitude);
        task.setOfficerLongitude(longitude);

        return fieldTaskRepository.save(task);
    }

    @Override
    public FieldTask completeTask(String taskId, String evidencePhotoUrl) {
        FieldTask task = fieldTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));

        task.setTaskStatus(FieldTask.TaskStatus.SELESAI);
        task.setCompletedAt(LocalDateTime.now());

        TaskEvidence evidence = new TaskEvidence();
        evidence.setTask(task);
        evidence.setPhotoUrl(evidencePhotoUrl);
        // ... save evidence

        return fieldTaskRepository.save(task);
    }

    @Override
    public FieldTask postponeTask(String taskId, String reason) {
        FieldTask task = fieldTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));

        task.setTaskStatus(FieldTask.TaskStatus.TERTUNDA);

        TaskPostponement postponement = new TaskPostponement();
        postponement.setTask(task);
        postponement.setReason(reason);
        postponement.setPostponedAt(LocalDateTime.now());
        // ... save postponement

        return fieldTaskRepository.save(task);
    }

    @Override
    public FieldTask reassignTask(String taskId, String newOfficerId) {
        FieldTask task = fieldTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));
        User newOfficer = userRepository.findById(newOfficerId)
            .orElseThrow(() -> new RuntimeException("Officer tidak ditemukan"));

        task.setOfficer(newOfficer);
        task.setTaskStatus(FieldTask.TaskStatus.DITUGASKAN_ULANG);

        return fieldTaskRepository.save(task);
    }
}
```

#### 3. ABSTRACTION — Interface menyembunyikan kompleksitas

```java
// Controller TIDAK PERNAH akses FieldTaskRepository langsung
// Semua logika kompleks disembunyikan di ServiceImpl

@Controller
public class WebController {

    @Autowired
    private FieldTaskService fieldTaskService;  // ← Abstraction

    @PostMapping("/petugas/task-action")
    public String taskAction(@RequestParam("taskId") String taskId,
                              @RequestParam("action") String action,
                              HttpSession session) {
        String officerId = (String) session.getAttribute("userId");
        if (officerId == null) {
            return "redirect:/petugas/login";
        }

        // Abstraction: 1 baris, banyak proses disembunyikan:
        // - action="start" → FieldTaskServiceImpl.startTask()
        // - action="complete" → FieldTaskServiceImpl.completeTask()
        // - action="postpone" → FieldTaskServiceImpl.postponeTask()
        // - action="reassign" → FieldTaskServiceImpl.reassignTask()

        switch (action) {
            case "start" -> fieldTaskService.startTask(taskId, null, null);
            case "complete" -> fieldTaskService.completeTask(taskId, null);
            case "postpone" -> fieldTaskService.postponeTask(taskId, "Alasan ditunda");
            case "reassign" -> fieldTaskService.reassignTask(taskId, officerId);
        }

        return "redirect:/petugas/task-detail?id=" + taskId;
    }
}
```

#### 4. ENCAPSULATION — Private fields + DTO

```java
// TaskExecutionDTO.java — ENCAPSULATION
public class TaskExecutionDTO {
    // Enkapsulasi: semua field PRIVATE
    private String taskId;
    private String description;
    private MultipartFile evidencePhoto;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;

    // Enkapsulasi: hanya getter/setter
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MultipartFile getEvidencePhoto() { return evidencePhoto; }
    public void setEvidencePhoto(MultipartFile evidencePhoto) { this.evidencePhoto = evidencePhoto; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
```

---

## 👤 ORANG 4 — MODUL SLA, SENGKETA & AUDIT

**Branch:** `fitur/modul-4-sla-dispute`
**Deskripsi:** Hitung mundur deadline otomatis, merge laporan ganda, tangani sengketa warga, catat jejak audit.

### 📁 FILE YANG DIKERJAKAN

#### Entity — WAJIB extends BaseEntity
```
src/main/java/com/plr/aduaja/model/
├── SlaRecord.java                 ← extends BaseEntity
├── SlaPauseLog.java               ← extends BaseEntity
├── ConfirmationRequest.java      ← extends BaseEntity
├── DisputeRecord.java            ← extends BaseEntity
├── MergeRecord.java              ← extends BaseEntity
├── ValidationDecision.java       ← extends BaseEntity
└── AuditLog.java                ← extends BaseEntity + IMMUTABLE
```

#### Service + Interface (WAJIB: Interface + Impl + @Override + Overload)
```
src/main/java/com/plr/aduaja/service/
├── SlaRecordService.java          ← INTERFACE (Abstraction)
└── SlaRecordServiceImpl.java      ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── SlaMonitoringService.java      ← INTERFACE (Abstraction)
└── SlaMonitoringServiceImpl.java ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── DisputeService.java            ← INTERFACE (Abstraction)
└── DisputeServiceImpl.java       ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── MergeRecordService.java        ← INTERFACE (Abstraction)
└── MergeRecordServiceImpl.java   ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── ConfirmationService.java      ← INTERFACE (Abstraction)
└── ConfirmationServiceImpl.java ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── ValidationDecisionService.java ← INTERFACE (Abstraction)
└── ValidationDecisionServiceImpl.java ← @Override + Overloading (Polymorphism)

src/main/java/com/plr/aduaja/service/
├── AuditLogService.java          ← INTERFACE (Abstraction)
└── AuditLogServiceImpl.java     ← @Override + Overloading (Polymorphism)
```

#### DTO (WAJIB — untuk semua form input)
```
src/main/java/com/plr/aduaja/dto/
├── SlaStatusDTO.java
├── DisputeDTO.java
├── MergeDTO.java
└── AuditLogDTO.java
```

#### View HTML (5 files)
```
src/main/resources/templates/
├── admin/
│   ├── sengketa-panel.html
│   ├── merge-ticket-panel.html
│   ├── validation-panel.html
│   └── dinas/
│       └── sengketa-dinas.html
└── warga/
    └── report-detail.html  ← (bagian sengketa warga saja)
```

### 🛣️ ROUTE (Endpoint)

| Method | Path | Fungsi |
|--------|------|--------|
| GET | `/admin/sengketa` | Panel sengketa masuk |
| POST | `/admin/sengketa` | Proses keputusan sengketa |
| GET | `/admin/merge` | Panel merge laporan ganda |
| POST | `/admin/merge` | Merge 2 laporan |
| GET | `/admin/validation` | Panel keputusan validasi |
| POST | `/admin/validation` | Simpan keputusan validasi |
| GET | `/admin/dinas/sengketa` | Sengketa di level dinas |

### 🎯 PBO EVIDENCE (4 Pilar) — KODE YANG BENAR

#### 1. INHERITANCE — extends BaseEntity

```java
// SlaRecord.java — INHERITANCE sejati
@Entity
@Table(name = "sla_records")
public class SlaRecord extends BaseEntity {  // ← INHERITANCE
    // extends BaseEntity → ADA createdAt & updatedAt OTOMATIS

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sla_id")
    private String slaId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;  // HAS-A (OneToOne) ≠ Inheritance

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

    public enum SlaStatus { BERJALAN, TERTUNDA, TERLAMBAT, SELESAI }

    // Getter & Setter
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

#### 2. POLYMORPHISM — @Override + Overloading

```java
// SlaRecordService.java — Interface
public interface SlaRecordService {
    // OVERLOADING: 4+ method berbeda
    Optional<SlaRecord> findById(String id);
    Optional<SlaRecord> findByReportId(String reportId);
    List<SlaRecord> getAllSlaRecords();
    List<SlaRecord> getSlaRecordsByStatus(SlaStatus status);  // OVERLOAD
    List<SlaRecord> getOverdueSlaRecords();                   // OVERLOAD
    List<SlaRecord> getSlaRecordsByDateRange(LocalDateTime start, LocalDateTime end);  // OVERLOAD

    SlaRecord createSlaRecord(String reportId, Integer durationHours);
    SlaRecord pauseSla(String slaId, String reason);
    SlaRecord resumeSla(String slaId);
    SlaRecord completeSla(String slaId);
    void checkAndUpdateOverdueSla();
}

// SlaRecordServiceImpl.java — @Override (Run-time Polymorphism)
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
    public List<SlaRecord> getAllSlaRecords() {
        return slaRecordRepository.findAll();
    }

    @Override  // ← OVERLOAD: filter by status
    public List<SlaRecord> getSlaRecordsByStatus(SlaStatus status) {
        return slaRecordRepository.findByCurrentStatus(status);
    }

    @Override  // ← OVERLOAD: overdue only
    public List<SlaRecord> getOverdueSlaRecords() {
        return slaRecordRepository.findOverdue();
    }

    @Override  // ← OVERLOAD: date range
    public List<SlaRecord> getSlaRecordsByDateRange(LocalDateTime start, LocalDateTime end) {
        return slaRecordRepository.findByDateRange(start, end);
    }

    @Override
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
    public SlaRecord pauseSla(String slaId, String reason) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
            .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan"));

        // Log pause (HAS-A relationship)
        SlaPauseLog pauseLog = new SlaPauseLog();
        pauseLog.setSlaRecord(sla);
        pauseLog.setPausedAt(LocalDateTime.now());
        pauseLog.setReason(reason);
        sla.getPauseLogs().add(pauseLog);

        sla.setCurrentStatus(SlaStatus.TERTUNDA);
        return slaRecordRepository.save(sla);
    }

    @Override
    public SlaRecord resumeSla(String slaId) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
            .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan"));

        sla.setCurrentStatus(SlaStatus.BERJALAN);
        return slaRecordRepository.save(sla);
    }

    @Override
    public SlaRecord completeSla(String slaId) {
        SlaRecord sla = slaRecordRepository.findById(slaId)
            .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan"));

        sla.setCurrentStatus(SlaStatus.SELESAI);
        sla.setCompletedAt(LocalDateTime.now());
        return slaRecordRepository.save(sla);
    }

    @Override
    public void checkAndUpdateOverdueSla() {
        List<SlaRecord> activeSlas = slaRecordRepository.findByCurrentStatus(SlaStatus.BERJALAN);
        LocalDateTime now = LocalDateTime.now();

        for (SlaRecord sla : activeSlas) {
            LocalDateTime effectiveDeadline = sla.getSlaDeadlineAt()
                .minusMinutes(sla.getTotalPausedMinutes());

            if (now.isAfter(effectiveDeadline)) {
                sla.setCurrentStatus(SlaStatus.TERTUNDA);
                slaRecordRepository.save(sla);
            }
        }
    }
}
```

#### 3. ABSTRACTION — Interface menyembunyikan kompleksitas

```java
// Controller TIDAK PERNAH tahu:
// - Bagaimana calculate effective deadline
// - Bagaimana pause minutes dihitung
// - Bagaimana notification SLA warning dikirim
// Semua disembunyikan di SlaMonitoringServiceImpl

@Controller
public class WebController {

    @Autowired
    private SlaRecordService slaRecordService;  // ← Abstraction

    @GetMapping("/admin/sla-monitoring")
    public String slaMonitoring(Model model) {
        // Abstraction: hanya tahu ada "SLA Records", tidak tahu detail
        List<SlaRecord> overdue = slaRecordService.getOverdueSlaRecords();
        List<SlaRecord> berjalan = slaRecordService.getSlaRecordsByStatus(SlaStatus.BERJALAN);
        List<SlaRecord> selesai = slaRecordService.getSlaRecordsByStatus(SlaStatus.SELESAI);

        model.addAttribute("overdueCount", overdue.size());
        model.addAttribute("berjalanCount", berjalan.size());
        model.addAttribute("selesaiCount", selesai.size());
        model.addAttribute("overdueList", overdue);
        model.addAttribute("berjalanList", berjalan);

        return "admin/sla-monitoring";
    }
}
```

#### 4. ENCAPSULATION — Private fields + DTO + IMMUTABLE AuditLog

```java
// AuditLog.java — INHERITANCE + ENCAPSULATION + IMMUTABLE
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {  // ← INHERITANCE
    // extends BaseEntity → ADA createdAt & updatedAt OTOMATIS

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private FieldTask task;

    // ENCAPSULATION: field PRIVATE, TIDAK ADA SETTER PUBLIK
    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;  // PRIVATE — tidak ada public setter

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;  // PRIVATE — tidak ada public setter

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    // Getter only — ENCAPSULATION (no setters for sensitive fields)
    public String getLogId() { return logId; }
    public User getActor() { return actor; }
    public Report getReport() { return report; }
    public FieldTask getTask() { return task; }
    public String getActionType() { return actionType; }
    public String getOldValue() { return oldValue; }  // READ ONLY
    public String getNewValue() { return newValue; }  // READ ONLY
    public String getIpAddress() { return ipAddress; }
    public String getDeviceInfo() { return deviceInfo; }
    public LocalDateTime getLoggedAt() { return loggedAt; }

    // Package-private setters (hanya bisa diakses oleh AuditLogService)
    void setActor(User actor) { this.actor = actor; }
    void setReport(Report report) { this.report = report; }
    void setTask(FieldTask task) { this.task = task; }
    void setActionType(String actionType) { this.actionType = actionType; }
    void setOldValue(String oldValue) { this.oldValue = oldValue; }  // Package-only
    void setNewValue(String newValue) { this.newValue = newValue; }  // Package-only
    void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}

// AuditLogService.java — ENCAPSULATION
public interface AuditLogService {
    // OVERLOADING: method sama, parameter beda
    AuditLog logAction(User actor, String actionType, String oldVal, String newVal);
    AuditLog logReportAction(User actor, Report report, String action,
                              String oldVal, String newVal);  // OVERLOAD
    AuditLog logTaskAction(User actor, FieldTask task, String action,
                            String oldVal, String newVal);  // OVERLOAD

    List<AuditLog> getLogsByReport(String reportId);
    List<AuditLog> getLogsByActor(String actorId);
}

// AuditLogServiceImpl.java — @Override (Run-time Polymorphism)
@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public AuditLog logAction(User actor, String actionType, String oldVal, String newVal) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setActionType(actionType);
        log.setOldValue(oldVal);  // Set via package-private setter
        log.setNewValue(newVal);  // Set via package-private setter
        log.setLoggedAt(LocalDateTime.now());
        log.setIpAddress(getCurrentIpAddress());
        log.setDeviceInfo(getCurrentDeviceInfo());
        return auditLogRepository.save(log);
    }

    @Override  // ← OVERLOAD: 1 parameter lebih banyak (Report)
    public AuditLog logReportAction(User actor, Report report, String action,
                                      String oldVal, String newVal) {
        AuditLog log = logAction(actor, action, oldVal, newVal);  // Reuse
        log.setReport(report);
        return auditLogRepository.save(log);
    }

    @Override  // ← OVERLOAD: 1 parameter lebih banyak (FieldTask)
    public AuditLog logTaskAction(User actor, FieldTask task, String action,
                                   String oldVal, String newVal) {
        AuditLog log = logAction(actor, action, oldVal, newVal);  // Reuse
        log.setTask(task);
        return auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> getLogsByReport(String reportId) {
        Report report = new Report();
        report.setReportId(reportId);
        return auditLogRepository.findByReport(report);
    }

    @Override
    public List<AuditLog> getLogsByActor(String actorId) {
        User actor = new User();
        actor.setUserId(actorId);
        return auditLogRepository.findByActor(actor);
    }

    private String getCurrentIpAddress() {
        // Implementation
        return "0.0.0.0";
    }

    private String getCurrentDeviceInfo() {
        // Implementation
        return "Unknown";
    }
}
```

---

---

## 📊 MATRIKS KEPEMILIKAN FILE

### Legend: OWNS = Edit/Create | READ = Inject & baca | SKIP = Tidak berhubungan

| File | Orang 1 | Orang 2 | Orang 3 | Orang 4 |
|------|:-------:|:-------:|:-------:|:-------:|
| **Shared Infrastructure (DO NOT TOUCH tanpa consensus)** | | | | |
| `BaseEntity.java` | OWNS (create) | READ | READ | READ |
| `Region.java` | READ | READ | READ | READ |
| `User.java` (base fields) | OWNS | READ | READ | READ |
| **Orang 1 — Auth** | | | | |
| `User.java` (auth fields) | OWNS | | | |
| `UserProfile.java` | OWNS | | | |
| `LoginAttempt.java` | OWNS | | | |
| `OtpVerification.java` | OWNS | | | |
| `ActiveSession.java` | OWNS | | | |
| `UserService.java` (Interface) | OWNS | | | |
| `UserServiceImpl.java` (@Override) | OWNS | | | |
| `LoginDTO.java` | OWNS | | | |
| `RegisterDTO.java` | OWNS | | | |
| `ProfileDTO.java` | OWNS | | | |
| `warga/login.html` | OWNS | | | |
| `warga/profile.html` | OWNS | | | |
| **Orang 2 — Reporting** | | | | |
| `Report.java` | | OWNS | | |
| `ReportCategory.java` | | OWNS | | |
| `ReportRevision.java` | | OWNS | | |
| `Notification.java` | | OWNS | | |
| `ReportService.java` (Interface) | | OWNS | | |
| `ReportServiceImpl.java` (@Override) | | OWNS | | |
| `CreateReportDTO.java` | | OWNS | | |
| `ReportFilterDTO.java` | | OWNS | | |
| `warga/create-report.html` | | OWNS | | |
| `warga/report-history.html` | | OWNS | | |
| `warga/notifications.html` | | OWNS | | |
| **Orang 3 — Disposisi** | | | | |
| `Disposition.java` | | | OWNS | |
| `Agency.java` | | | OWNS | |
| `FieldTask.java` | | | OWNS | |
| `TaskEvidence.java` | | | OWNS | |
| `TaskPostponement.java` | | | OWNS | |
| `OfficerAttendance.java` | | | OWNS | |
| `FieldTaskService.java` (Interface) | | | OWNS | |
| `FieldTaskServiceImpl.java` (@Override) | | | OWNS | |
| `DispositionDTO.java` | | | OWNS | |
| `TaskExecutionDTO.java` | | | OWNS | |
| `admin/disposisi-panel.html` | | | OWNS | |
| `petugas/dashboard.html` | | | OWNS | |
| **Orang 4 — SLA** | | | | |
| `SlaRecord.java` | | | | OWNS |
| `SlaPauseLog.java` | | | | OWNS |
| `ConfirmationRequest.java` | | | | OWNS |
| `DisputeRecord.java` | | | | OWNS |
| `MergeRecord.java` | | | | OWNS |
| `ValidationDecision.java` | | | | OWNS |
| `AuditLog.java` | | | | OWNS |
| `SlaRecordService.java` (Interface) | | | | OWNS |
| `SlaRecordServiceImpl.java` (@Override) | | | | OWNS |
| `AuditLogService.java` (Interface) | | | | OWNS |
| `AuditLogServiceImpl.java` (@Override) | | | | OWNS |
| `admin/sengketa-panel.html` | | | | OWNS |
| `admin/merge-ticket-panel.html` | | | | OWNS |

---

---

## ✅ PBO EVIDENCE SUMMARY (FINAL — KOREKSI DARI VERSI LAMA)

### Apa yang BENAR dari Versi Lama:
- Vertical Slicing (pembagian per modul) ✅
- Git conflict prevention rules ✅
- Ownership matrix ✅
- Abstraction via Interface pattern ✅
- Encapsulation via DTO ✅

### Apa yang SALAH & SUDAH DIFIX di Versi Ini:

| Pilar PBO | Versi Lama (SALAH) | Versi Ini (BENAR) |
|-----------|-------------------|-------------------|
| **Inheritance** | `@OneToOne`, `@ManyToOne` disebut "Inheritance" | HARUS `extends BaseEntity` dengan `@MappedSuperclass` |
| **Polymorphism** | Enum switch-case disebut "Polymorphism" | HARUS `@Override` pada ServiceImpl + Overloading method |
| **Abstraction** | ✅ Sudah benar | ✅ Tetap: Controller → Service Interface → ServiceImpl |
| **Encapsulation** | ✅ Sudah benar | ✅ Tetap: Private fields + DTO + Immutable AuditLog |

### Checklist Wajib Setiap Developer:

```
SEBELUM COMMIT, PASTIKAN:

[ ] Entity ku extends BaseEntity
[ ] Ada file XxxService.java (Interface)
[ ] Ada file XxxServiceImpl.java (@Override)
[ ] Minimal 1 method overloaded di Service
[ ] Ada file XxxDTO.java untuk setiap form HTML
[ ] Semua field di Entity = private
[ ] AuditLog = package-private setter (immutable)
```

---

---

## 🚀 CHECKLIST SEBELUM START CODING

### Yang harus dilakukan Orang 1 duluan (SEKALI):

```bash
# 1. Buat BaseEntity.java
git add src/main/java/com/plr/aduaja/model/BaseEntity.java
git commit -m "feat: BaseEntity @MappedSuperclass (INHERITANCE parent)"
git push origin main

# 2. Semua orang pull & modifikasi entity -> extends BaseEntity
git pull origin main
git checkout -b fitur/modul-1-auth-profile  # atau modul lainnya

# 3. Mulai coding
```

### Yang harus dilakukan setiap orang:

```bash
# 1. Pull perubahan terbaru
git pull origin main

# 2. Buat branch sendiri
git checkout -b fitur/modul-X-nama

# 3. Modifikasi entity -> extends BaseEntity
# 4. Buat Interface + Impl (@Override + Overload)
# 5. Buat DTO untuk setiap form
# 6. Commit & push
git add .
git commit -m "feat(modul-X): description with PBO evidence"
git push origin fitur/modul-X-nama

# 7. Pull Request -> Review -> Merge
```

---

## 📞 JIKA ADA KONFLIK

```
1. JANGAN push langsung ke main
2. JANGAN edit file modul orang lain tanpa izin
3. Untuk BaseEntity, Enum, Shared Config → Hubungi Orang 1
4. Untuk ownership → hubungi owner file tersebut
5. Kalau emergency → diskusi di grup / video call
```

---

*Dokumen ini adalah versi FINAL yang telah dikoreksi dari kesalahan akademis OOP.*
*Setiap anggota tim WAJIB membaca dan memahami perbedaan HAS-A vs Inheritance,*
*dan Switch-case vs Polymorphism sebelum mulai coding.*
