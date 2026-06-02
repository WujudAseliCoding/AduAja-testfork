# SDD — Design Component: Notification, AuditLog & Attendance

## 1. Notification Service

### 1.1 Entity — `Notification`

| Field | Type | DB Column | Constraints |
|-------|------|-----------|-------------|
| `notificationId` | `String` (UUID) | `notification_id` | PK, auto-generated |
| `recipient` | `@ManyToOne → User` | `recipient_id` | FK, NOT NULL |
| `report` | `@ManyToOne → Report` | `report_id` | FK, nullable |
| `title` | `String` (200) | `title` | NOT NULL |
| `messageText` | `String` (TEXT) | `message_text` | NOT NULL |
| `referenceType` | `String` (50) | `reference_type` | — |
| `referenceId` | `String` | `reference_id` | — |
| `notificationType` | `Enum → NotificationType` | `notification_type` | Stored as string |
| `isRead` | `Boolean` | `is_read` | NOT NULL, default false |
| `sentAt` | `LocalDateTime` | `sent_at` | NOT NULL |
| `createdAt` | `LocalDateTime` | `created_at` | From BaseEntity |
| `updatedAt` | `LocalDateTime` | `updated_at` | From BaseEntity |

**Inheritance:** `Notification extends BaseEntity` (memiliki `createdAt` dan `updatedAt`)

**NotificationType enum:** `STATUS_BERUBAH`, `SENGKETA_MASUK`, `SENGKETA_DIPUTUS`, `TUGAS_BARU`, `SLA_WARNING`, `SELESAI_OTOMATIS`, `LAPORAN_DITERIMA`, `LAPORAN_DIVALIDASI`, `LAPORAN_DITOLAK`, `LAPORAN_PERLU_REVISI`

### 1.2 Interface — `NotificationService`

```
<<interface>>
NotificationService
├── getNotificationsByUser(userId)                → List<Notification>
├── getUnreadNotificationsByUser(userId)           → List<Notification>
├── getNotificationsByType(userId, referenceType)  → List<Notification>
├── createNotification(userId, title, msg, refType, refId) → Notification
├── markAsRead(notificationId)                     → Notification
├── markAllAsReadByUser(userId)                    → int
├── countUnreadByUser(userId)                      → long
│
├── default getUnreadNotifications(userId)         → delegates to getUnreadNotificationsByUser
├── default getUnreadCount(userId)                 → delegates to countUnreadByUser
├── default markAllAsRead(userId)                  → delegates to markAllAsReadByUser
├── default createNotification(userId, msg, type)  → delegates (SYSTEM refType)
└── default createNotificationForReport(...)        → delegates (REPORT refType)
```

**Abstraction:** Controller/Warga hanya inject `NotificationService` (interface), tidak tahu implementasi.

### 1.3 Implementation — `NotificationServiceImpl`

```
@Service @Transactional
NotificationServiceImpl implements NotificationService
├── @Autowired NotificationRepository
├── @Autowired UserRepository
└── All 7 abstract methods @Override
```

### 1.4 Repository — `NotificationRepository`

Methods:

| Method | Type |
|--------|------|
| `findByRecipientOrderBySentAtDesc(User)` | Derived |
| `findByRecipientAndIsReadOrderBySentAtDesc(User, Boolean)` | Derived |
| `findByRecipientAndReferenceTypeOrderBySentAtDesc(User, String)` | Derived |
| `findByRecipientUserId(String)` | Derived |
| `findByRecipientUserIdAndIsReadFalse(String)` | Derived |
| `findByRecipientUserIdAndNotificationType(String, NotificationType)` | Derived |
| `findByRecipientUserIdOrderBySentAtDesc(String)` | Derived |
| `countByRecipientUserIdAndIsReadFalse(String)` | Derived |
| `countByRecipientUserId(String)` | Derived |
| `countByUserIdAndIsRead(@Query)` | Custom JPQL |
| `markAllAsReadByUserId(@Modifying @Query)` | Custom JPQL UPDATE |
| `findRecentByUserId(@Query)` | Custom JPQL |

### 1.5 REST API

| Method | Endpoint | Service Method |
|--------|----------|----------------|
| GET | `/api/notifications/user/{userId}` | `getNotificationsByUser` |
| GET | `/api/notifications/user/{userId}/unread` | `getUnreadNotifications` |
| GET | `/api/notifications/user/{userId}/unread/count` | `getUnreadCount` |
| POST | `/api/notifications` | `createNotification(userId, msg, type)` |
| POST | `/api/notifications/{id}/read` | `markAsRead` |
| POST | `/api/notifications/user/{userId}/read-all` | `markAllAsRead` |

### 1.6 Class Diagram

```
┌─────────────────────────────────────────────┐
│               BaseEntity                    │
│  (@MappedSuperclass)                        │
│  - createdAt : LocalDateTime                │
│  - updatedAt : LocalDateTime                │
└─────────────────────┬───────────────────────┘
                      │ extends
┌──────────────────────┴────────────────────────┐
│             Notification                       │
│  (@Entity, @Table="notifications")             │
│  - notificationId : String (PK, UUID)          │
│  - recipient : @ManyToOne → User              │
│  - report : @ManyToOne → Report               │
│  - title : String                              │
│  - messageText : String (TEXT)                 │
│  - referenceType : String                      │
│  - referenceId : String                        │
│  - notificationType : Enum(NotificationType)   │
│  - isRead : Boolean (default false)            │
│  - sentAt : LocalDateTime                      │
└────────────────┬───────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│        NotificationRepository                 │
│  <<interface>> extends JpaRepository          │
│  + findByRecipientUserIdOrderBySentAtDesc()   │
│  + findByRecipientUserIdAndIsReadFalse()      │
│  + markAllAsReadByUserId(@Modifying @Query)   │
│  + countByRecipientUserIdAndIsReadFalse()     │
└────────────────────┬──────────────────────────┘
                     │ implements
┌────────────────────┴──────────────────────────┐
│        NotificationService <<interface>>       │
│  + getNotificationsByUser(userId)              │
│  + createNotification(userId, title, msg, ...) │
│  + markAsRead(notificationId)                  │
│  + countUnreadByUser(userId)                   │
│  + default methods (backward compat)           │
└────────────────────┬──────────────────────────┘
                     │ implements
┌────────────────────┴──────────────────────────┐
│        NotificationServiceImpl                 │
│  (@Service, @Transactional)                    │
│  - notificationRepository                      │
│  - userRepository                              │
└────────────────────────────────────────────────┘
```

---

## 2. AuditLog Service

### 2.1 Entity — `AuditLog`

| Field | Type | DB Column | Constraints |
|-------|------|-----------|-------------|
| `logId` | `String` (UUID) | `log_id` | PK, auto-generated |
| `actor` | `@ManyToOne → User` | `actor_id` | FK |
| `report` | `@ManyToOne → Report` | `report_id` | FK, nullable |
| `targetType` | `String` | `target_type` | Polymorphic target |
| `targetId` | `String` | `target_id` | Polymorphic target ID |
| `actionType` | `String` (100) | `action_type` | NOT NULL |
| `oldValue` | `String` (TEXT) | `old_value` | — |
| `newValue` | `String` (TEXT) | `new_value` | — |
| `ipAddress` | `String` (45) | `ip_address` | IPv6-capable |
| `deviceInfo` | `String` (255) | `device_info` | — |
| `loggedAt` | `LocalDateTime` | `logged_at` | NOT NULL |

**Encapsulation:** Semua setter **package-private** (tanpa `public`). Hanya class dalam package `com.plr.aduaja.model` bisa mengubah nilai. Getter semuanya **public**.

### 2.2 Interface — `AuditLogService`

```
<<interface>>
AuditLogService
│
├── log(User actor, String actionType, String oldVal, String newVal)                    // 4 param (basic)
├── log(User actor, Report report, String action, String oldVal, String newVal)         // 5 param (OVERLOAD)
├── log(User actor, String targetType, String targetId, String action,
│       String oldVal, String newVal, String ipAddress, String deviceInfo)              // 8 param (OVERLOAD)
│
├── getLogsByReport(String reportId)
├── getLogsByActor(String actorId)
├── getLogsByTarget(String targetType, String targetId)
└── getAllLogs()
```

**Polymorphism (Overloading):** Tiga method `log()` dengan nama sama, parameter berbeda (4, 5, dan 8 parameter).

### 2.3 Factory — `AuditLogFactory`

```
┌─ package: com.plr.aduaja.model ──────────────────────────┐
│  AuditLogFactory                                          │
│  + create(actor, actionType, oldVal, newVal)              │
│      → AuditLog (default: IP="0.0.0.0", device="System") │
│  + createWithReport(actor, report, action, oldVal, newVal)│
│      → AuditLog (targetType="REPORT", targetId=reportId)  │
│  + createFull(actor, targetType, targetId, action,        │
│               oldVal, newVal, ipAddress, deviceInfo)       │
│      → AuditLog (full custom)                             │
└───────────────────────────────────────────────────────────┘
```

**Factory Pattern:** Karena setter `AuditLog` package-private, hanya class dalam package yang sama (`com.plr.aduaja.model`) yang bisa mengakses setter. `AuditLogServiceImp` (di package `service`) tidak bisa langsung membuat `AuditLog` — harus melalui `AuditLogFactory`.

### 2.4 Repository — `AuditLogRepository`

| Method | Type |
|--------|------|
| `findByReportReportId(String)` | Derived |
| `findByTargetTypeAndTargetId(String, String)` | Derived |
| `findByActorUserId(String)` | Derived |
| `findByReportReportIdOrderByLoggedAtDesc(String)` | Derived |
| `findByActionType(String)` | Derived |

### 2.5 Class Diagram

```
┌─────────────────────────────────────────────┐
│               BaseEntity                    │
└─────────────────────┬───────────────────────┘
                      │ extends
┌──────────────────────┴────────────────────────┐
│              AuditLog                          │
│  (@Entity, @Table="audit_logs")                │
│  - logId : String (PK, UUID)                   │
│  - actor : @ManyToOne → User                  │
│  - report : @ManyToOne → Report               │
│  - targetType : String                         │
│  - targetId : String                           │
│  - actionType : String (NOT NULL)              │
│  - oldValue : String (TEXT)                    │
│  - newValue : String (TEXT)                    │
│  - ipAddress : String                          │
│  - deviceInfo : String                         │
│  - loggedAt : LocalDateTime                    │
│  Setter: PACKAGE-PRIVATE ← Encapsulation       │
└────────────────┬───────────────────────────────┘
                 │
                 ├──────────────────────────────┐
                 ▼                              ▼
┌──────────────────────────────┐  ┌───────────────────────────────┐
│   AuditLogRepository         │  │   AuditLogFactory              │
│   <<interface>>              │  │   (package: model)             │
│   extends JpaRepository      │  │   + create(...)                │
└──────────┬───────────────────┘  │   + createWithReport(...)      │
           │                      │   + createFull(...)            │
           │ implements           └───────────────┬───────────────┘
           ▼                                      │
┌──────────────────────────────┐                  │
│  AuditLogService             │                  │
│  <<interface>>               │                  │
│  + log(4 param)              │                  │
│  + log(5 param) ← OVERLOAD   │                  │
│  + log(8 param) ← OVERLOAD   │                  │
│  + getLogsByReport()         │                  │
│  + getLogsByActor()          │                  │
│  + getLogsByTarget()         │                  │
└──────────┬───────────────────┘                  │
           │ implements                           │
           ▼                                      │
┌──────────────────────────────┐                  │
│  AuditLogServiceImpl         │ delegates to ────┘
│  (@Service, @Transactional)  │  (via Factory)
│  - auditLogRepository        │
└──────────────────────────────┘
```

---

## 3. Attendance Service

### 3.1 Entity — `OfficerAttendance`

| Field | Type | DB Column | Constraints |
|-------|------|-----------|-------------|
| `attendanceId` | `String` (UUID) | `attendance_id` | PK, auto-generated |
| `officer` | `@ManyToOne → User` | `officer_id` | FK, NOT NULL |
| `checkInAt` | `LocalDateTime` | `check_in_at` | NOT NULL |
| `checkInLatitude` | `BigDecimal(10,8)` | `check_in_latitude` | — |
| `checkInLongitude` | `BigDecimal(11,8)` | `check_in_longitude` | — |
| `checkOutAt` | `LocalDateTime` | `check_out_at` | — |
| `deviceInfo` | `String` | `device_info` | — |
| `shiftStatus` | `Enum → ShiftStatus` | `shift_status` | NOT NULL |

**ShiftStatus enum:** `AKTIF` → `ISTIRAHAT` → `SELESAI_SHIFT` (state machine)

### 3.2 Service — `AttendanceService`

```
AttendanceService (@Service)
├── getAllAttendance()                          → List<OfficerAttendance>
├── getAttendanceByOfficer(String officerId)   → List<OfficerAttendance>
├── getCurrentShift(String officerId)          → Optional<OfficerAttendance>
├── checkIn(officerId, lat, lng, deviceInfo)   → OfficerAttendance
├── checkOut(String attendanceId)              → OfficerAttendance
├── setBreak(String attendanceId)              → OfficerAttendance
└── resumeFromBreak(String attendanceId)       → OfficerAttendance
```

**Catatan:** `AttendanceService` belum di-refactor ke interface + impl. Saat ini masih langsung `@Service class`.

### 3.3 Repository — `OfficerAttendanceRepository`

| Method | Type |
|--------|------|
| `findByOfficerUserId(String)` | Derived |
| `findTopByOfficerUserIdAndShiftStatusNotOrderByCheckInAtDesc(String, ShiftStatus)` | Derived |
| `findByOfficerUserIdAndCheckInAtBetween(String, LocalDateTime, LocalDateTime)` | Derived |
| `findByShiftStatus(ShiftStatus)` | Derived |

### 3.4 REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/attendance` | List all attendance |
| GET | `/api/attendance/officer/{officerId}` | By officer |
| GET | `/api/attendance/officer/{officerId}/current` | Current active shift |
| POST | `/api/attendance/checkin` | Check-in (params: officerId, lat, lng, deviceInfo) |
| POST | `/api/attendance/checkout/{attendanceId}` | Check-out |
| POST | `/api/attendance/break/{attendanceId}` | Start break |
| POST | `/api/attendance/resume/{attendanceId}` | Resume from break |

### 3.5 Shift State Machine

```
    ┌──────────┐
    │  AKTIF   │ ◄──────┐
    └────┬─────┘        │
         │              │
    ┌────▼─────┐   ┌────┴──────┐
    │ ISTIRAHAT│   │ resume()  │
    └────┬─────┘   └───────────┘
         │
    ┌────▼────────┐
    │SELESAI_SHIFT│
    └─────────────┘
```

---

## 4. OOP 4 Pilar

### 4.1 Inheritance

```
BaseEntity (@MappedSuperclass)
├── createdAt : LocalDateTime
├── updatedAt : LocalDateTime
├── @PrePersist → set createdAt & updatedAt
└── @PreUpdate → set updatedAt

Notification extends BaseEntity    ← Inheritance
AuditLog extends BaseEntity        ← Inheritance
```

### 4.2 Polymorphism

**Runtime (@Override):**
- `NotificationServiceImpl implements NotificationService` — semua method dikasih `@Override`
- `AuditLogServiceImpl implements AuditLogService` — semua method dikasih `@Override`

**Compile-time (Overloading):**
```java
// AuditLogService — 3 overloads of log()
AuditLog log(User actor, String actionType, String oldVal, String newVal);                      // 4 param
AuditLog log(User actor, Report report, String action, String oldVal, String newVal);           // 5 param
AuditLog log(User actor, String targetType, String targetId, String action,
             String oldVal, String newVal, String ipAddress, String deviceInfo);                 // 8 param
```

### 4.3 Abstraction

```java
// Controller hanya inject Interface:
@Autowired private NotificationService notificationService;  // ← Interface
@Autowired private AuditLogService auditLogService;          // ← Interface
// AttendanceService belum di-refactor ke interface
```

### 4.4 Encapsulation

```java
// AuditLog — semua setter package-private (tanpa public)
class AuditLog {
    private String actionType;   // ← private field
    // ...
    void setActionType(String v) { this.actionType = v; }  // ← package-private (ENCAPSULATION)
}

// Hanya AuditLogFactory (dalam package yang sama) yang bisa akses setter
class AuditLogFactory {
    public static AuditLog create(User actor, String actionType, ...) {
        AuditLog log = new AuditLog();
        log.setActor(actor);          // ← bisa akses package-private setter
        // ...
    }
}
```

---

## 5. Database Schema

```sql
-- Notifications
CREATE TABLE notifications (
    notification_id VARCHAR(255) PRIMARY KEY,
    recipient_id VARCHAR(255) NOT NULL REFERENCES users(user_id),
    report_id VARCHAR(255) REFERENCES reports(report_id),
    title VARCHAR(200) NOT NULL,
    message_text TEXT NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(255),
    notification_type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Audit Logs
CREATE TABLE audit_logs (
    log_id VARCHAR(255) PRIMARY KEY,
    actor_id VARCHAR(255) REFERENCES users(user_id),
    report_id VARCHAR(255) REFERENCES reports(report_id),
    target_type VARCHAR(255),
    target_id VARCHAR(255),
    action_type VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    device_info VARCHAR(255),
    logged_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Officer Attendance
CREATE TABLE officer_attendance (
    attendance_id VARCHAR(255) PRIMARY KEY,
    officer_id VARCHAR(255) NOT NULL REFERENCES users(user_id),
    check_in_at TIMESTAMP NOT NULL,
    check_in_latitude DECIMAL(10,8),
    check_in_longitude DECIMAL(11,8),
    check_out_at TIMESTAMP,
    device_info VARCHAR(255),
    shift_status VARCHAR(50) NOT NULL
);
```
