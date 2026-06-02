# Prompt AI Agent — Memperbaiki Data Flow SLA, Tugas, dan Laporan

## Masalah

Data flow antara ajuan penundaan (petugas), pause/resume SLA (admin dinas), status tugas, dan status laporan tidak konsisten. Ada beberapa bug serius:

1. **`pauseSla()` membuat duplikat `TaskPostponement`** — setiap admin pause SLA, terbuat record `TaskPostponement` baru yang tidak diperlukan.
2. **`resumeSla()` memanggil `startTask()`** — ini me-reset `startedAt` ke waktu sekarang, menghilangkan waktu mulai asli tugas.
3. **Tidak ada method khusus untuk mengubah status tugas** — semua method punya side effect.
4. **Warga tidak melihat status SLA** — di halaman detail laporan, warga tidak tahu apakah SLA sedang dijeda atau terlambat.

---

## 1. Bug: `pauseSla()` Duplikat TaskPostponement

### Lokasi
`AdminDinasController.java:971-997` — method `adminDinasPauseSla()`

### Kode Sekarang
```java
@PostMapping("/admin/dinas/pause-sla")
public String adminDinasPauseSla(...) {
    FieldTask task = fieldTaskService.getTaskById(taskId)...;
    if (task.getSlaRecord() != null) {
        slaRecordService.pauseSla(task.getSlaRecord().getSlaId(), reason, adminId);  // ✅ PAUSE SLA SUDAH BENAR
    }
    task.setTaskStatus(FieldTask.TaskStatus.TERTUNDA);        // ❌ Baris 987: Set status di memory (tidak di-save)
    fieldTaskService.postponeTask(taskId, reason, adminId);   // ❌ Baris 990: Juga SET TERTUNDA + BUAT TaskPostponement BARU
}
```

### Akibat
Setiap admin pause SLA akan:
- Membuat record `TaskPostponement` baru dengan `ApprovalStatus = DISETUJUI` (tidak diperlukan untuk pause biasa)
- Mengotori database dengan data penundaan yang tidak pernah diminta petugas

### Perbaikan
Hapus baris 987 (`task.setTaskStatus`) dan ganti baris 990 dengan method yang **hanya mengubah status tugas tanpa membuat TaskPostponement**.

---

## 2. Bug: `resumeSla()` Memanggil `startTask()` — Reset `startedAt`

### Lokasi
`AdminDinasController.java:999-1022` — method `adminDinasResumeSla()`

### Kode Sekarang
```java
@PostMapping("/admin/dinas/resume-sla")
public String adminDinasResumeSla(...) {
    FieldTask task = fieldTaskService.getTaskById(taskId)...;
    if (task.getSlaRecord() != null) {
        slaRecordService.resumeSla(task.getSlaRecord().getSlaId());  // ✅ RESUME SLA SUDAH BENAR
    }
    task.setTaskStatus(FieldTask.TaskStatus.SEDANG_DIKERJAKAN);      // ❌ Baris 1014: Set status di memory
    fieldTaskService.startTask(taskId, null, null);                  // ❌ Baris 1015: START TASK — reset startedAt + ada validasi jarak!
}
```

### Akibat
`startTask()` (di `FieldTaskServiceImpl.java:154-186`) melakukan:
1. Validasi geofencing (jarak petugas) — bisa throw exception!
2. `task.setStartedAt(LocalDateTime.now())` — **me-reset waktu mulai asli!**
3. Set latitude/longitude petugas

Ini salah karena resume ≠ start. Resume harusnya hanya mengembalikan status tugas ke SEDANG_DIKERJAKAN tanpa mengubah `startedAt`.

### Perbaikan
Ganti dengan method baru yang **hanya mengubah status tugas ke SEDANG_DIKERJAKAN tanpa side effect**.

---

## 3. Perbaikan yang Diperlukan

### a. Buat Method Baru di `FieldTaskService` dan `FieldTaskServiceImpl`

Tambahkan dua method berikut:

```java
// ========== DI FieldTaskService.java (interface) ==========

/**
 * Mengubah status tugas menjadi SEDANG_DIKERJAKAN tanpa side effect.
 * Tidak memvalidasi jarak, tidak mengubah startedAt, tidak mengubah koordinat.
 */
FieldTask resumeTask(String taskId);

/**
 * Mengubah status tugas menjadi TERTUNDA tanpa membuat TaskPostponement baru.
 */
FieldTask setTaskAsTertunda(String taskId);  // ✅ SUDAH ADA (dari agent sebelumnya)

/**
 * Mengubah status tugas menjadi SEDANG_DIKERJAKAN.
 */
FieldTask setTaskAsSedangDikerjakan(String taskId);  // PERLU BARU
```

#### Implementasi `resumeTask()` di `FieldTaskServiceImpl.java`:
```java
@Override
public FieldTask resumeTask(String taskId) {
    FieldTask task = fieldTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
    task.setTaskStatus(TaskStatus.SEDANG_DIKERJAKAN);
    // TIDAK mengubah startedAt, TIDAK validasi jarak, TIDAK set koordinat
    return fieldTaskRepository.save(task);
}
```

#### Implementasi `setTaskAsSedangDikerjakan()` di `FieldTaskServiceImpl.java`:
```java
@Override
public FieldTask setTaskAsSedangDikerjakan(String taskId) {
    FieldTask task = fieldTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
    task.setTaskStatus(TaskStatus.SEDANG_DIKERJAKAN);
    return fieldTaskRepository.save(task);
}
```

### b. Perbaiki `pauseSla()` di `AdminDinasController.java`

**SEBELUM:**
```java
task.setTaskStatus(FieldTask.TaskStatus.TERTUNDA);
fieldTaskService.postponeTask(taskId, reason, adminId);  // ❌ buat TaskPostponement baru
```

**SESUDAH:**
```java
fieldTaskService.setTaskAsTertunda(taskId);  // ✅ hanya ubah status, tanpa buat TaskPostponement
```

### c. Perbaiki `resumeSla()` di `AdminDinasController.java`

**SEBELUM:**
```java
task.setTaskStatus(FieldTask.TaskStatus.SEDANG_DIKERJAKAN);
fieldTaskService.startTask(taskId, null, null);  // ❌ reset startedAt
```

**SESUDAH:**
```java
fieldTaskService.resumeTask(taskId);  // ✅ hanya ubah status, tanpa side effect
```

### d. Gunakan Method yang Sama di `approve-postponement`

Di `AdminDinasController.adminDinasApprovePostponement()`, method `setTaskAsTertunda()` SUDAH digunakan dengan benar (tidak perlu diubah):

```java
// ✅ SUDAH BENAR:
fieldTaskService.setTaskAsTertunda(task.getTaskId());
if (freshTask.getSlaRecord() != null) {
    slaRecordService.pauseSla(freshTask.getSlaRecord().getSlaId(), ...);
}
```

---

## 4. Tampilkan Status SLA/Waktu Tugas ke Warga

### Lokasi
`src/main/resources/templates/warga/report-detail.html`

### Yang Perlu Ditambahkan

#### a. Di sebelah atau di bawah SLA Deadline Countdown yang sudah ada
Di area sekitar baris 910-1005 (sesuai hasil eksplorasi sebelumnya), tampilkan badge status SLA:

```html
<!-- Status SLA -->
<div th:if="${slaStatus != null}" class="mt-2">
  <span th:if="${slaStatus == 'TERTUNDA'}"
        class="text-xs px-2 py-1 rounded bg-yellow-100 text-yellow-700 font-semibold">
    SLA Sedang Dijeda
  </span>
  <span th:if="${slaStatus == 'TERLAMBAT'}"
        class="text-xs px-2 py-1 rounded bg-red-100 text-red-700 font-semibold">
    SLA Terlambat
  </span>
</div>
```

#### b. Di controller warga, pastikan data SLA dikirim ke view
Cari di `WargaController.java` method yang handle `/warga/report-detail`. Tambahkan:

```java
// Ambil status SLA untuk ditampilkan ke warga
Map<String, Object> slaInfo = slaMonitoringService.getReportSlaStatus(reportId);
model.addAttribute("slaStatus", slaInfo.get("status"));
model.addAttribute("slaDeadline", slaInfo.get("deadline"));
model.addAttribute("slaPausedMinutes", slaInfo.get("pausedMinutes"));
```

---

## 5. Tampilkan Status Tugas (TERTUNDA) ke Warga

Di `warga/report-detail.html`, setelah informasi tugas, tambahkan indikasi jika tugas sedang ditunda:

```html
<div th:if="${taskStatus == 'TERTUNDA'}"
     class="mt-2 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
  <p class="text-sm text-yellow-800">
    <i data-lucide="pause-circle" class="w-4 h-4 inline mr-1"></i>
    Tugas sedang ditunda. Admin dinas akan melanjutkan setelah kendala selesai.
  </p>
</div>
```

---

## Ringkasan File yang Perlu Diubah

| File | Perubahan |
|------|-----------|
| `service/FieldTaskService.java` | Tambah method `resumeTask(String taskId)` dan `setTaskAsSedangDikerjakan(String taskId)` |
| `service/FieldTaskServiceImpl.java` | Implementasi `resumeTask()` dan `setTaskAsSedangDikerjakan()` |
| `controller/AdminDinasController.java` | `pauseSla()`: ganti `postponeTask` → `setTaskAsTertunda`. `resumeSla()`: ganti `startTask` → `resumeTask` |
| `controller/WargaController.java` | Tambah SLA status + task status ke model di `/warga/report-detail` |
| `templates/warga/report-detail.html` | Tampilkan badge SLA (TERTUNDA/TERLAMBAT) dan status tugas ditunda |

---

## Catatan Penting

- Method `setTaskAsTertunda(String taskId)` **SUDAH ADA** (dibuat oleh agent sebelumnya di `FieldTaskServiceImpl.java:282-289`)
- Method `setTaskAsSedangDikerjakan()` bisa berupa method yang sama dengan `resumeTask()` — cukup pilih satu nama yang konsisten
- Jangan ubah `approve-postponement` endpoint — sudah benar menggunakan `setTaskAsTertunda()`
- `postponeTask()` hanya dipanggil dari `pauseSla()` (line 990) — setelah diganti dengan `setTaskAsTertunda()`, method ini tidak terpakai. Bisa dihapus nanti atau dibiarkan
- Test: restart app, login sebagai admin dinas, buka progress page, coba pause → resume → cek SLA record, task status, dan `startedAt` tidak berubah
