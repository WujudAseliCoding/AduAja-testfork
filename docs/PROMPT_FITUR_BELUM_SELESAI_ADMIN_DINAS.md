# Prompt Pelengkap Fitur Admin Dinas AduAja

## Ringkasan Masalah

Berdasarkan analisis terhadap SRS_content.txt dan seluruh kode sumber, ditemukan **5 (lima) masalah utama** pada modul Admin Dinas yang belum diimplementasi sesuai spesifikasi. Berikut prompt/instruksi untuk melengkapi setiap fitur.

---

## Prompt 1: Deskripsi/Instruksi dari Admin Pusat Tidak Masuk ke Laporan Admin Dinas

### Masalah
Form disposisi detail (`admin/disposisi-detail.html`) mengirim field bernama `instructions`, `priority`, dan `deadline` tetapi **method** `AdminPusatController.adminDisposisiPost()` di `AdminPusatController.java:621-647` hanya membaca `id`, `dinasId`, dan `catatan`. Tiga field lainnya (`instructions`, `priority`, `deadline`) diabaikan.

### Akar Masalah
1. `AdminPusatController.java:621-647` method `adminDisposisiPost()` tidak mendeklarasikan parameter `@RequestParam(value = "priority")`, `@RequestParam(value = "deadline")`, atau `@RequestParam(value = "instructions")`.
2. `Disposition.java` (entity) tidak memiliki field `priority` atau `deadline`.
3. `DispositionDTO.java` tidak memiliki field untuk priority/deadline/instructions.
4. `DispositionService.createDisposition()` hanya menerima `notes` (string biasa), bukan priority/deadline.

### Yang Harus Diperbaiki

#### a. Entity `Disposition.java`
Tambah field berikut:
```java
@Column(name = "priority", length = 20)
private String priority; // "Rendah", "Sedang", "Tinggi", "Kritis"

@Column(name = "deadline")
private LocalDateTime deadline; // deadline dari admin pusat

@Column(name = "instructions", columnDefinition = "TEXT")
private String instructions; // instruksi detail dari admin pusat (pisah dari notes)
```

#### b. DTO `DispositionDTO.java`
Tambah field `priority`, `deadline`, `instructions` dan getter/setter-nya.

#### c. Service `DispositionService.java` (interface) & `DispositionServiceImpl.java`
Ubah method `createDisposition()` atau buat overload baru yang menerima priority, deadline, dan instructions:
```java
Disposition createDisposition(String reportId, String dispatchedById,
    String targetAgencyId, String priority, LocalDateTime deadline,
    String instructions, String notes);
```

#### d. Controller `AdminPusatController.java`
Di method `adminDisposisiPost()` (baris 621), tambah parameter:
```java
@RequestParam(value = "priority", required = false) String priority,
@RequestParam(value = "deadline", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
@RequestParam(value = "instructions", required = false) String instructions
```
Teruskan nilai-nilai ini ke `dispositionService.createDisposition()`.

#### e. Form `disposisi-detail.html`
Pastikan field `name` di form sesuai dengan parameter controller:
- `name="priority"` ✓ (sudah sesuai)
- `name="deadline"` ✓ (sudah sesuai)
- `name="instructions"` untuk instruksi (bukan `catatan`)

#### f. Tidak perlu ubah `disposisi-panel.html` (form sederhana), biarkan tetap menggunakan field `catatan`.

---

## Prompt 2: Tingkat Prioritas dari Admin Pusat Tidak Tersimpan

### Masalah
Di `disposisi-detail.html` baris 144-213, admin pusat bisa memilih prioritas (Rendah/Sedang/Tinggi/Kritis), tapi data ini **tidak pernah disimpan** ke database karena:
1. `Disposition` entity tidak punya field `priority`
2. Controller tidak membaca parameter `priority`

### SRS Reference
Tidak ada FR spesifik yang menyebut "admin pusat menentukan prioritas", tetapi SRS 3.14 (FR-DSP-01 s/d 06) tentang disposisi menyebutkan perlunya informasi yang memadai saat meneruskan laporan ke dinas. Prioritas adalah informasi esensial.

### Yang Harus Diperbaiki
(Sama dengan Prompt 1 point a, c, d — menambah field `priority` ke entity, service, dan controller)

#### Selain itu, di `AdminDinasController.java`:
Pada method `adminDinasPenugasan()` (baris 159-221), baca priority dari `Disposition` entity (bukan hardcoded "Sedang"):
```java
// Cari disposition untuk report ini
Disposition disp = dispositionService.getDispositionByReportId(reportId).orElse(null);
String priority = disp != null && disp.getPriority() != null ? disp.getPriority() : "Sedang";
m.put("prioritas", priority);
```

#### Di `adminDinasQueue()` juga sama:
```java
m.put("prioritas", "Sedang"); // HARUS diganti dengan baca dari disposition
```

---

## Prompt 3: Deadline dari Admin Pusat Tidak Tersimpan

### Masalah
- `disposisi-detail.html` baris 215-238 memiliki input `datetime-local` untuk deadline.
- Nilai deadline tidak pernah dibaca atau disimpan.
- Di `AdminDinasController.adminDinasPenugasan()`, `m.put("deadline", "-")` selalu hardcoded "-".

### SRS Reference
**FR-SLA-01** (SRS 3.16): Sistem HARUS menampilkan informasi target batas waktu maksimal pada setiap laporan.
**FR-SLA-02**: Sistem HARUS menampilkan "Tanggal Dikirim".
**FR-SLA-03**: Sistem HARUS menampilkan perhitungan otomatis "Target Selesai".

### Yang Harus Diperbaiki

#### a. Entity, Service, Controller (sama dengan Prompt 1)
Tambah field `deadline` (LocalDateTime) ke `Disposition`.

#### b. `AdminDinasController.java`
Di method `adminDinasPenugasan()`:
```java
// Baca deadline dari disposition
LocalDateTime deadline = disp != null && disp.getDeadline() != null
    ? disp.getDeadline() : null;
m.put("deadline", deadline != null ? deadline.format(ControllerHelper.DATETIME_FMT) : "-");
m.put("deadlineDate", deadline != null ? deadline.toLocalDate().toString() : "-");
```

Di method `adminDinasQueue()`:
```java
// Tampilkan deadline dan sisa waktu
m.put("deadline", deadline != null ? deadline.format(ControllerHelper.DATE_FMT) : "-");
m.put("sisaWaktu", deadline != null ? hitungSisaWaktu(deadline) : "-");
```

#### c. Template `penugasan-petugas.html`
Sudah ada `th:text="|Deadline: ${report['deadline']}|"` di baris 103 — ini akan otomatis terisi jika data deadline dikirim.

---

## Prompt 4: Sistem Peringatan Wilayah Petugas Tidak Sesuai Lokasi Laporan (FR-PRS-03)

### Masalah
- `penugasan-petugas.html` baris 386-401 hanya menampilkan **teks statis** tentang FR-PRS-03, bukan validasi aktif.
- Tidak ada peringatan JavaScript/client-side saat petugas yang dipilih tidak sesuai wilayah.
- Tidak ada validasi server-side.
- Petugas tidak memiliki data `wilayahTugas` (selalu "-").

### SRS Reference
**FR-PRS-03** (SRS 3.15): Perangkat lunak AKAN memunculkan pesan peringatan pencegahan apabila Administrator mencoba mengalokasikan tugas ke Penanggung Jawab Lapangan yang wilayah tugasnya tidak sesuai dengan lokasi laporan.

### Yang Harus Diperbaiki

#### a. Entity `User` / `UserProfile`
Tambah field untuk wilayah tugas petugas:
```java
// Di UserProfile.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "wilayah_tugas_region_id")
private Region wilayahTugas; // wilayah tugas petugas

@Column(name = "nip", length = 30)
private String nip; // Nomor Induk Pegawai (pindah dari UserProfile jika perlu)
```

#### b. DTO `CreatePetugasDTO.java`
Tambah field:
```java
private String nip;
private String wilayahTugasRegionId; // region id untuk wilayah tugas
```

#### c. Service `UserServiceImpl.java`
Di `createPetugas()`, simpan NIP dan wilayah tugas ke `UserProfile`.

#### d. Controller `AdminDinasController.java`
Di method yang menampilkan daftar petugas, baca NIP dan wilayah tugas dari `UserProfile`:
```java
m.put("nip", p.getUserProfile() != null && p.getUserProfile().getNip() != null
    ? p.getUserProfile().getNip() : "-");
m.put("wilayahTugas", p.getUserProfile() != null && p.getUserProfile().getWilayahTugas() != null
    ? p.getUserProfile().getWilayahTugas().getRegionName() : "-");
```

#### e. Validasi Peringatan (Client-side + Server-side)

**Client-side (JavaScript di `penugasan-petugas.html`):**
```javascript
// Saat form akan disubmit, cek wilayah petugas vs lokasi laporan
document.querySelector('form').addEventListener('submit', function(e) {
    const selectedPetugas = document.querySelector('input[name="petugasId"]:checked');
    if (selectedPetugas) {
        const label = selectedPetugas.closest('label');
        const wilayahPetugas = label.dataset.wilayah; // dari th:data-wilayah
        const wilayahLaporan = document.getElementById('lokasi-laporan').value;
        // Bandingkan (sederhana) atau tampilkan peringatan
        if (wilayahPetugas && wilayahLaporan && wilayahPetugas !== wilayahLaporan) {
            if (!confirm('PERINGATAN: Wilayah tugas petugas (' + wilayahPetugas +
                ') tidak sesuai dengan lokasi laporan (' + wilayahLaporan +
                '). Apakah Anda yakin ingin tetap menugaskan petugas ini?')) {
                e.preventDefault();
            }
        }
    }
});
```

**Server-side (di `FieldTaskServiceImpl.createTask()`):**
```java
// Validasi wilayah sebelum membuat task
Report report = reportRepository.findById(reportId).orElseThrow(...);
User officer = userRepository.findById(officerId).orElseThrow(...);
Region wilayahLaporan = report.getRegion();
Region wilayahPetugas = officer.getUserProfile() != null
    ? officer.getUserProfile().getWilayahTugas() : null;

if (wilayahLaporan != null && wilayahPetugas != null
    && !wilayahLaporan.getRegionId().equals(wilayahPetugas.getRegionId())) {
    // Log peringatan atau lempar exception
    log.warn("Wilayah petugas {} tidak sesuai dengan lokasi laporan {}", officerId, reportId);
    // Bisa throw exception atau return warning
}
```

#### f. Template `penugasan-petugas.html`
Hapus kotak teks statis FR-PRS-03 (baris 386-401) dan ganti dengan div peringatan dinamis yang muncul hanya saat ketidaksesuaian terdeteksi:
```html
<div id="wilayah-warning" class="hidden bg-red-50 border border-red-200 rounded-lg p-4">
  <div class="flex items-start gap-3">
    <i data-lucide="alert-triangle" class="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0"></i>
    <div class="text-sm text-red-800">
      <strong>Peringatan Wilayah (FR-PRS-03):</strong>
      Wilayah tugas petugas <span id="p-nama"></span> (<span id="p-wilayah"></span>)
      tidak sesuai dengan lokasi laporan (<span id="l-wilayah"></span>).
    </div>
  </div>
</div>
```

---

## Prompt 5: Lokasi dan NIP Petugas Tidak Ditampilkan di Daftar Petugas

### Masalah
- Di `AdminDinasController.java` baris 86-96 (dashboard) dan baris 190-201 (penugasan), `nip` selalu "-" dan `wilayahTugas` selalu "-".
- `CreatePetugasDTO.java` hanya punya `fullName`, `email`, `phoneNumber`, `password` — tidak ada `nip` atau `wilayahTugas`.
- `petugas.html` (tabel daftar petugas, baris 79-99) tidak menampilkan kolom NIP dan Wilayah Tugas.

### Yang Harus Diperbaiki

#### a. `UserProfile.java` + `User.java`
Tambah field `nip` (String) di `UserProfile`.
Tambah relasi `wilayahTugas` ke `Region` di `UserProfile`.
*(Lihat Prompt 4 point a)*

#### b. `CreatePetugasDTO.java`
```java
private String nip;
private String wilayahTugasRegionId;
// + getter/setter
```

#### c. `AdminDinasController.java`
Di method `adminDinasPenugasan()` dan `adminDinasDashboard()`, ubah mapping petugas:
```java
m.put("nip", p.getUserProfile() != null && p.getUserProfile().getNip() != null
    ? p.getUserProfile().getNip() : "-");
m.put("wilayahTugas", p.getUserProfile() != null && p.getUserProfile().getWilayahTugas() != null
    ? p.getUserProfile().getWilayahTugas().getRegionName() : "-");
```

#### d. `admin/dinas/petugas.html`
Tambah kolom NIP dan Wilayah Tugas di tabel daftar petugas (baris 82-86):
```html
<th class="pb-3 font-medium">NIP</th>
<th class="pb-3 font-medium">Wilayah Tugas</th>
```
Dan di baris data (sekitar baris 89-96):
```html
<td class="py-3 text-gray-600" th:text="${p.userProfile != null && p.userProfile.nip != null ? p.userProfile.nip : '-'}">NIP</td>
<td class="py-3 text-gray-600" th:text="${p.userProfile != null && p.userProfile.wilayahTugas != null ? p.userProfile.wilayahTugas.regionName : '-'}">Wilayah</td>
```

#### e. Form Tambah Petugas (`petugas.html` baris 39-68)
Tambah field input untuk NIP dan Wilayah Tugas:
```html
<div>
  <label class="block text-sm font-medium text-gray-700 mb-1">NIP</label>
  <input type="text" name="nip"
         class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500">
</div>
<div>
  <label class="block text-sm font-medium text-gray-700 mb-1">Wilayah Tugas</label>
  <select name="wilayahTugasRegionId"
          class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500">
    <option value="">-- Pilih Wilayah --</option>
    <option th:each="r : ${regions}" th:value="${r.regionId}" th:text="${r.regionName}"></option>
  </select>
</div>
```

#### f. Controller `AdminDinasController.java`
Di `adminDinasPetugas()`, tambah daftar region ke model:
```java
@Autowired private com.plr.aduaja.service.RegionService regionService;
// ... di method:
model.addAttribute("regions", regionService.getAllRegions());
```

---

## Ringkasan File yang Perlu Diubah

| File | Prompt Terkait |
|------|----------------|
| `model/Disposition.java` | 1, 2, 3 |
| `model/UserProfile.java` | 4, 5 |
| `dto/DispositionDTO.java` | 1 |
| `dto/CreatePetugasDTO.java` | 4, 5 |
| `service/DispositionService.java` | 1 |
| `service/DispositionServiceImpl.java` | 1, 2, 3 |
| `service/UserServiceImpl.java` | 4, 5 |
| `service/FieldTaskServiceImpl.java` | 4 |
| `controller/AdminPusatController.java` | 1, 2, 3 |
| `controller/AdminDinasController.java` | 2, 3, 4, 5 |
| `templates/admin/disposisi-detail.html` | 1 |
| `templates/admin/dinas/penugasan-petugas.html` | 4 |
| `templates/admin/dinas/petugas.html` | 4, 5 |
| `templates/admin/dinas/dinas-dashboard.html` | 2 (opsional) |
| `templates/admin/dinas/dinas-queue.html` | 3 (opsional) |

---

## Urutan Implementasi yang Direkomendasikan

1. **Phase 1 - Data Model** (Prompt 1a, 4a, 5a): Tambah field ke entity (`Disposition`, `UserProfile`)
2. **Phase 2 - DTO & Service** (Prompt 1b, 1c, 4b, 5b): Update DTO dan service layer
3. **Phase 3 - Controller** (Prompt 1d, 2, 3, 4d, 5c, 5f): Update controller untuk membaca/menyimpan data baru
4. **Phase 4 - Templates** (Prompt 1e, 4e, 4f, 5d, 5e): Update HTML templates
5. **Phase 5 - Validasi** (Prompt 4e, 6): Implementasi peringatan wilayah FR-PRS-03 dan status ketersediaan dinamis
6. **Phase 6 - Jeda Waktu** (Prompt 7): Implementasi fitur pause/resume SLA

---

## Prompt 6: Status Ketersediaan Petugas Masih di-Hardcode (FR-PRS-02)

### Masalah
Di `AdminDinasController.java`, method `adminDinasDashboard()` (baris 86-96) dan `adminDinasPenugasan()` (baris 190-201), **status ketersediaan petugas selalu di-hardcode** menjadi `"Tersedia"` tanpa pengecekan kondisi riil di lapangan. Akibatnya, Admin Dinas dapat menugaskan petugas yang sedang tidak aktif, sedang istirahat, atau sedang memiliki tugas aktif — melanggar FR-PRS-02.

### SRS Reference
**FR-PRS-02** (SRS 3.15 baris 8982-9034): Perangkat lunak AKAN menampilkan status ketersediaan operasional petugas lapangan ("Siap Bertugas", "Sedang Bertugas", "Istirahat", atau "Selesai Shift") untuk mencegah alokasi tugas kepada personel yang tidak tersedia.

### Status Ketersediaan yang Harus Diterapkan

| Status SRS | Kondisi Logika |
|------------|----------------|
| `"Siap Bertugas"` | Petugas sudah **Check-In** hari ini (shift AKTIF) DAN **tidak memiliki** tugas berstatus `SEDANG_DIKERJAKAN` |
| `"Sedang Bertugas"` | Petugas sudah **Check-In** hari ini (shift AKTIF) DAN **memiliki** tugas berstatus `SEDANG_DIKERJAKAN` |
| `"Istirahat"` | Petugas sudah **Check-In** hari ini dengan shift status `ISTIRAHAT` |
| `"Selesai Shift"` | Petugas sudah **Check-Out** (shift status `SELESAI_SHIFT`) atau **belum Check-In** sama sekali hari ini |

### Yang Harus Diperbaiki

#### a. Logic Helper — Buat method untuk menentukan status ketersediaan

Di `AdminDinasController.java` (atau di `ControllerHelper.java` agar reusable), buat method:

```java
import com.plr.aduaja.model.OfficerAttendance;
import com.plr.aduaja.model.OfficerAttendance.ShiftStatus;
import com.plr.aduaja.service.AttendanceService;

private String determinePetugasStatus(User petugas, AttendanceService attendanceService, FieldTaskService fieldTaskService) {
    // 1. Cek apakah petugas sudah check-in hari ini
    Optional<OfficerAttendance> currentShift = attendanceService.getCurrentShift(petugas.getUserId());

    if (currentShift.isEmpty()) {
        // Tidak ada shift aktif / belum check-in
        return "Selesai Shift";
    }

    OfficerAttendance shift = currentShift.get();
    ShiftStatus shiftStatus = shift.getShiftStatus();

    // 2. Cek status shift
    switch (shiftStatus) {
        case ISTIRAHAT:
            return "Istirahat";
        case SELESAI_SHIFT:
            return "Selesai Shift";
        case AKTIF:
            // 3. Cek apakah petugas sedang memiliki tugas aktif
            List<FieldTask> activeTasks = fieldTaskService.getTasksByOfficerAndStatus(
                    petugas.getUserId(), FieldTask.TaskStatus.SEDANG_DIKERJAKAN);
            if (activeTasks != null && !activeTasks.isEmpty()) {
                return "Sedang Bertugas";
            }
            return "Siap Bertugas";
        default:
            return "Siap Bertugas";
    }
}
```

#### b. `AdminDinasController.java` — `adminDinasDashboard()` (baris 85-97)

Ubah mapping petugas dari hardcode menjadi dinamis:

```java
@Autowired
private AttendanceService attendanceService;

// ... di dalam method adminDinasDashboard():
List<User> realPetugas = userService.findByRole(User.Role.PETUGAS);
List<Map<String, Object>> petugasList = realPetugas.stream().map(p -> {
    Map<String, Object> m = new HashMap<>();
    m.put("id", p.getUserId());
    m.put("nama", p.getFullName());
    // BACA NIP dari UserProfile (real, bukan hardcode "-")
    m.put("nip", p.getUserProfile() != null && p.getUserProfile().getNip() != null
            ? p.getUserProfile().getNip() : "-");
    // STATUS DINAMIS berdasarkan absensi dan tugas aktif
    m.put("statusKetersediaan", determinePetugasStatus(p, attendanceService, fieldTaskService));
    // BACA wilayah tugas dari UserProfile
    m.put("wilayahTugas", p.getUserProfile() != null && p.getUserProfile().getWilayahTugas() != null
            ? p.getUserProfile().getWilayahTugas().getRegionName() : "-");
    m.put("tugasAktif", (int) fieldTaskService.getTasksByOfficerAndStatus(
            p.getUserId(), FieldTask.TaskStatus.SEDANG_DIKERJAKAN).size());
    m.put("kontak", p.getEmail());
    return m;
}).collect(Collectors.toList());
model.addAttribute("availablePetugas", petugasList.isEmpty() ? new ArrayList<>() : petugasList);
```

#### c. `AdminDinasController.java` — `adminDinasPenugasan()` (baris 190-201)

Terapkan perubahan yang sama persis seperti point b di atas untuk blok mapping petugas yang ada di method ini.

#### d. Template `penugasan-petugas.html` — Warna Badge Ketersediaan

Update logika pewarnaan badge status ketersediaan (baris 347-351) agar menangani semua status SRS:

```html
<span
  class="text-xs px-3 py-1 rounded-full font-semibold"
  th:classappend="${petugas['statusKetersediaan'] == 'Siap Bertugas'} ? 'bg-green-100 text-green-700' : 
                  (${petugas['statusKetersediaan'] == 'Sedang Bertugas'} ? 'bg-blue-100 text-blue-700' : 
                  (${petugas['statusKetersediaan'] == 'Istirahat'} ? 'bg-yellow-100 text-yellow-700' : 
                  'bg-gray-100 text-gray-700'))"
  th:text="${petugas['statusKetersediaan']}"
></span>
```

#### e. Template `penugasan-petugas.html` — Disable otomatis petugas tidak tersedia

Perbaiki kondisi `th:disabled` di baris 332 agar petugas dengan status selain "Siap Bertugas" dan "Sedang Bertugas" tidak bisa dipilih:

```html
<input
  type="radio"
  name="petugasId"
  th:value="${petugas['id']}"
  class="mt-1 w-4 h-4 text-blue-600"
  th:disabled="${petugas['statusKetersediaan'] != 'Siap Bertugas' and petugas['statusKetersediaan'] != 'Sedang Bertugas'}"
  required
/>
```

> **Catatan**: Petugas dengan status "Sedang Bertugas" tetap bisa dipilih untuk penugasan baru (tugas dapat dialokasikan ke petugas yang sama), tetapi sistem harus memberi peringatan. Opsional: setel `th:disabled` hanya untuk `"Istirahat"` dan `"Selesai Shift"`.

#### f. Template `penugasan-petugas.html` — Tooltip/Peringatan untuk status tidak siap

Tambahkan elemen penjelasan singkat di samping badge status agar Admin Dinas memahami mengapa petugas tidak dapat dipilih:

```html
<span th:if="${petugas['statusKetersediaan'] == 'Istirahat' or petugas['statusKetersediaan'] == 'Selesai Shift'}"
      class="text-xs text-red-500 ml-2">
  (Tidak tersedia)
</span>
```

#### g. Pastikan `AttendanceService` sudah di-inject

Di `AdminDinasController.java`, tambahkan:

```java
@Autowired
private AttendanceService attendanceService;
```

---

## Prompt 7: Fitur Jeda Waktu SLA (FR-JDA-01 s/d FR-JDA-05) Belum Ada Endpoint dan UI untuk Admin Dinas

### Masalah
1. **Tidak ada endpoint khusus** di `AdminDinasController` untuk menjeda (pause) atau melanjutkan (resume) waktu SLA dari sisi Admin Dinas.
2. **Tidak ada UI/Modal** di template `progress-update.html` untuk input alasan jeda waktu.
3. **Method `postponeTask()`** di `FieldTaskServiceImpl.java` (baris 151-166) hanya membuat record `TaskPostponement` dan mengubah status task, tetapi **tidak mengubah status SLA** (`SlaRecord`) menjadi `TERTUNDA` — sehingga hitung mundur SLA tetap berjalan.
4. **Method `pauseSla()`** di `SlaRecordServiceImpl.java` (baris 83-88) mengubah status SLA menjadi `TERTUNDA` tetapi **tidak mencatat log** ke `SlaPauseLog` dan **tidak menyimpan timestamp pause**.
5. **Method `resumeSla()`** di `SlaRecordServiceImpl.java` (baris 91-97) mengembalikan status SLA ke `BERJALAN` tetapi **tidak menghitung durasi jeda** dan **tidak menyesuaikan `slaDeadlineAt`** (tidak menambahkan waktu jeda ke deadline).

### SRS Reference
- **FR-JDA-01** (SRS 3.17): Sistem AKAN menyediakan fungsionalitas penghentian waktu (Jeda) bagi Admin Dinas dan akan merespons notifikasi apabila terdapat pengajuan penundaan dari Petugas.
- **FR-JDA-02**: Sistem AKAN mewajibkan Admin untuk memasukkan keterangan teks alasan penundaan saat Jeda Waktu diaktifkan.
- **FR-JDA-03**: Sistem AKAN menyelaraskan status waktu laporan menjadi "Tertunda" secara global dan menghentikan sementara hitung mundur SLA.
- **FR-JDA-04**: Sistem AKAN menyediakan fungsionalitas pemulihan waktu (Lanjutkan Waktu) yang hanya dapat diakses pada laporan berstatus "Tertunda".
- **FR-JDA-05**: Sistem AKAN melanjutkan kembali hitung mundur SLA dan otomatis mengubah status laporan menjadi "Sedang Berjalan" setelah pemulihan waktu dikonfirmasi.

### Yang Harus Diperbaiki

#### a. Service `SlaRecordServiceImpl.java` — Perbaiki `pauseSla()` agar mencatat SlaPauseLog

```java
@Autowired
private SlaPauseLogRepository slaPauseLogRepository;

@Override
@Transactional
public SlaRecord pauseSla(String slaId, String reason, String pausedByUserId) {
    SlaRecord sla = slaRecordRepository.findById(slaId)
            .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan: " + slaId));

    // Ubah status SLA menjadi TERTUNDA
    sla.setCurrentStatus(SlaStatus.TERTUNDA);
    slaRecordRepository.save(sla);

    // Buat log pause
    User pausedBy = userRepository.findById(pausedByUserId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan: " + pausedByUserId));

    SlaPauseLog log = new SlaPauseLog();
    log.setSlaRecord(sla);
    log.setPausedBy(pausedBy);
    log.setPauseReason(reason);
    log.setPausedAt(LocalDateTime.now());
    // resumed_at dan paused_duration_minutes akan diisi saat resume
    slaPauseLogRepository.save(log);

    return sla;
}
```

> **Catatan**: Interface `SlaRecordService` juga perlu diubah — tambahkan method `pauseSla(String slaId, String reason, String pausedByUserId)` sebagai overload baru (jangan hapus yang lama, gunakan overloading).

#### b. Service `SlaRecordServiceImpl.java` — Perbaiki `resumeSla()` agar menghitung jeda dan menyesuaikan deadline

```java
@Override
@Transactional
public SlaRecord resumeSla(String slaId) {
    SlaRecord sla = slaRecordRepository.findById(slaId)
            .orElseThrow(() -> new RuntimeException("SLA tidak ditemukan: " + slaId));

    // Cari log pause yang belum di-resume (paling terbaru)
    List<SlaPauseLog> pauseLogs = slaPauseLogRepository
            .findBySlaRecordSlaIdOrderByPausedAtDesc(slaId);

    if (!pauseLogs.isEmpty()) {
        SlaPauseLog latestPause = pauseLogs.get(0);
        if (latestPause.getResumedAt() == null) {
            // Hitung durasi jeda dalam menit
            LocalDateTime now = LocalDateTime.now();
            long durationMinutes = java.time.Duration.between(latestPause.getPausedAt(), now).toMinutes();

            // Simpan durasi jeda
            latestPause.setResumedAt(now);
            latestPause.setPausedDurationMinutes((int) durationMinutes);
            slaPauseLogRepository.save(latestPause);

            // Update total waktu jeda di SlaRecord
            int totalPaused = (sla.getTotalPausedMinutes() != null ? sla.getTotalPausedMinutes() : 0)
                    + (int) durationMinutes;
            sla.setTotalPausedMinutes(totalPaused);

            // Sesuaikan deadline: tambahkan durasi jeda ke deadline
            sla.setSlaDeadlineAt(sla.getSlaDeadlineAt().plusMinutes(durationMinutes));
        }
    }

    // Kembalikan status SLA ke BERJALAN
    sla.setCurrentStatus(SlaStatus.BERJALAN);
    return slaRecordRepository.save(sla);
}
```

#### c. Service `FieldTaskServiceImpl.java` — Perbaiki `postponeTask()` agar juga pause SLA

Di method `postponeTask()` (baris 151-166), tambahkan logika untuk pause SLA:

```java
@Autowired
private SlaRecordService slaRecordService;

@Override
public FieldTask postponeTask(String taskId, String reason) {
    FieldTask task = fieldTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
    task.setTaskStatus(TaskStatus.TERTUNDA);
    fieldTaskRepository.save(task);

    // Simpan record penundaan
    TaskPostponement postponement = new TaskPostponement();
    postponement.setTask(task);
    postponement.setReason(reason != null && !reason.isBlank() ? reason : "Ditunda oleh petugas");
    postponement.setRequestedAt(LocalDateTime.now());
    postponement.setApprovalStatus(TaskPostponement.ApprovalStatus.MENUNGGU);
    taskPostponementRepository.save(postponement);

    // FIX: Jeda SLA juga — set status SLA menjadi TERTUNDA
    if (task.getSlaRecord() != null) {
        slaRecordService.pauseSla(task.getSlaRecord().getSlaId(), reason);
    }

    return task;
}
```

#### d. Buat method `resumeTask()` di `FieldTaskService.java` (interface) & `FieldTaskServiceImpl.java`

**Interface `FieldTaskService.java`:**
```java
FieldTask resumeTask(String taskId);
```

**Implementasi `FieldTaskServiceImpl.java`:**
```java
@Override
public FieldTask resumeTask(String taskId) {
    FieldTask task = fieldTaskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
    task.setTaskStatus(TaskStatus.SEDANG_DIKERJAKAN);
    fieldTaskRepository.save(task);

    // Resume SLA — hitung jeda dan sesuaikan deadline
    if (task.getSlaRecord() != null) {
        slaRecordService.resumeSla(task.getSlaRecord().getSlaId());
    }

    return task;
}
```

#### e. Controller `AdminDinasController.java` — Tambah endpoint pause dan resume

```java
// ==========================================
// ADMIN DINAS — JEDA WAKTU SLA (FR-JDA-01 s/d 05)
// ==========================================

@PostMapping("/admin/dinas/task/pause")
public String adminDinasTaskPause(
        @RequestParam(value = "taskId", required = false) String taskId,
        @RequestParam(value = "alasan", required = false) String alasan,
        HttpSession session
) {
    // SESSION CHECK
    if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

    if (taskId == null || taskId.trim().isEmpty() || alasan == null || alasan.trim().length() < 5) {
        return "redirect:/admin/dinas/progress?error=Alasan minimal 5 karakter";
    }

    try {
        // FR-JDA-02: Wajib memasukkan alasan teks
        fieldTaskService.postponeTask(taskId, alasan.trim());
        return "redirect:/admin/dinas/progress?paused=true";
    } catch (Exception e) {
        log.error("Gagal pause task {}: {}", taskId, e.getMessage(), e);
        return "redirect:/admin/dinas/progress?error=" + e.getMessage();
    }
}

@PostMapping("/admin/dinas/task/resume")
public String adminDinasTaskResume(
        @RequestParam(value = "taskId", required = false) String taskId,
        HttpSession session
) {
    // SESSION CHECK
    if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

    if (taskId == null || taskId.trim().isEmpty()) {
        return "redirect:/admin/dinas/progress";
    }

    try {
        // FR-JDA-04 & FR-JDA-05: Resume waktu dan lanjutkan hitung mundur SLA
        fieldTaskService.resumeTask(taskId);
        return "redirect:/admin/dinas/progress?resumed=true";
    } catch (Exception e) {
        log.error("Gagal resume task {}: {}", taskId, e.getMessage(), e);
        return "redirect:/admin/dinas/progress?error=" + e.getMessage();
    }
}
```

#### f. Template `progress-update.html` — Tambah tombol Pause dan Modal Form Alasan

Di dalam section sebelah kanan (detail tiket terpilih), setelah form update progress yang sudah ada (atau sebelum tombol submit), tambahkan:

```html
<!-- FR-JDA-01 s/d FR-JDA-05: Jeda Waktu SLA -->
<div th:if="${selectedTicket != null}" class="border-t border-gray-200 pt-6 mt-6">
  <div class="bg-amber-50 border border-amber-200 rounded-lg p-4">
    <div class="flex items-start gap-3">
      <i data-lucide="clock" class="w-5 h-5 text-amber-600 mt-0.5 flex-shrink-0"></i>
      <div class="flex-1">
        <p class="text-sm font-semibold text-amber-900 mb-1">
          Kendali Waktu Operasional (FR-JDA)
        </p>
        <p class="text-xs text-amber-800 mb-3">
          Jeda waktu operasional jika terdapat kendala di lapangan.
          Status tiket akan berubah menjadi "Tertunda" dan hitung mundur SLA akan dijeda.
        </p>

        <!-- Tombol Pause -->
        <button
          x-on:click="openPauseModal = true"
          class="inline-flex items-center gap-2 bg-amber-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-amber-700"
        >
          <i data-lucide="pause-circle" class="w-4 h-4"></i>
          Jeda Waktu
        </button>

        <!-- Tombol Resume (hanya tampil jika status task adalah TERTUNDA) -->
        <form th:action="@{/admin/dinas/task/resume}" method="post" class="inline">
          <input type="hidden" name="taskId" th:value="${selectedTicket.id}" />
          <button
            type="submit"
            class="inline-flex items-center gap-2 bg-green-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-green-700"
            th:if="${selectedTicket.statusTask == 'TERTUNDA'}"
          >
            <i data-lucide="play-circle" class="w-4 h-4"></i>
            Lanjutkan Waktu
          </button>
        </form>
      </div>
    </div>
  </div>
</div>

<!-- Modal Form Pause -->
<div
  x-show="openPauseModal"
  x-cloak
  class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
  x-on:keydown.escape.window="openPauseModal = false"
>
  <div class="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6" x-on:click.stop>
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-gray-900 flex items-center gap-2">
        <i data-lucide="pause-circle" class="w-5 h-5 text-amber-600"></i>
        Jeda Waktu Operasional
      </h3>
      <button x-on:click="openPauseModal = false" class="text-gray-400 hover:text-gray-600">
        <i data-lucide="x" class="w-5 h-5"></i>
      </button>
    </div>

    <!-- FR-JDA-02: Wajib masukkan alasan -->
    <form th:action="@{/admin/dinas/task/pause}" method="post" class="space-y-4">
      <input type="hidden" name="taskId" th:value="${selectedTicket.id}" />

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Alasan Penundaan <span class="text-red-500">*</span>
        </label>
        <textarea
          name="alasan"
          rows="4"
          class="w-full px-3 py-2 border border-gray-300 rounded-md"
          placeholder="Jelaskan alasan penundaan pekerjaan di lapangan (minimal 5 karakter)..."
          minlength="5"
          required
        ></textarea>
        <p class="text-xs text-gray-500 mt-1">Minimal 5 karakter (FR-JDA-02)</p>
      </div>

      <div class="bg-blue-50 border border-blue-200 rounded-lg p-3">
        <div class="flex items-start gap-2">
          <i data-lucide="info" class="w-4 h-4 text-blue-600 mt-0.5 flex-shrink-0"></i>
          <div class="text-xs text-blue-800">
            <strong>FR-JDA-03:</strong> Status tiket akan berubah menjadi "Tertunda"
            dan hitung mundur SLA akan dijeda sementara.<br />
            <strong>FR-JDA-04/05:</strong> Gunakan tombol "Lanjutkan Waktu" untuk
            melanjutkan kembali hitung mundur SLA.
          </div>
        </div>
      </div>

      <div class="flex gap-3 pt-2">
        <button
          type="submit"
          class="flex-1 bg-amber-600 text-white py-2 rounded-lg font-medium hover:bg-amber-700"
        >
          Konfirmasi Jeda
        </button>
        <button
          type="button"
          x-on:click="openPauseModal = false"
          class="flex-1 border border-gray-300 py-2 rounded-lg font-medium hover:bg-gray-50"
        >
          Batal
        </button>
      </div>
    </form>
  </div>
</div>
```

#### g. Controller `AdminDinasController.java` — `adminDinasProgress()` — Kirim status task (TERTUNDA/berjalan) ke view

Di method `adminDinasProgress()` (baris 253-294), saat membangun `ticketsInProgress`, tambahkan field `statusTask`:

```java
m.put("statusTask", t.getTaskStatus() != null ? t.getTaskStatus().name() : "BARU");
m.put("statusLabel", t.getTaskStatus() == TaskStatus.TERTUNDA ? "Tertunda" : "Dalam Penanganan");
```

Juga, tambahkan data SLA dan pause log ke model agar admin bisa melihat riwayat jeda:

```java
// Ambil data SLA untuk task ini
if (t.getSlaRecord() != null) {
    m.put("slaStatus", t.getSlaRecord().getCurrentStatus().name());
    m.put("slaDeadline", t.getSlaRecord().getSlaDeadlineAt() != null
            ? t.getSlaRecord().getSlaDeadlineAt().format(ControllerHelper.DATETIME_FMT) : "-");
    m.put("totalPausedMinutes", t.getSlaRecord().getTotalPausedMinutes() != null
            ? t.getSlaRecord().getTotalPausedMinutes() : 0);
}
```

#### h. Template `progress-update.html` — Tambah state `x-data` untuk mengelola modal

Di elemen `<div layout:fragment="content" ...>` yang sudah ada, ubah atau tambahkan atribut `x-data`:

```html
<div
  layout:fragment="content"
  class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100"
  x-data="{ openPauseModal: false }"
>
```

---

## Prompt 8: Admin Dinas Belum Terpisah per Instansi — Semua Admin Melihat Semua Laporan

### Masalah
Saat ini **semua admin dinas** melihat data yang sama persis, karena:
1. `User.java` entity tidak memiliki relasi ke `Agency` — `ADMIN_DINAS` tidak terikat instansi tertentu.
2. `AdminDinasController.java` di semua method menggunakan `dispositionService.getAllDispositions()` yang mengembalikan **semua disposisi tanpa filter**.
3. Daftar petugas di `buildPetugasList()` memanggil `userService.findByRole(User.Role.PETUGAS)` yang mengembalikan **semua petugas di seluruh instansi**.
4. Session login (`ControllerHelper.requireAnyAdminSession()`) hanya menyimpan `userId` — tidak ada info `agencyId` untuk membedakan admin dinas PU, admin dinas Kebersihan, dll.

**Akibatnya**: Admin Dinas Pekerjaan Umum bisa melihat dan menugaskan laporan yang seharusnya hanya untuk Dinas Kebersihan, dan sebaliknya.

### SRS Reference
- **FR-DSP-04** (SRS 3.14): Disposisi harus menyertakan identitas dinas tujuan yang jelas.
- **FR-PRS-01**: Admin Dinas hanya dapat mengelola tugas yang menjadi wewenang instansinya.
- **FR-AKS-01 s/d 03**: Sistem harus menerapkan pembatasan akses berbasis peran dan instansi.

### Yang Harus Diperbaiki

#### a. Entity `User.java` — Tambah relasi ke Agency

```java
// ============================================================
// HAS-A (Association) dengan Agency — untuk ADMIN_DINAS
// Admin Dinas terikat dengan satu instansi tertentu
// ============================================================
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "agency_id")
private Agency agency;
```

Tambahkan getter/setter:

```java
public Agency getAgency() { return agency; }
public void setAgency(Agency agency) { this.agency = agency; }
```

> **Catatan**: Field `agency` hanya diisi untuk user dengan role `ADMIN_DINAS`. Untuk role lain (`WARGA`, `ADMIN_PUSAT`, `PETUGAS`) biarkan `null`.

#### b. Service `UserServiceImpl.java` — Update `createPetugas()` agar terikat agency admin yang membuat

Tambahkan overload `createPetugas(CreatePetugasDTO dto, String agencyId)` atau ubah logika di controller agar petugas yang dibuat otomatis terikat dengan agency admin dinas yang membuatnya.

Alternatif: cukup pastikan `CreatePetugasDTO` memiliki field `agencyId` (bisa diisi otomatis oleh controller dari session).

#### c. Repository — Tambah method untuk filter by agency

**`UserRepository.java`:**
```java
List<User> findByRoleAndAgencyAgencyId(User.Role role, String agencyId);
List<User> findByAgencyAgencyId(String agencyId);
```

**`DispositionRepository.java`:**
```java
List<Disposition> findByTargetAgencyAgencyIdOrderByDispatchedAtDesc(String agencyId);
```
> Method ini **sudah ada** di `DispositionRepository.java` — digunakan oleh `DispositionServiceImpl.getDispositions(String agencyId)`.

#### d. Controller Helper — Tambah session key untuk agencyId

**`ControllerHelper.java`:**
```java
public static final String SESSION_AGENCY_ID = "agencyId";
public static final String SESSION_AGENCY_NAME = "agencyName";

public static String getSessionAgencyId(HttpSession session) {
    return (String) session.getAttribute(SESSION_AGENCY_ID);
}

public static String requireAgencySession(HttpSession session) {
    String userId = requireAnyAdminSession(session);
    String agencyId = getSessionAgencyId(session);
    if (userId == null || agencyId == null) return null;
    return userId;
}
```

#### e. Login — Simpan agencyId ke session saat admin dinas login

Di controller login (misal `AdminLoginController.java` atau yang menangani login admin), setelah user ditemukan dan role-nya `ADMIN_DINAS`:

```java
if (user.getRole() == User.Role.ADMIN_DINAS && user.getAgency() != null) {
    session.setAttribute("agencyId", user.getAgency().getAgencyId());
    session.setAttribute("agencyName", user.getAgency().getAgencyName());
}
```

> **Jika tidak ada login flow khusus untuk admin dinas**: Buat endpoint login terpisah atau perbaiki login flow yang ada agar menyimpan agencyId.

#### f. `AdminDinasController.java` — Filter semua query berdasarkan agencyId

**1. Semua method yang memanggil `dispositionService.getAllDispositions()`:**

Ubah menjadi:
```java
String agencyId = ControllerHelper.getSessionAgencyId(session);
if (agencyId == null) return "redirect:/admin/login";

// Sebelumnya: dispositionService.getAllDispositions()
// Sesudah:
List<Disposition> dispositions;
if (agencyId != null) {
    dispositions = dispositionService.getDispositionsByAgency(agencyId);
} else {
    dispositions = dispositionService.getAllDispositions(); // fallback
}
```

Method yang perlu diperbaiki:
- `adminDinasDashboard()` (baris ~68): `allDisp` untuk pendingAssignments
- `adminDinasQueue()` (baris ~118): `realDispositions` untuk laporanDinas
- `adminDinasPenugasan()` (baris ~169): `allDisp` untuk incomingReports

**2. Method `buildPetugasList()` — Filter petugas berdasarkan agency:**

```java
private List<Map<String, Object>> buildPetugasList(String agencyId) {
    List<User> realPetugas;
    if (agencyId != null) {
        realPetugas = userRepository.findByRoleAndAgencyAgencyId(User.Role.PETUGAS, agencyId);
    } else {
        realPetugas = userService.findByRole(User.Role.PETUGAS);
    }
    // ... sisanya sama
}
```

Atau jika tidak ada relasi agency di User untuk PETUGAS:

```java
// Filter petugas yang dibuat oleh admin dinas dari agency yang sama
// Atau: semua petugas visible ke semua admin dinas (lebih sederhana)
// Tergantung kebutuhan bisnis
```

> **Rekomendasi**: Petugas bisa dilihat oleh semua admin dinas (karena satu kota/kabupaten), tetapi **tugas hanya bisa ditugaskan oleh admin dinas yang berwenang**. Atau jika petugas juga terikat agency, filter seperti di atas.

**3. Method yang menampilkan nama dinas:**

Di `adminDinasDashboard()` dan method lain, ganti hardcoded `"Dinas Pekerjaan Umum"` dengan:

```java
String agencyName = ControllerHelper.getSessionAgencyName(session);
model.addAttribute("dinasName", agencyName != null ? agencyName : "Dinas Terkait");
```

#### g. Skenario Pendaftaran Admin Dinas — Saat admin pusat membuat akun admin dinas

Di `AdminPusatController.java` atau form pembuatan admin dinas, pastikan `agency` diisi:

```java
// Contoh: saat membuat admin dinas baru
User adminDinas = new User();
adminDinas.setRole(User.Role.ADMIN_DINAS);
Agency agency = agencyRepository.findById(selectedAgencyId)
    .orElseThrow(() -> new RuntimeException("Agency not found"));
adminDinas.setAgency(agency);
// ... field lain
userRepository.save(adminDinas);
```

#### h. Update Template — Tampilkan nama dinas yang sesuai

Di semua template admin dinas (`dinas-dashboard.html`, `dinas-queue.html`, `penugasan-petugas.html`, dll), pastikan judul/nama dinas menggunakan `th:text="${dinasName}"` yang sudah dikirim dari controller.

#### i. Update `disposisi-detail.html` — Tampilkan dinas tujuan yang sudah dipilih

Saat admin pusat sudah mendisposisikan laporan ke dinas tertentu, tampilkan informasi dinas tujuan di detail disposisi agar admin pusat tahu ke mana laporan dikirim.

### Contoh Implementasi Lengkap (Method `adminDinasDashboard`)

```java
@GetMapping("/admin/dinas/dashboard")
public String adminDinasDashboard(Model model, HttpSession session) {
    String sessionUserId = ControllerHelper.requireAgencySession(session);
    if (sessionUserId == null) return "redirect:/admin/login";

    String agencyId = ControllerHelper.getSessionAgencyId(session);
    String agencyName = ControllerHelper.getSessionAgencyName(session);

    model.addAttribute("dinasName", agencyName != null ? agencyName : "Dinas");

    // Statistik — filter by agency
    long diterima = dispositionService.getDispositionsByAgency(agencyId).size();
    // Untuk count by status, perlu bikin method baru atau filter di Java
    // Sementara: hitung dari fieldTask yang terkait disposisi agency ini
    long diproses = dispositionService.getDispositionsByAgency(agencyId).stream()
        .filter(d -> d.getReport() != null)
        .flatMap(d -> fieldTaskService.getTasksByReport(d.getReport().getReportId()).stream())
        .filter(t -> t.getTaskStatus() == FieldTask.TaskStatus.SEDANG_DIKERJAKAN)
        .count();

    // ... sisanya dengan filter agencyId
    
    List<Map<String, Object>> pendingAssignments = new ArrayList<>();
    List<Disposition> allDisp = dispositionService.getDispositionsByAgency(agencyId);
    for (Disposition d : allDisp) {
        if (d.getReport() != null) {
            List<FieldTask> existingTasks = fieldTaskService.getTasksByReport(d.getReport().getReportId());
            if (existingTasks.isEmpty()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", d.getReport().getReportId());
                m.put("judul", d.getReport().getTicketNumber() != null ? d.getReport().getTicketNumber() : "Laporan");
                m.put("kategori", d.getReport().getCategory() != null ? d.getReport().getCategory().getCategoryName() : "Lainnya");
                m.put("prioritas", d.getPriority() != null ? d.getPriority() : "Sedang");
                m.put("slaStatus", "-");
                pendingAssignments.add(m);
            }
        }
    }
    model.addAttribute("pendingAssignments", pendingAssignments);

    List<Map<String, Object>> petugasList = buildPetugasList(agencyId);
    model.addAttribute("availablePetugas", petugasList.isEmpty() ? new ArrayList<>() : petugasList);

    return "admin/dinas/dinas-dashboard";
}
```

### Catatan Tambahan

1. **Kompatibilitas mundur**: Jika ada admin dinas yang sudah ada di database tanpa `agency`, beri default atau minta admin pusat mengisi agency mereka.
2. **Sesi login**: Pastikan saat admin dinas login ulang, `agencyId` dan `agencyName` selalu disimpan ke session.
3. **Admin Pusat**: Admin pusat tetap bisa melihat SEMUA laporan (tidak perlu filter agency). Hanya `AdminDinasController` yang perlu filter.
4. **Migration SQL**: Jika menggunakan database existing, perlu migration untuk menambah kolom `agency_id` di tabel `users`:
   ```sql
   ALTER TABLE users ADD COLUMN agency_id VARCHAR(36);
   ALTER TABLE users ADD CONSTRAINT fk_users_agency FOREIGN KEY (agency_id) REFERENCES agencies(agency_id);
   ```

---

## Ringkasan File yang Perlu Diubah

| File | Prompt Terkait |
|------|----------------|
| `model/User.java` | 8a |
| `model/Disposition.java` | 1, 2, 3 |
| `model/UserProfile.java` | 4, 5 |
| `dto/DispositionDTO.java` | 1 |
| `dto/CreatePetugasDTO.java` | 4, 5 |
| `service/DispositionService.java` | 1 |
| `service/DispositionServiceImpl.java` | 1, 2, 3 |
| `service/UserServiceImpl.java` | 4, 5, 8b |
| `service/FieldTaskService.java` | 7d |
| `service/FieldTaskServiceImpl.java` | 4, 7c, 7d |
| `service/SlaRecordService.java` | 7a, 7b |
| `service/SlaRecordServiceImpl.java` | 7a, 7b |
| `repository/UserRepository.java` | 8c |
| `controller/AdminPusatController.java` | 1, 2, 3, 8g |
| `controller/AdminDinasController.java` | 2, 3, 4, 5, 6b, 6c, 6g, 7e, 8f |
| `controller/ControllerHelper.java` | 6a (opsional), 8d |
| `controller/AdminLoginController.java` | 8e (jika ada) |
| `templates/admin/disposisi-detail.html` | 1, 8i |
| `templates/admin/dinas/penugasan-petugas.html` | 4, 6d, 6e, 6f |
| `templates/admin/dinas/petugas.html` | 4, 5 |
| `templates/admin/dinas/dinas-dashboard.html` | 2 (opsional), 8h |
| `templates/admin/dinas/dinas-queue.html` | 3 (opsional), 8h |
| `templates/admin/dinas/progress-update.html` | 7f, 7g, 7h |

## Urutan Implementasi yang Direkomendasikan

1. **Phase 1 - Data Model** (Prompt 1a, 4a, 5a, 8a): Tambah field ke entity (`Disposition`, `UserProfile`, `User.agency`)
2. **Phase 2 - DTO & Service** (Prompt 1b, 1c, 4b, 5b): Update DTO dan service layer
3. **Phase 3 - Repository** (Prompt 8c): Tambah method query by agency
4. **Phase 4 - Controller Helper & Login** (Prompt 8d, 8e): Tambah session key, simpan agencyId saat login
5. **Phase 5 - Controller** (Prompt 1d, 2, 3, 4d, 5c, 5f, 6b, 6c, 6g, 7e, 8f, 8g): Update controller untuk membaca/menyimpan data baru dan filter by agency
6. **Phase 6 - Templates** (Prompt 1e, 4e, 4f, 5d, 5e, 6d, 6e, 6f, 7f, 7g, 7h, 8h, 8i): Update HTML templates
7. **Phase 7 - Validasi** (Prompt 4e, 6): Implementasi peringatan wilayah FR-PRS-03 dan status ketersediaan dinamis
8. **Phase 8 - Jeda Waktu** (Prompt 7): Implementasi fitur pause/resume SLA
