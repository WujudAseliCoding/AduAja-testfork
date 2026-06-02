# BAB 5 — ANALISIS KOMPONEN PELAPORAN WARGA

## 5.1. Deskripsi Singkat Komponen

Komponen pelaporan warga menangani seluruh siklus hidup laporan yang diajukan oleh warga melalui sistem AduAja. Alur dimulai saat warga mengisi formulir laporan, dilanjutkan dengan penyimpanan data ke database, pencatatan riwayat status, dan penelusuran laporan oleh warga.

Berdasarkan hasil penelusuran kode, terdapat **16 file** yang membentuk komponen ini:

| No | Nama File | Path Relatif | Fungsi |
|----|-----------|--------------|--------|
| 1 | `BaseEntity.java` | `model/BaseEntity.java` | Kelas abstrak induk yang menyediakan `createdAt` dan `updatedAt` otomatis bagi seluruh entity |
| 2 | `Report.java` | `model/Report.java` | Entity inti laporan, berisi field `reportId`, `ticketNumber`, `description`, `status`, `submittedAt`, serta relasi ke `User` (reporter), `ReportCategory`, `Region`, `SlaRecord`, `Disposition`, `FieldTask`, `ValidationDecision`, `ConfirmationRequest`, `DisputeRecord` |
| 3 | `ReportCategory.java` | `model/ReportCategory.java` | Entity kategori laporan dengan field `categoryId`, `categoryName`, `slaDurationHours`, `isActive` |
| 4 | `ReportRevision.java` | `model/ReportRevision.java` | Entity riwayat perubahan status, mencatat `oldStatus`, `newStatus`, `notes`, `changedBy`, `changedAt` |
| 5 | `CreateReportDTO.java` | `dto/CreateReportDTO.java` | Data Transfer Object yang memisahkan input form pembuatan laporan (`description`, `locationHint`, `latitude`, `longitude`, `photoBase64`, `categoryId`) dari entity |
| 6 | `ReportFilterDTO.java` | `dto/ReportFilterDTO.java` | Data Transfer Object untuk parameter filter laporan (`status`, `searchQuery`, `categoryId`, `startDate`, `endDate`) |
| 7 | `ReportRepository.java` | `repository/ReportRepository.java` | Interface Repository JPA yang menyediakan method query seperti `findByStatus`, `findByReporterUserIdOrderBySubmittedAtDesc`, `findByStatusAndDateRange`, `findByDateRange`, `countByStatus` |
| 8 | `ReportCategoryRepository.java` | `repository/ReportCategoryRepository.java` | Interface Repository untuk kategori dengan method `findByIsActiveTrue`, `findByCategoryName` |
| 9 | `ReportRevisionRepository.java` | `repository/ReportRevisionRepository.java` | Interface Repository untuk riwayat revisi dengan method `findByReportOrderByChangedAtDesc` |
| 10 | `ReportService.java` | `service/ReportService.java` | Interface service yang mendefinisikan kontrak untuk `createReport`, `updateStatus`, `findById`, `findByTicketNumber`, `getReportsByWarga`, `getReportsByStatus`, `getReportsByDateRange`, `getReportsByStatusAndDateRange` |
| 11 | `ReportServiceImpl.java` | `service/ReportServiceImpl.java` | Implementasi service yang mengandung logika bisnis untuk pembuatan laporan, pembaruan status, pembuatan revision record, dan filter data |
| 12 | `WebController.java` | `controller/WebController.java` | Controller MVC yang menangani route `/warga/create-report` (GET & POST), `/warga/report-history` (GET), dan `/warga/report-detail` (GET) |
| 13 | `ReportApiController.java` | `controller/ReportApiController.java` | REST API controller dengan endpoint `/api/reports` |
| 14 | `create-report.html` | `templates/warga/create-report.html` | Halaman Thymeleaf untuk form pembuatan laporan (kategori, deskripsi, kamera, peta, koordinat GPS) |
| 15 | `report-history.html` | `templates/warga/report-history.html` | Halaman Thymeleaf daftar laporan warga dengan filter status dan pencarian |
| 16 | `report-detail.html` | `templates/warga/report-detail.html` | Halaman Thymeleaf detail laporan dengan timeline revisi, tombol konfirmasi/sengketa, dan informasi SLA |

---

## 5.2. Desain Algoritma Internal

### 5.2.1. Fungsi Pembuatan Laporan Baru

**Method:** `ReportServiceImpl.createReport(CreateReportDTO dto, String wargaId)`

**Pseudocode (PDL):**

```
PROCEDURE createReport(dto: CreateReportDTO, wargaId: String)
RETURNS Report

    {1. Validasi keberadaan user pelapor}
    reporter ← userRepository.findById(wargaId)
    IF reporter IS null THEN
        THROW RuntimeException("User tidak ditemukan")
    ENDIF

    {2. Inisialisasi objek Report baru}
    report ← NEW Report()

    {3. Generate nomor tiket unik}
    report.ticketNumber ← "RPT-" + UUID.randomUUID().substring(0,8).toUpperCase()

    {4. Binding data dari DTO ke Entity (enkapsulasi)}
    report.reporter    ← reporter
    report.description ← dto.description
    report.locationHint ← dto.locationHint
    report.latitude    ← dto.latitude
    report.longitude   ← dto.longitude
    report.photoBase64 ← dto.photoBase64

    {5. Set timestamp dan status default}
    report.submittedAt ← LocalDateTime.now()
    report.status      ← Report.ReportStatus.MENUNGGU_VALIDASI

    {6. Set kategori jika ada}
    IF dto.categoryId IS NOT null AND dto.categoryId IS NOT blank THEN
        category ← categoryRepository.findById(dto.categoryId)
        IF category IS present THEN
            report.category ← category
        ENDIF
    ENDIF

    {7. Simpan ke database, BaseEntity.onCreate() otomatis set createdAt & updatedAt}
    savedReport ← reportRepository.save(report)

    {8. Kirim notifikasi ke semua admin pusat}
    admins ← userRepository.findByRole(User.Role.ADMIN_PUSAT)
    FOR EACH admin IN admins DO
        notificationService.createNotification(
            admin.userId,
            "Laporan Baru",
            "Laporan baru nomor " + savedReport.ticketNumber + " telah dibuat dan menunggu validasi.",
            "REPORT",
            savedReport.reportId
        )
    ENDFOR

    RETURN savedReport
END PROCEDURE
```

**Method yang akan memanggil prosedur ini:**

```
WebController.wargaCreateReportPost(CreateReportDTO dto, HttpSession session, RedirectAttributes redirectAttributes)
    → reportService.createReport(dto, userId)
```

**Detail parser request dari `report-detail.html`:**

| Nama Field Form | Tipe Data Java | Ditangkap Oleh |
|----------------|----------------|----------------|
| `dto.description` | `String` | `@ModelAttribute` dari `CreateReportDTO` |
| `dto.locationHint` | `String` | `@ModelAttribute` dari `CreateReportDTO` |
| `dto.latitude` | `BigDecimal` | `@ModelAttribute` dari `CreateReportDTO` |
| `dto.longitude` | `BigDecimal` | `@ModelAttribute` dari `CreateReportDTO` |
| `dto.photoBase64` | `String` | `@ModelAttribute` dari `CreateReportDTO` |
| `dto.categoryId` | `String` | `@ModelAttribute` dari `CreateReportDTO` |

---

### 5.2.2. Fungsi Pembaruan Status Laporan

**Method:** `ReportServiceImpl.updateStatus(String reportId, Report.ReportStatus newStatus, String notes, String changedBy)`  
**Overload:** `ReportServiceImpl.updateStatus(String reportId, Report.ReportStatus newStatus, String rejectionReason, String adminNotes, String changedBy)`

**Pseudocode (PDL) — Varian 1 (4 parameter):**

```
PROCEDURE updateStatus(reportId: String, newStatus: ReportStatus, notes: String, changedBy: String)
RETURNS Report

    {1. Cari laporan berdasarkan ID}
    report ← reportRepository.findById(reportId)
    IF report IS null THEN
        THROW RuntimeException("Report tidak ditemukan")
    ENDIF

    {2. Simpan status lama untuk audit trail}
    oldStatus ← report.status

    {3. Update catatan admin jika ada}
    IF notes IS NOT null THEN
        report.adminNotes ← notes
    ENDIF

    {4. Set status baru}
    report.status ← newStatus

    {5. Simpan perubahan}
    savedReport ← reportRepository.save(report)
    {   BaseEntity.onUpdate() otomatis mengupdate updatedAt   }

    {6. Catat revision record sebagai audit trail}
    CALL createRevision(savedReport, oldStatus, newStatus, notes, changedBy)

    RETURN savedReport
END PROCEDURE
```

**Pseudocode (PDL) — Varian 2 (5 parameter, overload):**

```
PROCEDURE updateStatus(reportId: String, newStatus: ReportStatus, rejectionReason: String, adminNotes: String, changedBy: String)
RETURNS Report

    {1. Cari laporan}
    report ← reportRepository.findById(reportId)
    IF report IS null THEN
        THROW RuntimeException("Report tidak ditemukan")
    ENDIF

    {2. Simpan status lama}
    oldStatus ← report.status

    {3. Perbarui status, alasan penolakan, dan catatan admin}
    report.status          ← newStatus
    report.rejectionReason ← rejectionReason
    report.adminNotes      ← adminNotes

    {4. Simpan}
    savedReport ← reportRepository.save(report)

    {5. Catat revision record dengan rejectionReason sebagai notes}
    CALL createRevision(savedReport, oldStatus, newStatus, rejectionReason, changedBy)

    RETURN savedReport
END PROCEDURE
```

**Prosedur pembantu — `createRevision` (internal private):**

```
PRIVATE PROCEDURE createRevision(report: Report, oldStatus: ReportStatus, newStatus: ReportStatus, notes: String, changedBy: String)

    {1. Buat objek revision record baru}
    revision ← NEW ReportRevision()

    {2. Isi data revision}
    revision.report     ← report
    revision.oldStatus  ← IF oldStatus IS NOT null THEN oldStatus.name() ELSE null
    revision.newStatus  ← newStatus.name()
    revision.notes      ← notes
    revision.changedBy  ← IF changedBy IS NOT null THEN changedBy ELSE "SYSTEM"
    revision.changedAt  ← LocalDateTime.now()

    {3. Simpan revision}
    revisionRepository.save(revision)
END PROCEDURE
```

**Entity yang terlibat dalam perubahan status:**
- `Report` — field `status` bertipe `Report.ReportStatus`, field `adminNotes`, field `rejectionReason`
- `ReportRevision` — mencatat `oldStatus`, `newStatus`, `notes`, `changedBy`, `changedAt`
- `BaseEntity` — `@PreUpdate` memicu perbaruan `updatedAt` secara otomatis

**Daftar nilai enum `Report.ReportStatus` (12 status):**

```
MENUNGGU_VALIDASI → PERLU_REVISI → DITOLAK → DIVALIDASI → DIDISPOSISI → DITUGASKAN → SEDANG_DIKERJAKAN → TERTUNDA → MENUNGGU_KONFIRMASI → SELESAI → SENGKETA → DITUTUP
```

---

### 5.2.3. Fungsi Pencarian/Filter Laporan

#### A. Filter berdasarkan status — `ReportServiceImpl.getReportsByStatus(Report.ReportStatus status)`

```
PROCEDURE getReportsByStatus(status: ReportStatus)
RETURNS List<Report>

    RETURN reportRepository.findByStatusOrderBySubmittedAtDesc(status)
END PROCEDURE

    ↓ Memanggil ↓

reportRepository.findByStatusOrderBySubmittedAtDesc(status: ReportStatus)
    → Query: SELECT r FROM Report r WHERE r.status = :status ORDER BY r.submittedAt DESC
    → Hasil: List<Report> yang diurutkan dari laporan terbaru
```

#### B. Filter berdasarkan warga — `ReportServiceImpl.getReportsByWarga(String wargaId)`

```
PROCEDURE getReportsByWarga(wargaId: String)
RETURNS List<Report>

    RETURN reportRepository.findByReporterUserIdOrderBySubmittedAtDesc(wargaId)
END PROCEDURE

    ↓ Memanggil ↓

reportRepository.findByReporterUserIdOrderBySubmittedAtDesc(wargaId: String)
    → Query: SELECT r FROM Report r WHERE r.reporter.userId = :wargaId ORDER BY r.submittedAt DESC
    → Hasil: List<Report> milik warga tertentu, terbaru dulu
```

#### C. Filter berdasarkan tanggal — `ReportServiceImpl.getReportsByDateRange(LocalDate start, LocalDate end)`

```
PROCEDURE getReportsByDateRange(start: LocalDate, end: LocalDate)
RETURNS List<Report>

    {1. Konversi LocalDate ke LocalDateTime (awal hari & akhir hari)}
    startDt ← start.atStartOfDay()             {2026-05-01T00:00:00}
    endDt   ← end.atTime(LocalTime.MAX)         {2026-05-31T23:59:59.999999999}

    {2. Query dengan JPQL}
    RETURN reportRepository.findByDateRange(startDt, endDt)
END PROCEDURE

    ↓ Memanggil ↓

reportRepository.findByDateRange(start: LocalDateTime, end: LocalDateTime)
    @Query("SELECT r FROM Report r WHERE r.submittedAt BETWEEN :start AND :end")
    → Hasil: List<Report> dengan submittedAt dalam rentang tanggal
```

#### D. Filter berdasarkan status + tanggal — `ReportServiceImpl.getReportsByStatusAndDateRange(Report.ReportStatus status, LocalDate start, LocalDate end)`

```
PROCEDURE getReportsByStatusAndDateRange(status: ReportStatus, start: LocalDate, end: LocalDate)
RETURNS List<Report>

    {1. Konversi tanggal}
    startDt ← start.atStartOfDay()
    endDt   ← end.atTime(LocalTime.MAX)

    {2. Query dengan JPQL 2 parameter}
    RETURN reportRepository.findByStatusAndDateRange(status, startDt, endDt)
END PROCEDURE

    ↓ Memanggil ↓

reportRepository.findByStatusAndDateRange(status: ReportStatus, start: LocalDateTime, end: LocalDateTime)
    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.submittedAt BETWEEN :start AND :end")
    → Hasil: List<Report> dengan status tertentu dalam rentang tanggal
```

#### E. Pencarian teks — `WebController.wargaReportHistory(...)` & `ReportRepository.findByDescriptionContainingIgnoreCaseOrLocationHintContainingIgnoreCase`

Pada `WebController.wargaReportHistory`, proses filtering dilakukan secara manual dalam layer controller:

```
PROCEDURE wargaReportHistory(filterStatus: String, searchQuery: String)
RETURNS ModelAndView

    {1. Ambil semua laporan milik warga yang sedang login}
    dbReports ← reportService.getReportsByWarga(userId)

    {2. Konversi List<Report> ke List<Map<String, Object>> untuk view}
    allReports ← konversiReportKeMap(dbReports)
    {   Konversi: Report.status → label warga via toWargaStatusLabel()   }
    {   Konversi: Report.ReportStatus → warna status badge (switch-case) }

    {3. Filter manual di controller}
    filtered ← EMPTY list
    FOR EACH r IN allReports DO
        matchStatus ← (filterStatus = "Semua") OR (r.status = filterStatus)
        matchQuery  ← (searchQuery IS blank) OR
                      (r.title.toLowerCase CONTAINS searchQuery.toLowerCase) OR
                      (r.id.toLowerCase CONTAINS searchQuery.toLowerCase) OR
                      (r.category.toLowerCase CONTAINS searchQuery.toLowerCase)
        IF matchStatus AND matchQuery THEN
            ADD r TO filtered
        ENDIF
    ENDFOR

    {4. Hitung jumlah per status}
    statusCounts ← MAP kosong
    statusCounts["Semua"] ← allReports.size
    FOR EACH status IN ["Menunggu","Diproses","Selesai","Ditolak","Sengketa"] DO
        statusCounts[status] ← COUNT of allReports WHERE r.status = status
    ENDFOR

    {5. Kirim ke view}
    model.reports      ← filtered
    model.totalCount   ← allReports.size
    model.filterStatus ← filterStatus
    model.searchQuery  ← searchQuery
    model.statusOptions ← ["Semua","Menunggu","Diproses","Selesai","Ditolak","Sengketa"]
    model.statusCounts ← statusCounts

    RETURN "warga/report-history"
END PROCEDURE
```

#### F. Method query yang tersedia di `ReportRepository` (untuk ekspansi pencarian):

| Signature Method | Query |
|-----------------|-------|
| `findByTicketNumber(String ticketNumber)` | `WHERE ticket_number = :ticketNumber` |
| `findByStatus(Report.ReportStatus status)` | `WHERE status = :status` |
| `findByReporterUserId(String reporterId)` | `WHERE reporter.userId = :reporterId` |
| `findByReporterUserIdOrderBySubmittedAtDesc(String reporterId)` | `WHERE reporter.userId = :reporterId ORDER BY submittedAt DESC` |
| `findByStatusOrderBySubmittedAtDesc(Report.ReportStatus status)` | `WHERE status = :status ORDER BY submittedAt DESC` |
| `findAllByOrderBySubmittedAtDesc()` | `SELECT * ORDER BY submittedAt DESC` |
| `findByDescriptionContainingIgnoreCase(String description)` | `WHERE LOWER(description) LIKE LOWER(:description)` |
| `findByDescriptionContainingIgnoreCaseOrLocationHintContainingIgnoreCase(String d, String l)` | `WHERE LOWER(description) LIKE LOWER(:d) OR LOWER(locationHint) LIKE LOWER(:l)` |
| `findByStatusAndDateRange(ReportStatus status, LocalDateTime start, LocalDateTime end)` | `WHERE status = :status AND submittedAt BETWEEN :start AND :end` (JPQL) |
| `findByDateRange(LocalDateTime start, LocalDateTime end)` | `WHERE submittedAt BETWEEN :start AND :end` (JPQL) |
| `countByStatus(Report.ReportStatus status)` | `SELECT COUNT(*) WHERE status = :status` |
| `countByReporterUserId(String reporterId)` | `SELECT COUNT(*) WHERE reporter.userId = :reporterId` |
| `findByParentReportIsNull()` | `WHERE parent_report_id IS NULL` |
| `findByRegionRegionId(String regionId)` | `WHERE region.regionId = :regionId` |
| `findByCategoryCategoryId(String categoryId)` | `WHERE category.categoryId = :categoryId` |
