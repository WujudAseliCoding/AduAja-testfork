# SCN-03 — Revisi 1x → Disetujui → Selesai

**Status Akhir Laporan:** `SELESAI`  
**Aktor:** Warga → Admin Pusat (minta revisi) → Warga (kirim revisi) → Admin Pusat (approve) → Admin Dinas → Petugas → Warga  
**Estimasi Waktu:** 30–40 menit  
**Prasyarat:** Semua akun aktif. Petugas terdaftar di dinas yang sama.

---

## 🗺️ Alur Skenario

```
Warga buat laporan
    → Admin Pusat: PERLU REVISI + isi catatan
    → Warga lihat catatan → kirim revisi
    → Status balik ke MENUNGGU_VALIDASI
    → Admin Pusat validasi ulang → APPROVE
    → (Lanjut seperti SCN-01: disposisi → penugasan → dikerjakan → konfirmasi → SELESAI)
```

---

## LANGKAH DETAIL

### FASE 1 — Warga Buat Laporan

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                                                                                                           |
|---|------|-----|------------|------------------|------|-----------------------------------------------------------------------------------------------------------------------------------|
| 1.1 | Login warga | `/warga/login` | - | Masuk dashboard | `[✓]` |                                                                                                                                   |
| 1.2 | Buat laporan baru dengan deskripsi **sengaja singkat** | `/warga/create-report` | - | Laporan tersubmit | `[x]` | Misal deskripsi: "Jalan rusak" (tanpa detail) ( sudah dipaksa minimal 20 karakter, jaid jika kurang dari 20 karakter tidak bisa ) |
| 1.3 | Cek status laporan | `/warga/report-detail?id=...` | Status | "Menunggu" | `[✓]` | CATAT ID: _______                                                                                                                 |

---

### FASE 2 — Admin Pusat Minta Revisi

🔄 **Ganti ke Admin Pusat**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗   | Catatan                                                                                                                                                |
|---|------|-----|------------|------------------|-------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2.1 | Login admin pusat | `/admin/login` | - | Masuk | `[✓]` |                                                                                                                                                        |
| 2.2 | Buka panel validasi | `/admin/validation` | Daftar laporan | Laporan tampil | `[✓]` |                                                                                                                                                        |
| 2.3 | Pilih laporan tadi | - | Detail | Tampil | `[✓]` |                                                                                                                                                        |
| 2.4 | Pilih aksi **"Perlu Revisi"** | - | Button/radio | Tersedia | `[✓]` |                                                                                                                                                        |
| 2.5 | Isi catatan revisi yang jelas | - | Field catatan | Misal: "Harap lengkapi deskripsi lokasi kerusakan secara detail" | `[✓]` |                                                                                                                                                        |
| 2.6 | **Submit minta revisi** | POST `/admin/validation` action=revision | Flash message | "Laporan Perlu Revisi" | `[x]` | saat ditekan revisi malah ke redirect ke halaman ini yang isinya kosong http://localhost:8080/admin/validation?id=f8b109e6-cf6a-4895-9f16-955b3c4815b5 |
| 2.7 | Cek laporan masih ada di antrian? | `/admin/validation` | Daftar | ⚠️ Laporan PERLU_REVISI: apakah masih tampil atau hilang? | `[x]` | sama seperti yang tadi redirect ke http://localhost:8080/admin/validation?id=f8b109e6-cf6a-4895-9f16-955b3c4815b5 yang isinya kosong                   |

---

### FASE 3 — Warga Kirim Revisi

🔄 **Kembali ke Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                                                             |
|---|------|-----|------------|------------------|------|-------------------------------------------------------------------------------------|
| 3.1 | Cek notifikasi | `/warga/notifications` | 📋 Notif | "Laporan Perlu Revisi" dengan catatan admin | `[✓]` |                                                                                     |
| 3.2 | Cek detail laporan | `/warga/report-detail?id=...` | 📋 Status | Status = **"Perlu Revisi"** (warna amber) | `[✓]` |                                                                                     |
| 3.3 | Cek catatan admin tampil | - | 📋 Catatan | Teks catatan dari admin tampil | `[x]` | tidak ada pesan revisi dari admin                                                   |
| 3.4 | Cek tombol revisi muncul | - | 📸 Tombol | Tombol/form "Kirim Revisi" tersedia | `[✓]` |                                                                                     |
| 3.5 | ⚠️ Coba submit revisi saat status bukan PERLU_REVISI | (Manipulasi: laporan lain yg bukan PERLU_REVISI) | Error | "Revisi hanya dapat dilakukan saat status Perlu Revisi" | `[✓]` | Skip jika tidak ada laporan lain                                                    |
| 3.6 | Isi deskripsi revisi yang lebih lengkap | - | Form revisi | Deskripsi baru bisa diisi | `[✓]` |                                                                                     |
| 3.7 | Update patokan lokasi | - | Field patokan | Bisa diubah | `[x]` | tidak bisa diubah malah tidak bisa ditekan, lokasi otomatis juga tidak bisa ditekan |
| 3.8 | Upload foto baru (opsional) | - | Upload foto | Bisa upload ulang | `[x]` | tidak bisa upload foot ulang, seharusnya bisa                                       |
| 3.9 | **Submit revisi** | POST `/warga/revise-report` | Flash message | "Revisi laporan berhasil dikirim. Admin akan meninjau kembali." | `[✓]` | bisa di submit tapi tidak ada flash message                                         |
| 3.10 | Cek status laporan setelah revisi | `/warga/report-detail?id=...` | 📋 Status | Status kembali ke **"Menunggu"** | `[✓]` |                                                                                     |
| 3.11 | Cek tombol revisi menghilang | - | 📸 Tombol | Tombol revisi tidak ada lagi | `[✓]` |                                                                                     |

---

### FASE 4 — Admin Pusat Validasi Ulang & Approve

🔄 **Kembali ke Admin Pusat**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan                            |
|---|------|-----|------------|------------------|-----|------------------------------------|
| 4.1 | Cek antrian validasi | `/admin/validation` | Daftar laporan | Laporan muncul kembali di antrian | `[✓]` |                                    |
| 4.2 | Cek deskripsi laporan sudah direvisi | - | 📋 Deskripsi | Deskripsi baru tampil (hasil revisi warga) | `[✓]` |                                    |
| 4.3 | **Approve laporan** | POST `/admin/validation` action=approve | Flash message | "Laporan Divalidasi" | `[✓]` | Tidak ada flash message, coba buat |

---

### FASE 5 — Lanjut sampai Selesai

> Ikuti langkah dari **SCN-01 FASE 3 s/d FASE 6** (Disposisi → Penugasan → Dikerjakan → Konfirmasi Warga)

| # | Aksi | Ringkasan | Status Akhir | ✓/✗  |
|---|------|-----------|-------------|------|
| 5.1 | Admin pusat disposisi | Set dinas, prioritas, deadline | DIDISPOSISI | `[✓]` |
| 5.2 | Admin dinas tugaskan petugas | Pilih petugas | DITUGASKAN | `[x]` |
| 5.3 | Petugas check-in + start tugas | Geofencing | SEDANG_DIKERJAKAN | `[x]` |
| 5.4 | Petugas upload foto + selesaikan | Before + After | MENUNGGU_KONFIRMASI | `[✓]` | status akhir gak lengkap
| 5.5 | Warga konfirmasi | Klik "Konfirmasi Selesai" | **SELESAI** | `[✓]` |

---

## ✅ Kriteria LULUS

- [x] Status laporan mengikuti: MENUNGGU → PERLU_REVISI → MENUNGGU → DIVALIDASI → ... → SELESAI
- [x] Catatan revisi dari admin tampil di halaman warga
- [✓] Warga hanya bisa revisi saat status PERLU_REVISI
- [✓] Setelah revisi, status kembali ke MENUNGGU_VALIDASI
- [✓] Laporan masuk kembali ke antrian admin

**Hasil Akhir:** `[ ] LULUS` / `[x] GAGAL`  
**Catatan Bug:** Coba perbaiki yang tidak sesuai_________________________________
