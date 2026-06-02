# Prompt untuk AI Agent — Memperbaiki SCN-08, SCN-09, SCN-10

Berikut adalah panduan lengkap dan detail untuk memperbaiki tiga skenario pengujian yang gagal. Bacalah seluruh dokumen ini sebelum memulai implementasi.

---

## Daftar Isi

1. [Informasi Proyek](#1-informasi-proyek)
2. [SCN-08: Konfirmasi Timeout — Laporan Ditutup Otomatis](#2-scn-08-konfirmasi-timeout--laporan-ditutup-otomatis)
3. [SCN-09: SLA Terlewat (Laporan Terlambat)](#3-scn-09-sla-terlewat-laporan-terlambat)
4. [SCN-10: Petugas Ajukan Penundaan → Resume → Selesai](#4-scn-10-petugas-ajukan-penundaan--resume--selesai)
5. [Cara Kerja Keseluruhan](#5-cara-kerja-keseluruhan)

---

## 1. Informasi Proyek

### Tech Stack
- **Backend:** Java 21, Spring Boot 3.x, Spring Security 6/7
- **Frontend:** Thymeleaf 3.1, Alpine.js, Tailwind CSS
- **Database:** PostgreSQL via Hibernate/JPA
- **Build:** Maven

### Struktur Direktori Utama
```
src/main/java/com/plr/aduaja/
  ├── controller/
  │   ├── AdminDinasController.java     # Semua endpoint admin dinas
  │   ├── PetugasController.java        # Semua endpoint petugas
  │   ├── MainController.java           # Landing page
  │   └── ControllerHelper.java         # Helper session & utilities
  ├── service/
  │   ├── ConfirmationServiceImpl.java  # SCN-08: Proses timeout konfirmasi
  │   ├── SlaRecordServiceImpl.java     # SCN-09: SLA pause/resume/overdue
  │   ├── SlaMonitoringServiceImpl.java # SCN-08/09: Scheduler tiap jam
  │   ├── FieldTaskServiceImpl.java     # SCN-10: Postpone/requestPostpone tugas
  │   ├── NotificationService.java      # Notifikasi ke warga
  │   └── NotificationServiceImpl.java
  ├── repository/
  │   ├── ConfirmationRequestRepository.java
  │   ├── SlaRecordRepository.java
  │   ├── TaskPostponementRepository.java
  │   └── FieldTaskRepository.java
  └── model/
      ├── ConfirmationRequest.java      # ResponseType: TERIMA, TOLAK, TIMEOUT
      ├── SlaRecord.java                # SlaStatus: BERJALAN, TERLAMBAT, TERTUNDA, SELESAI
      ├── TaskPostponement.java         # ApprovalStatus: MENUNGGU, DISETUJUI, DITOLAK
      ├── FieldTask.java                # TaskStatus: BARU, SEDANG_DIKERJAKAN, TERTUNDA, SELESAI
      └── Report.java                   # ReportStatus: ... SELESAI_OTOMATIS, SELESAI

src/main/resources/templates/
  ├── admin/dinas/
  │   ├── dinas-dashboard.html          # Dashboard admin dinas
  │   ├── dinas-queue.html              # Antrean laporan
  │   ├── progress-update.html          # Progress tugas + pause/resume SLA
  │   └── sengketa-dinas.html           # Panel resolusi sengketa
  ├── petugas/
  │   ├── task-detail.html              # Detail tugas + postpone modal
  │   └── tasks.html                    # Daftar tugas petugas
  ├── admin/
  │   └── sla.html                      # Monitoring SLA (admin pusat)
  └── warga/
      └── report-detail.html            # Detail laporan dari sisi warga

testing-scenarios/
  ├── SCN-08_konfirmasi-timeout.md      # Test scenario document
  ├── SCN-09_sla-terlambat.md           # Test scenario document
  └── SCN-10_tugas-tertunda.md          # Test scenario document
```

---

## 2. SCN-08: Konfirmasi Timeout — Laporan Ditutup Otomatis

### Tujuan
Ketika warga tidak merespons konfirmasi dalam 3x24 jam, scheduler harus otomatis:
1. Set `ConfirmationRequest.response = TIMEOUT`, `isLocked = true`
2. Ubah status laporan menjadi `SELESAI_OTOMATIS`
3. Kirim notifikasi "Laporan Ditutup Otomatis" ke warga

### Status Sekarang
| Area | Status | Detail |
|------|--------|--------|
| `ConfirmationServiceImpl.processTimeouts()` | ✅ SUDAH | Logic sudah benar, sudah set TIMEOUT, ubah status laporan, kirim notifikasi |
| `SlaMonitoringServiceImpl.checkSlaViolations()` | ✅ SUDAH | Sudah panggil `confirmationService.processTimeouts()` tiap jam |
| Reporter mapping di `processTimeouts()` | ⚠️ PERLU CEK | Baris 125: `report.getReporter()` mungkin null |

### Yang Perlu Dibetulkan

#### a. Buat endpoint manual untuk memicu processTimeouts (kebutuhan testing)
Buat endpoint GET sementara di `AdminDinasController` atau controller terpisah:
```
GET /admin/dinas/trigger-timeout
```
Yang memanggil `confirmationService.processTimeouts()` dan redirect dengan flash message.

#### b. Verifikasi notifikasi sampai ke warga
Cek di `NotificationServiceImpl.java` apakah `createNotification()` dengan tipe notifikasi `SELESAI_OTOMATIS` (dari enum `Notification.NotificationType`) berfungsi dengan benar. Jika belum ada tipe `SELESAI_OTOMATIS` di enum, tambahkan.

#### c. Pastikan warga tidak bisa ajukan sengketa setelah DITUTUP (SELESAI_OTOMATIS)
Cari di `WargaController.java` method `dispute-report` (POST `/warga/dispute-report`). Di method tersebut harus ada pengecekan:
```java
if (report.getStatus() == ReportStatus.SELESAI_OTOMATIS || report.getStatus() == ReportStatus.SELESAI) {
    // Tolak: laporan sudah final
}
```

### File yang relevan
| File | Baris Penting | Keterangan |
|------|---------------|------------|
| `service/ConfirmationServiceImpl.java` | 107-140 | `processTimeouts()` — sudah benar |
| `service/SlaMonitoringServiceImpl.java` | 35-54 | `checkSlaViolations()` — scheduler panggil processTimeouts |
| `controller/AdminDinasController.java` | - | Tambah endpoint `/admin/dinas/trigger-timeout` |
| `model/ConfirmationRequest.java` | 38-40 | Enum `ResponseType { TERIMA, TOLAK, TIMEOUT }` |
| `model/Report.java` | 113 | `SELESAI_OTOMATIS` sudah ada |
| `model/Notification.java` | 53 | Enum `NotificationType` — cek apakah `SELESAI_OTOMATIS` ada |
| `controller/WargaController.java` | Cari `/warga/dispute-report` |Harus blokir jika status `SELESAI_OTOMATIS` |

---

## 3. SCN-09: SLA Terlewat (Laporan Terlambat)

### Tujuan
1. SLA otomatis berubah ke `TERLAMBAT` setelah deadline lewat
2. Admin dinas bisa pause dan resume SLA
3. Saat resume, deadline diperpanjang sejumlah durasi pause
4. Admin dinas melihat indikasi visual laporan yang SLA-nya terlambat

### Status Sekarang
| Area | Status | Detail |
|------|--------|--------|
| SLA overdue detection | ✅ SUDAH | `SlaRecordServiceImpl.checkAndUpdateOverdueSla()` + scheduler |
| `pauseSla()` | ✅ SUDAH | `SlaRecordServiceImpl.pauseSla()` — buat SlaPauseLog + status TERTUNDA |
| `resumeSla()` | ✅ SUDAH | `SlaRecordServiceImpl.resumeSla()` — hitung durasi pause, perpanjang deadline |
| POST `/admin/dinas/pause-sla` | ✅ SUDAH | `AdminDinasController.adminDinasPauseSla()` |
| POST `/admin/dinas/resume-sla` | ✅ SUDAH | `AdminDinasController.adminDinasResumeSla()` |
| Indikasi SLA terlambat di admin dinas | ❌ BELUM | Tidak ada badge/highlight di dashboard dan queue |
| SLA late counter di queue | ❌ SALAH | `terlambatCount` dihitung dengan filter yang salah (baris 210) |
| Progress page auto-select | ❌ PERLU | Sama seperti sengketa, harus pilih via klik baris |

### Yang Perlu Dibetulkan

#### a. Perbaiki SLA late counter di queue page
Di `AdminDinasController.java` baris 210:
```java
long terlambatCount = laporanDinas.stream().filter(r -> "Terlambat SLA".equals(r.get("status"))).count();
```
Ini SALAH karena `r.get("status")` berisi `"Belum Ditindaklanjuti"`, bukan `"Terlambat SLA"`. Harusnya iterasi ulang atau cek SLA record dari report.

**Cara memperbaiki:** Di loop pembuatan `laporanDinas` (baris 194-207), untuk setiap disposisi, cari SLA record via `slaRecordRepository.findByReportReportId()` dan jika SLA status-nya `TERLAMBAT`, set properti `"status"` menjadi `"Terlambat SLA"`. Alternatif: buat properti terpisah `"slaLate"` boolean.

#### b. Tambah badge/indikasi SLA terlambat di Queue page
Di `dinas-queue.html` — untuk setiap baris, tambahkan badge merah jika SLA terlambat. Template sudah punya banner peringatan di baris 58-69 (`th:if="${terlambatCount > 0}"`), tetapi perlu juga badge per-item.

#### c. Tambah indikasi SLA terlambat di Admin Dinas Dashboard
Di `AdminDinasController.adminDinasDashboard()` (baris 69-131), untuk setiap `pendingAssignments`, SLA status masih di-hardcode `"-"` (baris 121). Ambil SLA record dari report ID untuk menampilkan status SLA yang benar.

Template `dinas-dashboard.html` baris 151-161 sudah punya badge merah untuk "Terlambat SLA", tetapi data SLA dari controller perlu diperbaiki.

#### d. Perbaiki judul/deskripsi dobel di task detail petugas (Bug dari Catatan SCN-09)
Di `PetugasController.java`, cari method yang handle `/petugas/task-detail`. Pastikan data `judul` dan `deskripsi` laporan tidak muncul dua kali. Kemungkinan ada duplikasi data saat query atau saat mapping field.

#### e. Perbaiki auto-select di progress page (sama seperti sengketa)
Di `adminDinasProgress()` baris 382-395, ada auto-select default ke item pertama. Hapus fallback `else if` dan `if (selected == null ...)` agar hanya pilih jika ada parameter `id`.

### File yang relevan
| File | Baris | Keterangan |
|------|-------|------------|
| `controller/AdminDinasController.java` | 172-231 | Queue — perbaiki `terlambatCount` di baris 210 |
| `controller/AdminDinasController.java` | 69-131 | Dashboard — ambil SLA status real di baris 121 |
| `controller/AdminDinasController.java` | 329-398 | Progress — hapus auto-select fallback |
| `controller/AdminDinasController.java` | 841-892 | Pause/Resume endpoint — sudah OK |
| `service/SlaRecordServiceImpl.java` | 87-103 | `pauseSla()` — sudah OK |
| `service/SlaRecordServiceImpl.java` | 106-129 | `resumeSla()` — sudah OK |
| `service/SlaRecordServiceImpl.java` | 141-152 | `checkAndUpdateOverdueSla()` — sudah OK |
| `templates/admin/dinas/dinas-queue.html` | 58-69, 149-154 | Banner + sisaWaktu. Tambah badge per-item |
| `templates/admin/dinas/dinas-dashboard.html` | 151-161 | Badge SLA per-item (data dari controller perlu diperbaiki) |
| `templates/admin/dinas/progress-update.html` | 131-136, 231-274 | SLA badge + pause/resume UI — sudah OK |
| `templates/petugas/task-detail.html` | - | Cek duplikasi judul/deskripsi |

---

## 4. SCN-10: Tugas Tertunda (Petugas Ajukan Penundaan)

### Tujuan
1. Petugas ajukan penundaan → `TaskPostponement` dibuat dengan status `MENUNGGU`
2. Status tugas tetap `SEDANG_DIKERJAKAN` (bukan langsung `TERTUNDA`)
3. Admin dinas melihat dan **menyetujui/menolak** penundaan
4. Admin pause SLA saat menyetujui penundaan
5. Setelah kendala selesai, admin resume SLA → deadline diperpanjang
6. Petugas lanjutkan tugas → selesai

### Status Sekarang
| Area | Status | Detail |
|------|--------|--------|
| `FieldTaskServiceImpl.requestPostpone()` | ✅ SUDAH | Membuat `TaskPostponement` dengan `MENUNGGU`, tidak ubah status tugas |
| `FieldTaskServiceImpl.postponeTask()` | ✅ SUDAH | Langsung set TERTUNDA + DISETUJUI (untuk admin) |
| POST postpone di PetugasController | ✅ SUDAH | `petugasTaskAction()` case "postpone" — panggil `requestPostpone()` |
| Flash message setelah postpone | ✅ SUDAH | "Pengajuan penundaan berhasil dikirim..." |
| Approve postponement endpoint | ❌ BELUM | Tidak ada endpoint untuk admin menyetujui/menolak penundaan |
| Admin UI untuk daftar penundaan | ❌ BELUM | Tidak ada panel/notifikasi di admin dinas |
| Task status history bug | ❌ ADA | Riwayat status tugas langsung berubah ke TERTUNDA padahal masih MENUNGGU |

### Yang Perlu Dibetulkan

#### a. Buat endpoint approve/reject postponement di AdminDinasController
```
POST /admin/dinas/approve-postponement
  @RequestParam("postponementId") String postponementId,
  @RequestParam(value = "action", defaultValue = "approve") String action  // "approve" atau "reject"
```

Logika:
- **approve:**
  1. Set `TaskPostponement.approvalStatus = DISETUJUI`, `approvedBy = adminId`
  2. Ubah `FieldTask.taskStatus = TERTUNDA` via `fieldTaskService.postponeTask()`
  3. Pause SLA via `slaRecordService.pauseSla()`
  4. Flash message "Penundaan disetujui"
- **reject:**
  1. Set `TaskPostponement.approvalStatus = DITOLAK`, `approvedBy = adminId`
  2. Task status tetap `SEDANG_DIKERJAKAN` (tidak berubah)
  3. Flash message "Penundaan ditolak"

#### b. Tambah method di FieldTaskService untuk approve postponement
```java
FieldTask approvePostponement(String postponementId, String adminId, boolean approved);
```
Atau langsung handle di controller dengan repository.

#### c. Tampilkan daftar penundaan MENUNGGU di admin dinas progress page
Di `adminDinasProgress()` (baris 329-398), tambahkan data `pendingPostponements` ke model dengan query dari `taskPostponementRepository.findByApprovalStatus(MENUNGGU)`.

Di `progress-update.html`, tambahkan section/kartu yang menampilkan daftar penundaan menunggu lengkap dengan tombol Setujui/Tolak.

#### d. Perbaiki task status history yang langsung berubah ke TERTUNDA
Cari di code yang mencatat riwayat status tugas (mungkin di `FieldTaskServiceImpl` atau via audit trail). Pastikan saat `requestPostpone()` dipanggil (petugas ajukan penundaan), riwayat status tugas TIDAK mencatat "TERTUNDA". Hanya ketika admin benar-benar approve (via `postponeTask()` atau `approvePostponement()`) barulah status berubah.

#### e. Hubungkan approve postponement dengan pause SLA
Di endpoint approve (poin a), setelah approve, otomatis pause SLA. Gunakan `slaRecordService.pauseSla()` dengan `task.getSlaRecord().getSlaId()`.

#### f. Resume SLA di endpoint resume yang sudah ada
Endpoint `POST /admin/dinas/resume-sla` (baris 869-892) sudah ada dan berfungsi. Pastikan setelah resume, task status dikembalikan ke `SEDANG_DIKERJAKAN` (sudah dilakukan di baris 884).

### File yang relevan
| File | Baris | Keterangan |
|------|-------|------------|
| `controller/AdminDinasController.java` | - | **Tambah** `POST /admin/dinas/approve-postponement` |
| `controller/AdminDinasController.java` | 329-398 | Progress page — **tambah** `pendingPostponements` ke model |
| `controller/AdminDinasController.java` | 841-867 | `pauseSla()` — perlu dipanggil dari approve postponement |
| `controller/PetugasController.java` | 177-189 | `postpone` case — sudah OK |
| `service/FieldTaskServiceImpl.java` | 248-264 | `postponeTask()` — langsung TERTUNDA + DISETUJUI (untuk admin) |
| `service/FieldTaskServiceImpl.java` | 266-283 | `requestPostpone()` — MENUNGGU, tugas tidak berubah (sudah OK) |
| `model/TaskPostponement.java` | 40-42 | Enum `ApprovalStatus { MENUNGGU, DISETUJUI, DITOLAK }` |
| `repository/TaskPostponementRepository.java` | 17 | `findByApprovalStatus(ApprovalStatus)` |
| `templates/admin/dinas/progress-update.html` | - | **Tambah** section daftar penundaan menunggu |
| `templates/petugas/task-detail.html` | 250-274 | Postpone button + pending status — sudah OK |

---

## 5. Cara Kerja Keseluruhan

### Urutan Prioritas Pengerjaan

1. **SCN-09 (perbaikan kecil dulu):**
   - Perbaiki SLA late counter di queue (baris 210)
   - Tambah badge SLA terlambat di dashboard dan queue
   - Perbaiki judul/deskripsi dobel di task detail petugas

2. **SCN-10 (paling kompleks):**
   - Tambah endpoint `POST /admin/dinas/approve-postponement`
   - Tambah daftar penundaan di progress page
   - Hubungkan approve → pause SLA
   - Perbaiki riwayat status tugas

3. **SCN-08 (penyempurnaan):**
   - Tambah endpoint `/admin/dinas/trigger-timeout` untuk testing
   - Verifikasi notifikasi SELESAI_OTOMATIS
   - Blokir dispute-report jika status SELESAI_OTOMATIS

### Catatan Penting
- Jangan hapus kode yang sudah ada — hanya tambah atau perbaiki
- Ikuti pattern yang sudah ada (gunakan `@Autowired`, `@Transactional`, method pattern yang sama)
- Untuk view, ikuti style template yang sudah ada (Tailwind classes, Alpine.js)
- Flash message gunakan `redirectAttributes.addFlashAttribute("success", "...")` atau `"error", "..."`
- Semua endpoint redirect setelah POST (PRG pattern)
- Test dengan cara: restart aplikasi, ikuti langkah-langkah di file `testing-scenarios/SCN-*.md`
