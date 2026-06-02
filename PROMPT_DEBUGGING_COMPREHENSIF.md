# PROMPT DEBUGGING & REFACTORING KOMPREHENSIF — ADUAJA

## ROLE AND CONTEXT

Kamu adalah **Senior Java Spring Boot Architect & Code Reviewer**. Kamu menerima codebase proyek AduAja — Sistem Informasi Pengaduan dan Aspirasi Masyarakat yang dibangun dengan Spring Boot 4.x, Spring Data JPA, Thymeleaf, H2 Database, dan Lombok.

Tugasmu adalah melakukan **debugging menyeluruh** dan **refactoring kode** terhadap SELURUH codebase. Kamu harus memastikan:

1. Tidak ada **logika yang salah** (bug, data tidak konsisten, null pointer risk)
2. Tidak ada **routing yang error** (404, 500, redirect salah)
3. Semua data **up-to-date dengan database** (tidak pakai data dummy/hardcode)
4. Tidak ada **DRY violation** (kode berulang, extracted ke method/fungsi bersama)
5. Tidak ada **spaghetti code** (method kepanjangan, Controller terlalu gemuk)
6. **Struktur file rapi** (sesuai MVC, nama konsisten)
7. **4 Pilar PBO** sudah benar (Inheritance via extends BaseEntity, Polymorphism via @Override + Overloading, Abstraction via Interface/Impl, Encapsulation via DTO + private field)

---

## CARA KERJA

Kamu akan menerima seluruh file proyek sebagai konteks. Lakukan langkah berikut secara berurutan:

### LANGKAH 1: SCANNING KODE — Cari Semua Masalah

Scan seluruh file Java, HTML, dan konfigurasi. Catat setiap masalah yang ditemukan dalam kategori berikut:

#### Kategori A: Routing & Controller Issues

Periksa setiap `@Controller` dan `@RestController`:

- [ ] Apakah semua `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` memiliki path yang benar?
- [ ] Apakah ada **duplikasi path** di controller berbeda? (Contoh: `/warga/profile` di dua controller berbeda → Spring akan error)
- [ ] Apakah ada **infinite redirect**? (redirect ke halaman yang sama tanpa kondisi keluar)
- [ ] Apakah setiap method yang butuh session sudah melakukan `HttpSession session` dan `session.getAttribute("userId")` ?
- [ ] Apakah ada `@ModelAttribute` yang bind langsung ke Entity (bukan DTO)? — ini harus diubah.
- [ ] Apakah redirect setelah POST menggunakan `RedirectAttributes.addFlashAttribute()` untuk success/error message?

#### Kategori B: Service Layer Issues

- [ ] Apakah setiap service memiliki **Interface + Implementation** pattern?
- [ ] Apakah setiap method di interface memiliki **@Override** di implementation?
- [ ] Apakah ada **`default` method di interface** yang hanya ada untuk backward compatibility? — **HAPUS**, panggil langsung method aslinya di controller.
- [ ] Apakah ada method yang melakukan **query yang sama berulang-ulang**? — Extract ke private helper method.
- [ ] Apakah `@Transactional` sudah ditambahkan di method yang melakukan multiple write operations?
- [ ] Apakah exception handling sudah benar? (throw `RuntimeException` atau custom exception, bukan `return null`)

#### Kategori C: Entity & Database Issues

- [ ] Apakah **SEMUA entity sudah extends `BaseEntity`** ? Periksa satu per satu.
- [ ] Apakah ada entity yang masih memiliki field `createdAt` atau `updatedAt` sendiri (duplikasi dengan BaseEntity)? — Hapus, biarkan BaseEntity yang handle.
- [ ] Apakah semua kolom database menggunakan `@Column(name = "...")` yang sesuai?
- [ ] Apakah ada relasi `@OneToOne`, `@OneToMany`, `@ManyToOne` yang benar?
- [ ] Apakah `CascadeType` dan `FetchType` sudah sesuai? (LAZY untuk performa, ALL untuk composition)
- [ ] Apakah enum sudah didefinisikan di dalam entity atau di file terpisah?

#### Kategori D: DTO & Encapsulation Issues

- [ ] Apakah **setiap form input** menggunakan DTO (bukan Entity langsung)?
- [ ] Apakah method di Controller yang menerima data dari HTML menggunakan `@ModelAttribute` dengan DTO?
- [ ] Apakah semua field di DTO bersifat `private`?
- [ ] Apakah ada field di Entity yang seharusnya tidak memiliki public setter? (Contoh: `createdAt`, `passwordHash`)
- [ ] Apakah ada data yang bocor dari Entity ke View via `model.addAttribute("entity", entity)` seharusnya via DTO atau Map?

#### Kategori E: Thymeleaf View Issues

- [ ] Apakah setiap form HTML menggunakan `th:action="@{...}"` yang benar?
- [ ] Apakah setiap form menggunakan `th:object="${dto}"` dan `th:field="*{fieldName}"`?
- [ ] Apakah ada atribut HTML yang menggunakan `th:value` di form? — Ini SALAH untuk input form. Harus `th:field`.
- [ ] Apakah ada link atau redirect yang mengarah ke path yang tidak ada?
- [ ] Apakah layout Thymeleaf (`layout:decorate="~{layouts/master}"`) sudah benar?
- [ ] Apakah semua fragment (`layout:fragment="content"`) ada di layout?

#### Kategori F: DRY Violations

Cari dan eliminasi:

- [ ] Apakah ada blok kode yang sama di 2+ file berbeda? → Extract ke utility/helper.
- [ ] Apakah ada `DateTimeFormatter` yang dibuat ulang di setiap method? → Buat satu constants.
- [ ] Apakah ada pola "if (userId == null) return redirect" yang berulang? → Buat method `checkSession()`.
- [ ] Apakah ada konversi Report → Map yang berulang di setiap method di controller? → Extract ke helper.
- [ ] Apakah ada method yang melakukan hal yang sama dengan parameter sedikit berbeda? → Gunakan overloading.
- [ ] Apakah ada method Repository yang tidak terpakai? → Hapus.
- [ ] Apakah ada method Service yang tidak terpakai di interface? → Hapus.

#### Kategori G: Spaghetti Code Issues

- [ ] Apakah ada method Controller yang memiliki > 50 baris? → Refactor, pecah ke method private.
- [ ] Apakah ada method yang melakukan 3+ tanggung jawab berbeda? → Pisahkan ke method terpisah.
- [ ] Apakah ada logic bisnis di Controller yang seharusnya di Service? → Pindahkan.
- [ ] Apakah ada logic view di Service yang seharusnya di Controller? → Pindahkan.
- [ ] Apakah ada method yang menggunakan `@SuppressWarnings`? → Perbaiki, jangan di-suppress.

#### Kategori H: Session & Security Issues

- [ ] Apakah setiap halaman yang membutuhkan login sudah melakukan session check?
- [ ] Apakah ada informasi session yang bocor? (`userId`, `userName`, `userRole`)
- [ ] Apakah ada URL yang bisa diakses tanpa login yang seharusnya dilindungi?
- [ ] Apakah logout sudah meng-invalidate session?

#### Kategori I: 4 Pilar PBO Verification

- [ ] **INHERITANCE**: Semua entity extends `BaseEntity`. Tidak ada yang hanya menggunakan `@MappedSuperclass` tanpa extends.
- [ ] **POLYMORPHISM (Compile-time)**: Setiap Service Interface memiliki method overloading (satu nama method, parameter beda).
- [ ] **POLYMORPHISM (Run-time)**: Setiap ServiceImpl memiliki `@Override` pada setiap method dari interface.
- [ ] **ABSTRACTION**: Controller hanya meng-inject Interface Service, bukan Implementasi langsung.
- [ ] **ENCAPSULATION**: Semua field entity = private. Setiap form menggunakan DTO, bukan Entity.

#### Kategori J: HTML Template - Data Binding Issues

- [ ] Apakah `create-report.html` → form submit ke `/warga/create-report` → `CreateReportDTO` → `ReportServiceImpl.createReport(CreateReportDTO, String)`?
- [ ] Apakah `report-history.html` → tampil dari `getReportsByWarga(userId)`?
- [ ] Apakah `report-detail.html` → tampil dari `findById(id)`?
- [ ] Apakah `profile.html` → form submit ke `/warga/profile/edit`? Action method ada?
- [ ] Apakah data yang ditampilkan di HTML SELALU dari database, bukan hardcoded?

---

### LANGKAH 2: PERBAIKAN KODE

Setelah menemukan semua masalah, lakukan perbaikan dengan urutan prioritas:

#### PRIORITAS TINGGI (Harus selesai)

1. **Perbaiki semua routing yang error** — duplicate path, missing endpoint, redirect loop
2. **Perbaiki session checking** — semua halaman login-protected
3. **Hapus semua data dummy/hardcode** — ganti dengan data dari database via Service
4. **Entity BaseEntity** — pastikan semua entity extends BaseEntity
5. **DTO untuk semua form** — ganti binding Entity langsung dengan DTO
6. **Hapus `default` method backward compatibility** — ganti panggilan di controller dengan method asli

#### PRIORITAS SEDANG (Lakukan setelah prioritas tinggi)

7. **Extract helper method** — untuk DateTimeFormatter, Map konversi, session check
8. **Refactor method > 50 baris** — pecah jadi method private kecil
9. **Tambahkan @Transactional** — di method Service yang multiple write
10. **Exception handling yang konsisten** — gunakan pattern yang sama di semua controller

#### PRIORITAS RENDAH (Jika waktu masih ada)

11. **Hapus import yang tidak terpakai**
12. **Rapikan komentar** — standarisasi format
13. **Optimasi query** — tambahkan index jika perlu
14. **Standarisasi naming convention**

---

### LANGKAH 3: VERIFIKASI

Setelah semua perbaikan selesai, lakukan verifikasi:

1. **Compile success**: `./mvnw compile` tanpa error
2. **TEST**: Jalankan aplikasi, test semua flow:
   - Register → OTP → Login → Dashboard (data dari DB)
   - Buat laporan → Data tersimpan → Riwayat tampil
   - Edit profil → Data berubah → Tampil benar
   - Filter laporan → Hasil filter benar
3. **PBO check**: Pastikan 4 Pilar terlihat jelas di kode dengan komentar
4. **DRY check**: Tidak ada kode yang duplikat > 3 baris

---

## TEMPLATE OUTPUT UNTUK SETIAP PERBAIKAN

Untuk setiap masalah yang kamu temukan, tuliskan dengan format:

```
## [KATEGORI] Deskripsi Masalah

### File: path/ke/file.java :baris

### Sebelum (SALAH):
<KODE YANG SALAH>

### Sesudah (BENAR):
<KODE YANG SUDAH DIPERBAIKI>

### Alasan Perbaikan:
<Penjelasan kenapa ini salah dan bagaimana perbaikannya>
```

Contoh:

```
## [DRY] DateTimeFormatter dibuat ulang di setiap method Controller

### File: src/main/java/com/plr/aduaja/controller/WargaController.java :45
### File: src/main/java/com/plr/aduaja/controller/AdminPusatController.java :67
### File: src/main/java/com/plr/aduaja/controller/WargaAuthController.java :23

### Sebelum (SALAH):
return "redirect:/warga/profile";

### Sesudah (BENAR):
return "redirect:/warga/profile";
```

---

## FILE-FILE YANG WAJIB DIPERIKSA

### Controller (16 files)
Periksa SEMUA controller berikut:

```
src/main/java/com/plr/aduaja/controller/
├── MainController.java           ← Route / dan /index
├── WargaAuthController.java      ← Login/register warga
├── AdminAuthController.java      ← Login admin
├── WargaController.java          ← Dashboard, laporan, profil warga
├── AdminPusatController.java     ← Dashboard, validasi, disposisi admin pusat
├── AdminDinasController.java     ← Dashboard dinas, penugasan
├── PetugasController.java        ← Task, eksekusi lapangan
├── ReportApiController.java      ← REST API report
├── UserApiController.java        ← REST API user
├── NotificationApiController.java ← REST API notifikasi
├── TicketApiController.java      ← REST API tiket
├── DisposisiApiController.java   ← REST API disposisi
├── SengketaApiController.java    ← REST API sengketa
├── AttendanceApiController.java  ← REST API absensi
└── AdminApiController.java       ← REST API admin
```

### Service (15 files)
```
src/main/java/com/plr/aduaja/service/
├── UserService.java              ← Interface
├── UserServiceImpl.java          ← Implementasi
├── AuthService.java              ← Interface
├── AuthServiceImpl.java          ← Implementasi
├── OtpService.java               ← Interface
├── OtpServiceImpl.java           ← Implementasi
├── ReportService.java            ← Interface
├── ReportServiceImpl.java        ← Implementasi
├── NotificationService.java       ← Interface
├── NotificationServiceImpl.java   ← Implementasi
├── DispositionService.java        ← Interface
├── DispositionServiceImpl.java    ← Implementasi
├── FieldTaskService.java          ← Interface
├── FieldTaskServiceImpl.java      ← Implementasi
├── ... (sisanya 7 service lagi)
```

### Entity (23 files)
```
src/main/java/com/plr/aduaja/model/
├── BaseEntity.java               ← WAJIB, parent class
├── User.java                     ← WAJIB extends BaseEntity
├── UserProfile.java              ← WAJIB extends BaseEntity
├── Report.java                   ← WAJIB extends BaseEntity
├── ReportCategory.java           ← WAJIB extends BaseEntity
├── ReportRevision.java           ← WAJIB extends BaseEntity
├── Notification.java             ← WAJIB extends BaseEntity
├── LoginAttempt.java             ← WAJIB extends BaseEntity
├── OtpVerification.java          ← WAJIB extends BaseEntity
├── ActiveSession.java            ← WAJIB extends BaseEntity
├── FieldTask.java                ← WAJIB extends BaseEntity
├── Disposition.java              ← WAJIB extends BaseEntity
├── Agency.java                   ← WAJIB extends BaseEntity
├── TaskEvidence.java             ← WAJIB extends BaseEntity
├── TaskPostponement.java         ← WAJIB extends BaseEntity
├── OfficerAttendance.java        ← WAJIB extends BaseEntity
├── SlaRecord.java                ← WAJIB extends BaseEntity
├── SlaPauseLog.java              ← WAJIB extends BaseEntity
├── ConfirmationRequest.java      ← WAJIB extends BaseEntity
├── DisputeRecord.java            ← WAJIB extends BaseEntity
├── MergeRecord.java              ← WAJIB extends BaseEntity
├── ValidationDecision.java       ← WAJIB extends BaseEntity
├── AuditLog.java                 ← WAJIB extends BaseEntity
├── Region.java                   ← WAJIB extends BaseEntity
```

### DTO (5+ files)
```
src/main/java/com/plr/aduaja/dto/
├── LoginDTO.java
├── RegisterDTO.java
├── ProfileDTO.java
├── OtpVerifyDTO.java
├── ResetPasswordDTO.java
├── CreateReportDTO.java
├── ReportFilterDTO.java
├── ... (DTO lainnya)
```

### Repository (23 files)
```
src/main/java/com/plr/aduaja/repository/
├── Semua 23 Repository...
```

### View HTML (35+ files)
```
src/main/resources/templates/
├── warga/*.html          ← 8 files
├── admin/*.html          ← 11 files
├── admin/dinas/*.html    ← 6 files
├── petugas/*.html        ← 9 files
├── layouts/master.html   ← 1 file
├── index.html            ← 1 file
```

### Configuration (5+ files)
```
src/main/resources/application.properties
src/main/java/com/plr/aduaja/config/SecurityConfig.java
src/main/java/com/plr/aduaja/config/DataSeeder.java
```

---

## CONTOH MASALAH YANG SUDAH DIIDENTIFIKASI

Untuk menghemat waktu, berikut masalah yang sudah saya identifikasi di awal. Kamu bisa mulai dari sini:

### Masalah 1: Backward Compatibility Default Methods

**File:** `src/main/java/com/plr/aduaja/service/ReportService.java` :53-94
**File:** `src/main/java/com/plr/aduaja/service/NotificationService.java` :31-58

Ada banyak `default` method yang hanya wrapper untuk method lain. Ini adalah **DRY violation** — semua panggilan di Controller harus menggunakan method asli, bukan default wrapper.

Contoh:
```java
// SALAH — default method wrapper
default Report updateStatus(String id, Report.ReportStatus status) {
    return updateStatus(id, status, null, "SYSTEM");
}

default Optional<Report> getReportById(String id) {
    return findById(id);
}
```

**Perbaikan:** Hapus semua `default` method. Update semua panggilan di Controller untuk menggunakan method asli.

---

### Masalah 2: Entity extends BaseEntity — Belum Semua

**File:** `src/main/java/com/plr/aduaja/model/`

Hanya entity Modul 1 yang extends BaseEntity. Modul 2, 3, 4 entity belum extends BaseEntity.

Contoh (belum extends):
```java
public class Report {  // ← belum extends BaseEntity
public class FieldTask {  // ← belum extends BaseEntity
public class Disposition {  // ← belum extends BaseEntity
```

**Perbaikan:** Tambahkan `extends BaseEntity` ke SEMUA entity. Jika entity memiliki field `updatedAt` sendiri, hapus field tersebut (karena sudah ada di BaseEntity).

---

### Masalah 3: Potensi Infinite Redirect

**File:** `src/main/java/com/plr/aduaja/controller/WargaController.java`

Periksa apakah ada session redirect yang berpotensi infinite loop:
```java
if (userId == null) return "redirect:/warga/login";
```

Pastikan `wargaLoginPage()` di `WargaAuthController.java` TIDAK melakukan redirect lain yang bisa menyebabkan loop.

---

### Masalah 4: Monolitik Controller yang Terlalu Gemuk

**File:** (cek semua controller)

Jika ada controller dengan > 300 baris, refactor dengan memindahkan:
- Logic bisnis → Service layer
- Helper method → Utility class atau private method
- Konversi Entity → Map → Helper method terpisah

---

### Masalah 5: Format Data di HTML Template

**File:** `src/main/resources/templates/warga/*.html`

Pastikan:
- Input form menggunakan `th:field`, bukan `th:value`
- Form menggunakan `th:object` dengan DTO
- Action POST menggunakan `@{}` syntax

---

### Masalah 6: Duplikasi Session Check

**File:** Beberapa controller

Pola `String userId = (String) session.getAttribute("userId");` dan `if (userId == null) return "redirect:/warga/login";` berulang di hampir semua method. Extract ke method helper atau interceptor.

---

## OUTPUT FORMAT

Setelah selesai, buat file laporan:

```
LAPORAN_DEBUGGING_ADUAJA.md
```

Yang berisi:
1. **Ringkasan Eksekutif**: Berapa banyak masalah ditemukan, per kategori
2. **Daftar Lengkap Perbaikan**: Format [KATEGORI] Deskripsi → File:Baris → Sebelum → Sesudah → Alasan
3. **File yang diubah**: List semua file yang dimodifikasi
4. **Verifikasi**: Hasil compile, hasil test manual

---

## MULAI

Mulai dengan membaca SELURUH file proyek. Catat masalah-masalah yang ditemukan. Lakukan perbaikan. Verifikasi hasil compile dan test.

**JANGAN** membuat asumsi. **JANGAN** menghapus fungsionalitas. **JANGAN** mengubah arsitektur utama (struktur MVC tetap).

**BOLEH**:
- Memecah method panjang
- Mengekstrak kode duplikat
- Menambah helper
- Memperbaiki nama variable
- Menambah komentar PBO
- Memperbaiki routing
- Menambah validasi

**TIDAK BOLEH**:
- Mengganti framework
- Menghapus entity/table
- Mengganti tipe data kolom
- Menghapus fitur yang sudah ada
- Mengubah struktur folder utama
