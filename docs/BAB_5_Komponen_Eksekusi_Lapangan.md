# BAB 5 — Komponen Eksekusi Lapangan dan Sengketa

## Daftar File yang Teridentifikasi

### Model (Entitas JPA)
| No | File | Paket | Keterangan |
|----|------|-------|------------|
| 1 | `FieldTask.java` | `com.plr.aduaja.model` | Entitas tugas lapangan dengan enum `TaskStatus { BARU, SEDANG_DIKERJAKAN, TERTUNDA, SELESAI, DITUGASKAN_ULANG }` |
| 2 | `DisputeRecord.java` | `com.plr.aduaja.model` | Entitas sengketa dengan enum `ResolutionType { TUGASKAN_KEMBALI, TUTUP_LAPORAN }` |
| 3 | `ConfirmationRequest.java` | `com.plr.aduaja.model` | Entitas permintaan konfirmasi warga dengan enum `ResponseType { TERIMA, TOLAK, TIMEOUT }` |
| 4 | `TaskEvidence.java` | `com.plr.aduaja.model` | Entitas bukti foto petugas dengan enum `EvidenceType { SEBELUM, SESUDAH }` |
| 5 | `TaskPostponement.java` | `com.plr.aduaja.model` | Entitas penundaan tugas dengan enum `ApprovalStatus { MENUNGGU, DISETUJUI, DITOLAK }` |
| 6 | `Report.java` | `com.plr.aduaja.model` | Entitas laporan — status relevan: `DITUGASKAN, SEDANG_DIKERJAKAN, TERTUNDA, MENUNGGU_KONFIRMASI, SELESAI, SENGKETA, DITUTUP` |

### Repository
| No | File | Method Penting |
|----|------|---------------|
| 1 | `FieldTaskRepository.java` | `findByOfficerUserId`, `findByOfficerUserIdAndTaskStatus`, `findByReportReportId`, `countByTaskStatus`, `findTopByReportReportIdOrderByStartedAtDesc` |
| 2 | `DisputeRecordRepository.java` | `findByReportReportId`, `findByFiledByUserId`, `findByResolutionIsNull`, `countByResolutionIsNull` |
| 3 | `ConfirmationRequestRepository.java` | `findByReportReportId`, `findByDeadlineAtBeforeAndResponseIsNull`, `findByIsLockedFalse` |
| 4 | `ReportRepository.java` | Digunakan di semua service untuk memperbarui status Report |

### Service
| No | File | Method Utama |
|----|------|-------------|
| 1 | `FieldTaskService.java` | `getAllTasks()`, `getTaskById(String)`, `getTasksByOfficer(String)`, `getTasksByOfficerAndStatus(String, TaskStatus)`, `getTasksByReport(String)`, `createTask(String, String, String)`, `startTask(String, BigDecimal, BigDecimal)`, `completeTask(String)`, `countByStatus(TaskStatus)` |
| 2 | `DisputeService.java` | `getAllDisputes()`, `getDisputeByReportId(String)`, `getPendingDisputes()`, `createDispute(String, String, String, String)`, `resolveDispute(String, String, ResolutionType, String)` |
| 3 | `ConfirmationService.java` | `getByReportId(String)`, `createConfirmation(String, String, int)`, `respond(String, ResponseType)`, `processTimeouts()` |

### Controller — REST API
| No | File | Endpoint |
|----|------|----------|
| 1 | `TicketApiController.java` | `GET /api/tickets`, `GET /api/tickets/{id}`, `GET /api/tickets/status/{status}`, `GET /api/tickets/petugas/{petugasId}`, `POST /api/tickets`, `POST /api/tickets/{id}/start`, `POST /api/tickets/{id}/complete`, `GET /api/tickets/count` |
| 2 | `SengketaApiController.java` | `GET /api/sengketa`, `GET /api/sengketa/{id}`, `GET /api/sengketa/report/{reportId}`, `GET /api/sengketa/pending`, `POST /api/sengketa`, `POST /api/sengketa/{id}/resolve` |

### Controller — MVC (Thymeleaf)
| No | File | View Path |
|----|------|-----------|
| 1 | `WebController.java` — `adminDinasPenugasan` | `GET/POST /admin/dinas/penugasan` — buat FieldTask |
| 2 | `WebController.java` — `adminDinasProgress` | `GET/POST /admin/dinas/progress` — start task |
| 3 | `WebController.java` — `adminDinasClose` | `GET/POST /admin/dinas/close` — complete task |
| 4 | `WebController.java` — `petugasTaskAction` | `POST /petugas/task-action` — start/complete task dari sisi petugas |
| 5 | `WebController.java` — `petugasTaskExecutionPost` | `POST /petugas/task-execution` — complete task dari halaman eksekusi |
| 6 | `WebController.java` — `adminSengketaPanel` | `GET/POST /admin/sengketa` — panel resolusi sengketa admin |
| 7 | `WebController.java` — `adminDinasSengketa` | `GET/POST /admin/dinas/sengketa` — panel resolusi sengketa admin dinas |

---

## Deskripsi Singkat Komponen

Komponen Eksekusi Lapangan dan Sengketa menangani seluruh siklus hidup penanganan laporan di lapangan — mulai dari penugasan petugas (`FieldTaskService`), pelaksanaan tugas (start/complete), konfirmasi hasil pekerjaan ke warga (`ConfirmationService`), hingga penanganan sengketa jika warga menolak hasil pekerjaan (`DisputeService`).

Alur utamanya:
1. Admin dinas menugaskan petugas ke suatu laporan → `FieldTask` dibuat dengan `TaskStatus.BARU`
2. Petugas memulai pekerjaan di lokasi → `startTask()` → status menjadi `SEDANG_DIKERJAKAN`
3. Petugas menyelesaikan pekerjaan dan mengunggah bukti → `completeTask()` → status menjadi `SELESAI`
4. Sistem mengirim `ConfirmationRequest` ke warga dengan batas waktu (deadline)
5. Warga merespon: **TERIMA** → laporan ditutup (`DITUTUP`); **TOLAK** → laporan masuk status `SENGKETA`
6. Jika warga tidak merespon hingga deadline → `processTimeouts()` → otomatis `TIMEOUT` dan laporan ditutup
7. Admin meninjau sengketa → `resolveDispute()` dengan opsi:
   - `TUGASKAN_KEMBALI` → laporan kembali ke status `DITUGASKAN` (ditugaskan ulang)
   - `TUTUP_LAPORAN` → laporan ditutup permanen (`DITUTUP`)
8. Jika petugas perlu menunda → `TaskPostponement` dengan approval status `MENUNGGU`, `DISETUJUI`, atau `DITOLAK`
9. Petugas mengunggah bukti foto (`TaskEvidence`) dengan tipe `SEBELUM` dan `SESUDAH`

---

## Desain Algoritma Internal (Pseudocode / PDL)

### 1. Algoritma Penugasan Petugas — `FieldTaskService.createTask(reportId, officerId, assignedById)`

```
FUNCTION createTask(reportId: String, officerId: String, assignedById: String) RETURNS FieldTask
    // 1. Validasi keberadaan entitas relasi
    report ← reportRepository.findById(reportId)
    IF report IS null THEN
        THROW RuntimeException("Report not found")
    
    officer ← userRepository.findById(officerId)
    IF officer IS null THEN
        THROW RuntimeException("Officer not found")
    
    assignedBy ← userRepository.findById(assignedById)
    IF assignedBy IS null THEN
        THROW RuntimeException("Assigner not found")
    
    // 2. Buat objek FieldTask baru
    task ← new FieldTask()
    task.setReport(report)
    task.setOfficer(officer)
    task.setAssignedBy(assignedBy)
    task.setTaskStatus(TaskStatus.BARU)
    
    // 3. Cari SLA Record yang terkait dengan report (jika ada)
    sla ← slaRecordRepository.findByReportReportId(reportId)
    task.setSlaRecord(sla)  // bisa null
    
    // 4. Simpan ke database dan return
    RETURN fieldTaskRepository.save(task)
END FUNCTION
```

### 2. Algoritma Mulai Pekerjaan — `FieldTaskService.startTask(taskId, latitude, longitude)`

```
FUNCTION startTask(taskId: String, latitude: BigDecimal, longitude: BigDecimal) RETURNS FieldTask
    // 1. Cari task berdasarkan ID
    task ← fieldTaskRepository.findById(taskId)
    IF task IS null THEN
        THROW RuntimeException("Task not found")
    
    // 2. Ubah status menjadi SEDANG_DIKERJAKAN
    task.setTaskStatus(TaskStatus.SEDANG_DIKERJAKAN)
    
    // 3. Catat timestamp mulai
    task.setStartedAt(LocalDateTime.now())
    
    // 4. Simpan koordinat GPS petugas saat memulai
    task.setOfficerLatitude(latitude)
    task.setOfficerLongitude(longitude)
    
    // 5. Simpan perubahan dan return
    RETURN fieldTaskRepository.save(task)
END FUNCTION
```

### 3. Algoritma Selesaikan Pekerjaan — `FieldTaskService.completeTask(taskId)`

```
FUNCTION completeTask(taskId: String) RETURNS FieldTask
    // 1. Cari task berdasarkan ID
    task ← fieldTaskRepository.findById(taskId)
    IF task IS null THEN
        THROW RuntimeException("Task not found")
    
    // 2. Ubah status menjadi SELESAI
    task.setTaskStatus(TaskStatus.SELESAI)
    
    // 3. Catat timestamp selesai
    task.setCompletedAt(LocalDateTime.now())
    
    // 4. Simpan dan return
    RETURN fieldTaskRepository.save(task)
END FUNCTION
```

### 4. Algoritma Buat Permintaan Konfirmasi — `ConfirmationService.createConfirmation(reportId, wargaId, deadlineHours)`

```
FUNCTION createConfirmation(reportId: String, wargaId: String, deadlineHours: int) RETURNS ConfirmationRequest
    // 1. Validasi keberadaan Report dan User (warga)
    report ← reportRepository.findById(reportId)
    IF report IS null THEN
        THROW RuntimeException("Report not found")
    
    warga ← userRepository.findById(wargaId)
    IF warga IS null THEN
        THROW RuntimeException("Warga not found")
    
    // 2. Buat objek ConfirmationRequest
    confirmation ← new ConfirmationRequest()
    confirmation.setReport(report)
    confirmation.setWarga(warga)
    
    // 3. Hitung deadline = waktu_sekarang + deadlineHours
    confirmation.setDeadlineAt(LocalDateTime.now().plusHours(deadlineHours))
    
    // 4. Set isLocked = false (masih bisa direspon)
    confirmation.setIsLocked(false)
    
    // 5. Simpan dan return
    RETURN confirmationRequestRepository.save(confirmation)
END FUNCTION
```

### 5. Algoritma Warga Merespon Konfirmasi — `ConfirmationService.respond(reportId, response)`

```
FUNCTION respond(reportId: String, response: ResponseType) RETURNS ConfirmationRequest
    // 1. Cari ConfirmationRequest berdasarkan reportId
    confirmation ← confirmationRequestRepository.findByReportReportId(reportId)
    IF confirmation IS null THEN
        THROW RuntimeException("Confirmation not found")
    
    // 2. Set response, timestamp, dan kunci konfirmasi
    confirmation.setResponse(response)
    confirmation.setRespondedAt(LocalDateTime.now())
    confirmation.setIsLocked(true)
    
    // 3. Logika berdasarkan tipe response
    CASE response OF
        ResponseType.TERIMA:
            // Warga menerima hasil → laporan ditutup
            confirmation.getReport().setStatus(Report.ReportStatus.DITUTUP)
            confirmation.getReport().setUpdatedAt(LocalDateTime.now())
            reportRepository.save(confirmation.getReport())
        
        ResponseType.TOLAK:
            // Warga menolak hasil → laporan masuk sengketa
            confirmation.getReport().setStatus(Report.ReportStatus.SENGKETA)
            confirmation.getReport().setUpdatedAt(LocalDateTime.now())
            reportRepository.save(confirmation.getReport())
    END CASE
    
    // 4. Simpan dan return
    RETURN confirmationRequestRepository.save(confirmation)
END FUNCTION
```

### 6. Algoritma Proses Timeout Konfirmasi Otomatis — `ConfirmationService.processTimeouts()`

```
FUNCTION processTimeouts() RETURNS void
    // 1. Cari semua ConfirmationRequest yang deadline-nya sudah lewat DAN belum direspon
    timedOut ← confirmationRequestRepository.findByDeadlineAtBeforeAndResponseIsNull(LocalDateTime.now())
    
    // 2. Loop setiap konfirmasi yang timeout
    FOR EACH confirmation IN timedOut DO
        // 2a. Set response = TIMEOUT
        confirmation.setResponse(ResponseType.TIMEOUT)
        confirmation.setRespondedAt(LocalDateTime.now())
        confirmation.setIsLocked(true)
        
        // 2b. Laporan otomatis ditutup
        confirmation.getReport().setStatus(Report.ReportStatus.DITUTUP)
        confirmation.getReport().setUpdatedAt(LocalDateTime.now())
        reportRepository.save(confirmation.getReport())
    END FOR
    
    // 3. Simpan semua perubahan
    confirmationRequestRepository.saveAll(timedOut)
END FUNCTION
```

### 7. Algoritma Warga Mengajukan Sengketa — `DisputeService.createDispute(reportId, filedById, reasonText, evidencePhotoUrl)`

```
FUNCTION createDispute(reportId: String, filedById: String, reasonText: String, evidencePhotoUrl: String) RETURNS DisputeRecord
    // 1. Validasi keberadaan Report dan User
    report ← reportRepository.findById(reportId)
    IF report IS null THEN
        THROW RuntimeException("Report not found")
    
    filedBy ← userRepository.findById(filedById)
    IF filedBy IS null THEN
        THROW RuntimeException("User not found")
    
    // 2. Buat objek DisputeRecord
    dispute ← new DisputeRecord()
    dispute.setReport(report)
    dispute.setFiledBy(filedBy)
    dispute.setReasonText(reasonText)
    dispute.setEvidencePhotoUrl(evidencePhotoUrl)
    dispute.setFiledAt(LocalDateTime.now())
    
    // 3. Ubah status laporan menjadi SENGKETA
    report.setStatus(Report.ReportStatus.SENGKETA)
    report.setUpdatedAt(LocalDateTime.now())
    reportRepository.save(report)
    
    // 4. Simpan dispute dan return
    RETURN disputeRecordRepository.save(dispute)
END FUNCTION
```

### 8. Algoritma Admin Meresolusi Sengketa — `DisputeService.resolveDispute(disputeId, resolvedById, resolution, resolutionNotes)`

```
FUNCTION resolveDispute(disputeId: String, resolvedById: String, resolution: ResolutionType, resolutionNotes: String) RETURNS DisputeRecord
    // 1. Cari DisputeRecord berdasarkan ID
    dispute ← disputeRecordRepository.findById(disputeId)
    IF dispute IS null THEN
        THROW RuntimeException("Dispute not found")
    
    // 2. Validasi resolver (admin)
    resolvedBy ← userRepository.findById(resolvedById)
    IF resolvedBy IS null THEN
        THROW RuntimeException("Resolver not found")
    
    // 3. Set hasil resolusi
    dispute.setResolvedBy(resolvedBy)
    dispute.setResolution(resolution)
    dispute.setResolutionNotes(resolutionNotes)
    dispute.setResolvedAt(LocalDateTime.now())
    
    // 4. Logika berdasarkan tipe resolusi
    CASE resolution OF
        ResolutionType.TUGASKAN_KEMBALI:
            // Sengketa diterima → laporan ditugaskan ulang ke petugas
            dispute.getReport().setStatus(Report.ReportStatus.DITUGASKAN)
        
        ResolutionType.TUTUP_LAPORAN:
            // Sengketa ditolak → laporan ditutup permanen
            dispute.getReport().setStatus(Report.ReportStatus.DITUTUP)
    END CASE
    
    dispute.getReport().setUpdatedAt(LocalDateTime.now())
    reportRepository.save(dispute.getReport())
    
    // 5. Simpan dan return
    RETURN disputeRecordRepository.save(dispute)
END FUNCTION
```

### 9. Algoritma REST Endpoint — `SengketaApiController`

```
// GET /api/sengketa
ENDPOINT GET /api/sengketa
    RETURN 200 OK + disputeService.getAllDisputes()

// GET /api/sengketa/{id}
ENDPOINT GET /api/sengketa/{id}
    dispute ← disputeService.getAllDisputes().stream()
        .filter(d WHERE d.getDisputeId() == id)
        .findFirst()
    IF dispute IS present THEN
        RETURN 200 OK + dispute
    ELSE
        RETURN 404 Not Found

// GET /api/sengketa/report/{reportId}
ENDPOINT GET /api/sengketa/report/{reportId}
    dispute ← disputeService.getDisputeByReportId(reportId)
    IF dispute IS present THEN
        RETURN 200 OK + dispute
    ELSE
        RETURN 404 Not Found

// GET /api/sengketa/pending
ENDPOINT GET /api/sengketa/pending
    RETURN 200 OK + disputeService.getPendingDisputes()

// POST /api/sengketa
ENDPOINT POST /api/sengketa
    PARAM: reportId, filedById, reasonText, evidencePhotoUrl
    TRY
        dispute ← disputeService.createDispute(reportId, filedById, reasonText, evidencePhotoUrl)
        RETURN 201 Created + dispute
    CATCH Exception
        RETURN 400 Bad Request

// POST /api/sengketa/{id}/resolve
ENDPOINT POST /api/sengketa/{id}/resolve
    PARAM: resolution (ResolutionType), resolutionNotes, resolvedById
    TRY
        RETURN 200 OK + disputeService.resolveDispute(id, resolvedById, resolution, resolutionNotes)
    CATCH Exception
        RETURN 400 Bad Request
```

### 10. Algoritma REST Endpoint — `TicketApiController`

```
// GET /api/tickets
ENDPOINT GET /api/tickets
    RETURN 200 OK + fieldTaskService.getAllTasks()

// GET /api/tickets/{id}
ENDPOINT GET /api/tickets/{id}
    task ← fieldTaskService.getTaskById(id)
    IF task IS present THEN RETURN 200 OK + task
    ELSE RETURN 404 Not Found

// GET /api/tickets/status/{status}
ENDPOINT GET /api/tickets/status/{status}
    RETURN 200 OK + fieldTaskService.getTasksByOfficerAndStatus("", status)

// GET /api/tickets/petugas/{petugasId}
ENDPOINT GET /api/tickets/petugas/{petugasId}
    RETURN 200 OK + fieldTaskService.getTasksByOfficer(petugasId)

// POST /api/tickets
ENDPOINT POST /api/tickets
    PARAM: reportId, officerId, assignedById
    TRY
        task ← fieldTaskService.createTask(reportId, officerId, assignedById)
        RETURN 201 Created + task
    CATCH Exception
        RETURN 400 Bad Request

// POST /api/tickets/{id}/start
ENDPOINT POST /api/tickets/{id}/start
    PARAM: latitude (optional), longitude (optional)
    TRY
        RETURN 200 OK + fieldTaskService.startTask(id, latitude, longitude)
    CATCH Exception
        RETURN 400 Bad Request

// POST /api/tickets/{id}/complete
ENDPOINT POST /api/tickets/{id}/complete
    TRY
        RETURN 200 OK + fieldTaskService.completeTask(id)
    CATCH Exception
        RETURN 400 Bad Request

// GET /api/tickets/count
ENDPOINT GET /api/tickets/count
    counts ← Map(
        "baru"              → fieldTaskService.countByStatus(TaskStatus.BARU),
        "sedang_dikerjakan" → fieldTaskService.countByStatus(TaskStatus.SEDANG_DIKERJAKAN),
        "tertunda"          → fieldTaskService.countByStatus(TaskStatus.TERTUNDA),
        "selesai"           → fieldTaskService.countByStatus(TaskStatus.SELESAI)
    )
    RETURN 200 OK + counts
```

### 11. Algoritma Relasi Status Report dalam Siklus Eksekusi Lapangan

Berikut adalah diagram alur perubahan `Report.ReportStatus` yang terjadi di komponen ini:

```
DITUGASKAN
    │  [Admin dinas menugaskan petugas → FieldTaskService.createTask()]
    │
    ▼
SEDANG_DIKERJAKAN
    │  [Petugas mulai → FieldTaskService.startTask()]
    │
    ├──→ TERTUNDA
    │      [Petugas mengajukan penundaan → TaskPostponement]
    │
    ▼
SELESAI
    │  [Petugas selesai → FieldTaskService.completeTask()]
    │
    ▼
MENUNGGU_KONFIRMASI  (implisit, setelah ConfirmationRequest dibuat)
    │
    ├──→ [Warga TERIMA atau TIMEOUT] → DITUTUP
    │       ConfirmationService.respond(TERIMA) atau processTimeouts()
    │
    └──→ [Warga TOLAK] → SENGKETA
            ConfirmationService.respond(TOLAK) atau
            DisputeService.createDispute()
                │
                ├──→ [Admin: TUGASKAN_KEMBALI] → DITUGASKAN
                │       DisputeService.resolveDispute(TUGASKAN_KEMBALI)
                │
                └──→ [Admin: TUTUP_LAPORAN] → DITUTUP
                        DisputeService.resolveDispute(TUTUP_LAPORAN)
```
