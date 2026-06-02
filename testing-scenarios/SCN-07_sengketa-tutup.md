# SCN-07 — Sengketa: Laporan Ditutup Admin

**Status Akhir Laporan:** `DITUTUP`  
**Aktor:** Warga → (alur normal s/d MENUNGGU_KONFIRMASI) → Warga sengketa → Admin Pusat/Dinas tutup  
**Estimasi Waktu:** 25 menit

---

## 🗺️ Alur

```
[Alur normal s/d MENUNGGU_KONFIRMASI]
    → Warga ajukan sengketa
    → Status = SENGKETA
    → Admin Pusat: keputusan TUTUP LAPORAN
    → Status = DITUTUP (final)
```

---

## LANGKAH DETAIL

### FASE 1 — Alur Normal s/d MENUNGGU_KONFIRMASI

| # | Aksi | Hasil Ekspektasi | ✓/✗ |
|---|------|------------------|-----|
| 1.1–1.6 | Ikuti SCN-01 Fase 1–5 | Status = MENUNGGU_KONFIRMASI | `[✓]` |

---

### FASE 2 — Warga Ajukan Sengketa

| # | Aksi | URL | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------------|-----|---------|
| 2.1 | Warga buka detail laporan | `/warga/report-detail?id=...` | Status = "Menunggu Konfirmasi" | `[✓]` | |
| 2.2 | Ajukan sengketa + alasan | POST `/warga/dispute-report` | Status = "Sengketa" | `[✗]` | |
| 2.3 | Cek notifikasi admin | Dashboard admin | Sengketa baru muncul | `[✗]` | |

---

### FASE 3 — Admin Dinas Tutup Sengketa

🔄 **Admin Dinas**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 3.1 | Buka panel sengketa | `/admin/sengketa` | Daftar sengketa | Sengketa dari warga tampil | `[✗]` | |
| 3.2 | Pilih sengketa | - | Detail | Alasan & foto bukti sengketa tampil | `[✗]` | |
| 3.3 | Pilih keputusan **"Tutup Laporan"** | - | Radio/button | Opsi tersedia | `[✗]` | |
| 3.4 | Isi catatan resolusi | - | Textarea | Bisa diisi | `[✗]` | |
| 3.5 | **Submit resolusi** | POST `/admin/sengketa` keputusan=tutup | Redirect | Kembali ke panel sengketa | `[✗]` | |
| 3.6 | Cek sengketa sudah resolved | `/admin/sengketa` | Status | Sengketa tidak tampil lagi di "Menunggu Tinjauan" | `[✗]` | |

---

### FASE 4 — Verifikasi di Warga

🔄 **Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 4.1 | Cek detail laporan | `/warga/report-detail?id=...` | 📋 Status | Status = **"Ditutup"** | `[✗]` | |
| 4.2 | Cek tombol aksi | - | 📸 Tombol | Tidak ada tombol aksi (final status) | `[✗]` | |
| 4.3 | Cek riwayat laporan | `/warga/report-history` | Filter status | Laporan tampil dengan label "Ditutup" | `[✗]` | |

---

## ✅ Kriteria LULUS

- [✗] Sengketa berhasil diajukan
- [✗] Admin bisa resolve dengan "Tutup Laporan"
- [✗] Status laporan akhir = DITUTUP
- [✗] Warga tidak bisa melakukan aksi apapun pada laporan DITUTUP

**Hasil Akhir:** `[ ] LULUS` / `[✗] GAGAL`  
**Catatan Bug:** _________________________________


---

# SCN-08 — Konfirmasi Timeout: Laporan Ditutup Otomatis

**Status Akhir Laporan:** `DITUTUP`  
**Aktor:** Warga → (alur normal s/d MENUNGGU_KONFIRMASI) → [Warga tidak merespons] → Sistem timeout → DITUTUP  
**Estimasi Waktu:** Tergantung scheduler (atau simulasi manual)  
**Catatan:** Deadline konfirmasi = 72 jam. Test ini membutuhkan simulasi atau pengecekan kode scheduler.

---

## 🗺️ Alur

```
[Alur normal s/d petugas selesai tugas]
    → Laporan = MENUNGGU_KONFIRMASI
    → ConfirmationRequest dibuat (deadline = now + 72 jam)
    → Warga TIDAK merespons sampai deadline lewat
    → Scheduler: processTimeouts() → DITUTUP + notif warga
```

---

## LANGKAH DETAIL

### FASE 1 — Alur hingga MENUNGGU_KONFIRMASI

| # | Aksi                                                       | Hasil Ekspektasi | ✓/✗ |
|---|------------------------------------------------------------|------------------|-----|
| 1.1 | Ikuti SCN-01 Fase 1–5 (s/d petugas selesai)                | Status = MENUNGGU_KONFIRMASI | `[✓]` |
| 1.2 | Cek ConfirmationRequest dibuat, DB / cek di detail laporan | Deadline 72 jam tampil di halaman warga | `[✓]` |

### FASE 2 — Simulasi Timeout (2 cara)

**Cara A: Simulasi via DB** (cepat)

| # | Aksi | Yang Dilakukan | Hasil Ekspektasi | ✓/✗ |
|---|------|----------------|------------------|-----|
| 2A.1 | Update deadline di DB | `UPDATE confirmation_requests SET deadline_at = NOW() - INTERVAL '1 hour' WHERE ...` | Deadline sudah lewat | `[✗]` |
| 2A.2 | Trigger scheduler manual | Panggil endpoint scheduler atau restart app | `processTimeouts()` dijalankan | `[✗]` |
| 2A.3 | Cek status laporan | DB / `/warga/report-detail` | Status = **DITUTUP** | `[✗]` |

**Cara B: Tunggu Scheduler Berjalan** (jika ada @Scheduled)

| # | Aksi | Yang Dicek | Hasil Ekspektasi | ✓/✗ |
|---|------|------------|------------------|-----|
| 2B.1 | Cek konfigurasi scheduler | Cari `@Scheduled` di kode | Ada scheduled task untuk `processTimeouts()` | `[✗]` |
| 2B.2 | Tunggu deadline + 1 run scheduler | - | Status laporan = DITUTUP | `[✗]` |

### FASE 3 — Verifikasi

| # | Aksi | URL | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------------|-----|---------|
| 3.1 | Cek detail laporan warga | `/warga/report-detail` | Status = **"Ditutup"** | `[✗]` | |
| 3.2 | Cek notifikasi warga | `/warga/notifications` | "Laporan Ditutup Otomatis" + penjelasan 3x24 jam | `[✗]` | |
| 3.3 | ⚠️ Cek ConfirmationRequest response = TIMEOUT | DB | `response = TIMEOUT`, `is_locked = true` | `[ ]` | |

---

## ✅ Kriteria LULUS

- [✗] Setelah deadline lewat + scheduler berjalan, status = DITUTUP
- [✗] Warga mendapat notifikasi "Laporan Ditutup Otomatis"
- [✗] ConfirmationRequest.response = TIMEOUT

**Hasil Akhir:** `[ ] LULUS` / `[✗] GAGAL`  
**Catatan Bug:** sudah melakukan simulasi timeout dari DB langsung, tapi status laporan tidak berubah ke DITUTUP. Perlu dicek apakah scheduler `processTimeouts()` berjalan dengan benar dan mengupdate status laporan. Juga cek apakah ada error di log saat scheduler jalan.
