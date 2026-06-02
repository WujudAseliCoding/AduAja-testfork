# PROMPT FIX — Antigravity

Perbaiki penyimpangan berikut antara spesifikasi dan implementasi yang sudah dibuat. **Hanya item di bawah ini yang perlu diperbaiki.**

---

## Fix 1: AuditLog.java — Setter harus PACKAGE-PRIVATE (Immutable)

**Lokasi:** `src/main/java/com/plr/aduaja/model/AuditLog.java`

Saat ini semua setter masih `public`. Ubah menjadi **package-private** (tanpa keyword `public`):

### SEBELUM (SALAH):
```java
public void setLogId(String logId) { this.logId = logId; }
public void setActor(User actor) { this.actor = actor; }
public void setReport(Report report) { this.report = report; }
public void setTask(FieldTask task) { this.task = task; }
public void setActionType(String actionType) { this.actionType = actionType; }
public void setOldValue(String oldValue) { this.oldValue = oldValue; }
public void setNewValue(String newValue) { this.newValue = newValue; }
public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
```

### SESUDAH (BENAR) — package-private, tanpa `public`:
```java
void setLogId(String logId) { this.logId = logId; }
void setActor(User actor) { this.actor = actor; }
void setReport(Report report) { this.report = report; }
void setTask(FieldTask task) { this.task = task; }
void setActionType(String actionType) { this.actionType = actionType; }
void setOldValue(String oldValue) { this.oldValue = oldValue; }
void setNewValue(String newValue) { this.newValue = newValue; }
void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
```

**Penjelasan:** Package-private setter membuat AuditLog hanya bisa dimodifikasi oleh class dalam package yang sama (`com.plr.aduaja.model`), yaitu hanya oleh `AuditLogServiceImpl` yang membuat log baru. Class lain hanya bisa membaca (READ-ONLY). Ini adalah **Encapsulation + Immutability**.

---

## Fix 2: AuditLog.java — Ganti field `task` dengan `targetType` + `targetId`

**Lokasi:** `src/main/java/com/plr/aduaja/model/AuditLog.java`

Hapus relasi `@ManyToOne` ke `FieldTask task`. Ganti dengan 2 field String `targetType` dan `targetId`.

### HAPUS:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "task_id")
private FieldTask task;

// Getter
public FieldTask getTask() { return task; }

// Setter — package-private
void setTask(FieldTask task) { this.task = task; }
```

### TAMBAH:
```java
@Column(name = "target_type")
private String targetType;

@Column(name = "target_id")
private String targetId;

// Getter
public String getTargetType() { return targetType; }
public String getTargetId() { return targetId; }

// Setter — package-private
void setTargetType(String targetType) { this.targetType = targetType; }
void setTargetId(String targetId) { this.targetId = targetId; }
```

**Penjelasan:** `targetType` berisi string seperti `"REPORT"`, `"FIELD_TASK"`, `"USER"`. `targetId` berisi ID dari entity yang dilog. Pendekatan ini lebih fleksibel dan tidak bergantung pada entity `FieldTask` (yang merupakan milik modul Orang 3).

---

## Fix 3: AuditLogServiceImpl.java — Sesuaikan dengan perubahan AuditLog

**Lokasi:** `src/main/java/com/plr/aduaja/service/AuditLogServiceImpl.java`

### Method `log(User, Report, FieldTask, ...)` — ganti FieldTask dengan targetType/targetId:

**SEBELUM:**
```java
@Override
@Transactional
public AuditLog log(User actor, Report report, FieldTask task, String action,
                    String oldVal, String newVal, String ipAddress, String deviceInfo) {
    AuditLog auditLog = new AuditLog();
    auditLog.setActor(actor);
    auditLog.setReport(report);
    auditLog.setTask(task);
    auditLog.setActionType(action);
    auditLog.setOldValue(oldVal);
    auditLog.setNewValue(newVal);
    auditLog.setIpAddress(ipAddress);
    auditLog.setDeviceInfo(deviceInfo);
    auditLog.setLoggedAt(LocalDateTime.now());
    return auditLogRepository.save(auditLog);
}
```

**SESUDAH:**
```java
@Override
@Transactional
public AuditLog log(User actor, String targetType, String targetId, String action,
                    String oldVal, String newVal, String ipAddress, String deviceInfo) {
    AuditLog auditLog = new AuditLog();
    auditLog.setActor(actor);
    auditLog.setTargetType(targetType);
    auditLog.setTargetId(targetId);
    auditLog.setActionType(action);
    auditLog.setOldValue(oldVal);
    auditLog.setNewValue(newVal);
    auditLog.setIpAddress(ipAddress);
    auditLog.setDeviceInfo(deviceInfo);
    auditLog.setLoggedAt(LocalDateTime.now());
    return auditLogRepository.save(auditLog);
}
```

### Interface AuditLogService.java — sesuaikan signature method:

**SEBELUM:**
```java
AuditLog log(User actor, Report report, FieldTask task, String action,
             String oldVal, String newVal, String ipAddress, String deviceInfo);
```

**SESUDAH:**
```java
AuditLog log(User actor, String targetType, String targetId, String action,
             String oldVal, String newVal, String ipAddress, String deviceInfo);
```

### Method `getLogsByTask()` — ganti dengan `getLogsByTarget()`:

**SEBELUM:**
```java
@Override
public List<AuditLog> getLogsByTask(String taskId) {
    return auditLogRepository.findByTaskTaskId(taskId);
}
```

**SESUDAH:**
```java
@Override
public List<AuditLog> getLogsByTarget(String targetType, String targetId) {
    return auditLogRepository.findByTargetTypeAndTargetId(targetType, targetId);
}
```

**Interface:** tambahkan method `getLogsByTarget(String targetType, String targetId)`.

---

## Fix 4: AuditLogRepository.java — Sesuaikan query method

**Lokasi:** `src/main/java/com/plr/aduaja/repository/AuditLogRepository.java`

### SEBELUM:
```java
List<AuditLog> findByTaskTaskId(String taskId);
```

### SESUDAH:
```java
List<AuditLog> findByTargetTypeAndTargetId(String targetType, String targetId);
```

---

## Fix 5: Controller prefix — Ubah `/admin/m4` jadi `/admin`

**Lokasi:** `src/main/java/com/plr/aduaja/controller/AdminControllerModul4.java`

### SEBELUM:
```java
@Controller
@RequestMapping("/admin/m4")
public class AdminControllerModul4 {
```

### SESUDAH:
```java
@Controller
@RequestMapping("/admin")
public class AdminControllerModul4 {
```

### Sesuaikan semua `redirect:` di method POST:

| Method | SEBELUM | SESUDAH |
|--------|---------|---------|
| `prosesSengketa` | `redirect:/admin/m4/sengketa` | `redirect:/admin/sengketa` |
| `prosesMerge` | `redirect:/admin/m4/merge` | `redirect:/admin/merge` |
| `prosesValidasi` | `redirect:/admin/m4/validation` | `redirect:/admin/validation` |

---

## Fix 6: Update URL panel SLA di Controller

**Lokasi:** `src/main/java/com/plr/aduaja/controller/AdminControllerModul4.java`

Panel SLA `/admin/m4/sla` juga berubah jadi `/admin/sla` (karena prefix sudah `/admin`).

---

## Fix 7 (Optional): Hapus method repository yang tidak dipakai

**Lokasi:** `src/main/java/com/plr/aduaja/repository/SlaRecordRepository.java`

Method `findBySlaDeadlineAtBeforeAndCurrentStatusNot` sudah tidak dipakai (sudah diganti dengan `findOverdue` yang pakai `@Query`). Boleh dihapus atau dibiarkan saja.

---

## CHECKLIST SETELAH FIX

```
[ ] AuditLog setter = package-private (tanpa public)
[ ] AuditLog pakai targetType + targetId (bukan FieldTask task)
[ ] AuditLogServiceImpl pakai targetType/targetId
[ ] AuditLogService interface signature disesuaikan
[ ] AuditLogRepository pakai findByTargetTypeAndTargetId()
[ ] Controller prefix: /admin (bukan /admin/m4)
[ ] Semua redirect: sudah sesuai prefix /admin
[ ] `mvn compile` sukses
[ ] Akses: http://localhost:8080/admin/sengketa ✅
```

---

## URUTAN PERBAIKAN (dari yang paling penting)

1. **Fix 1** — AuditLog package-private setter (Encapsulation)
2. **Fix 2** — AuditLog ganti FieldTask → targetType/targetId
3. **Fix 3** — AuditLogServiceImpl sesuaikan
4. **Fix 4** — AuditLogRepository sesuaikan
5. **Fix 5** — Controller prefix
6. **Fix 6** — redirect URL

Setelah semua selesai, jalankan `mvn compile` dan pastikan tidak ada error.
