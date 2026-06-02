# SCN-06 — Sengketa: Petugas Baru Ditugaskan

**Status Akhir Laporan:** `SELESAI` (setelah sengketa diselesaikan & petugas baru berhasil kerjakan)  
**Aktor:** Warga → (alur normal s/d MENUNGGU_KONFIRMASI) → Warga TOLAK konfirmasi → SENGKETA → Admin Dinas resolve → Petugas Baru → Selesai  
**Estimasi Waktu:** 40–50 menit  
**Prasyarat:** Ada 2 akun petugas di dinas yang sama. **(prasyarat ini belum tercapai di semua admin dinas)**

---

## 🗺️ Alur

```
[Alur normal s/d petugas selesai tugas]
    → Laporan = MENUNGGU_KONFIRMASI
    → Warga MENOLAK konfirmasi (tidak puas)
    → Status = SENGKETA
    → Admin Dinas buka panel sengketa
    → Keputusan: "Terima" + pilih petugas baru
    → Status laporan = DITUGASKAN (petugas baru)
    → Petugas baru kerjakan tugas
    → Warga konfirmasi → SELESAI
```

---

## LANGKAH DETAIL

### FASE 1 — Alur Normal s/d MENUNGGU_KONFIRMASI

| # | Aksi | Hasil Ekspektasi | ✓/✗ |
|---|------|------------------|-----|
| 1.1 | Warga buat laporan | Status = Menunggu | `[✓]` |
| 1.2 | Admin pusat approve | Status = Divalidasi | `[✓]` |
| 1.3 | Admin pusat disposisi | Status = Didisposisi + SLA dibuat | `[✓]` |
| 1.4 | Admin dinas tugaskan petugas A | Status = Ditugaskan | `[✗]` | status masih tetap "Didisposisi"
| 1.5 | Petugas A check-in + start + upload foto + selesai | Status FieldTask = SELESAI | `[✓]` |
| 1.6 | Cek status laporan | Status laporan = **MENUNGGU_KONFIRMASI** | `[✓]` |

---

### FASE 2 — Warga Ajukan Sengketa (Tidak Puas)

🔄 **Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan                                                        |
|---|------|-----|------------|------------------|-----|----------------------------------------------------------------|
| 2.1 | Buka detail laporan | `/warga/report-detail?id=...` | Status | "Menunggu Konfirmasi" | `[✓]` |                                                                |
| 2.2 | Lihat tombol yang tersedia | - | 📸 Tombol | Tombol "Konfirmasi Selesai" DAN "Ajukan Sengketa" tersedia | `[✓]` |                                                                |
| 2.3 | Klik **"Ajukan Sengketa"** | - | Form sengketa | Form alasan + upload foto bukti muncul | `[✓]` |                                                                |
| 2.4 | Isi alasan sengketa | - | Textarea alasan | Bisa diisi | `[✓]` |                                                                |
| 2.5 | Upload foto bukti ketidakpuasan | - | Upload foto | Preview foto tampil | `[✗]` | tidak bisa upload, tidak ada preview foto, tetapi kamera aktif |
| 2.6 | **Submit sengketa** | POST `/warga/dispute-report` | Flash message | "Sengketa berhasil diajukan." | `[✗]` |                                                                |
| 2.7 | Cek status laporan | `/warga/report-detail?id=...` | 📋 Status | Status = **"Sengketa"** | `[✗]` |                                                                |
| 2.8 | ⚠️ Coba ajukan sengketa lagi | POST dispute lagi | Error | Tidak bisa ajukan sengketa 2x pada laporan yang sama | `[✗]` | tidak dapat ditest karena sengketa sebelumnya belum berfungsi  |

---

### FASE 3 — Admin Dinas Resolve Sengketa

🔄 **Admin Dinas**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 3.1 | Buka panel sengketa dinas | `/admin/dinas/sengketa` | Daftar sengketa | Sengketa warga tampil | `[✗]` | |
| 3.2 | Pilih sengketa tersebut | - | Detail sengketa | Alasan + foto bukti warga tampil | `[✗]` | |
| 3.3 | Cek daftar petugas pengganti | - | Dropdown petugas | Daftar petugas (kecuali petugas A) tampil | `[✗]` | |
| 3.4 | Pilih **keputusan "Diterima"** | - | Radio/select | "Tugaskan Kembali" tersedia | `[✗]` | |
| 3.5 | Pilih petugas B sebagai pengganti | - | Dropdown | Petugas B terpilih | `[✗]` | |
| 3.6 | Isi catatan resolusi | - | Textarea | Bisa diisi | `[✗]` | |
| 3.7 | **Submit resolusi** | POST `/admin/dinas/sengketa` | Redirect | "reassigned=true" | `[✗]` | |

---

### FASE 4 — Verifikasi Setelah Resolusi

🔄 **Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 4.1 | Cek detail laporan | `/warga/report-detail?id=...` | 📋 Status | Status = **"Ditugaskan"** (petugas baru) | `[✗]` | |

🔄 **Petugas B (petugas baru)**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 4.2 | Login petugas B | `/petugas/login` | - | Masuk dashboard | `[✗]` | |
| 4.3 | Cek daftar tugas | `/petugas/tasks` | Tugas baru | Tugas laporan sengketa muncul | `[✗]` | |
| 4.4 | Kerjakan tugas (start → foto → selesai) | - | - | Status → MENUNGGU_KONFIRMASI lagi | `[✗]` | |

🔄 **Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 4.5 | Cek status laporan | `/warga/report-detail?id=...` | Status | "Menunggu Konfirmasi" | `[✗]` | |
| 4.6 | **Konfirmasi selesai** | POST `/warga/confirm-report` | Flash | "Laporan dikonfirmasi selesai" | `[✗]` | |
| 4.7 | Cek status akhir | - | 📋 Status | **SELESAI** | `[✗]` | |

---

## ✅ Kriteria LULUS

- [✗] Sengketa berhasil diajukan saat status MENUNGGU_KONFIRMASI
- [✗] Status laporan berubah ke SENGKETA
- [✗] Admin dinas bisa resolve dengan pilih petugas baru
- [✗] Petugas baru mendapat tugas dan bisa kerjakan
- [✗] Setelah petugas baru selesai, warga bisa konfirmasi dan status = SELESAI

**Hasil Akhir:** `[ ] LULUS` / `[✗] GAGAL`  
**Catatan Bug:** Untuk bug kemungkinan hanya pada fase 1.4, sisanya adalah implementasi fitur sengketa yang belum lengkap sehingga banyak kegagalan pada testing
