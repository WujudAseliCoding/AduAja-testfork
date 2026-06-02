# SCN-05 — Revisi Berulang 2x (Loop Validasi)

**Status Akhir Laporan:** `SELESAI`  
**Aktor:** Warga → Admin Pusat (2x minta revisi) → Warga (2x revisi) → Admin Pusat (approve ke-3) → Admin Dinas → Petugas → Warga  
**Estimasi Waktu:** 25 menit  
**Prasyarat:** Akun warga + admin pusat aktif.

---

## 🗺️ Alur

```
Warga buat laporan
    → Admin: PERLU REVISI (ke-1) + catatan pertama
    → Warga lihat catatan → kirim revisi ke-1 → status: MENUNGGU_VALIDASI
    → Admin: PERLU REVISI (ke-2) + catatan kedua
    → Warga lihat catatan baru → kirim revisi ke-2 → status: MENUNGGU_VALIDASI
    → Admin: APPROVE (ke-3)
    → Disposisi → Penugasan → Dikerjakan → Konfirmasi → SELESAI
```

---

## LANGKAH DETAIL

### FASE 1 — Warga Buat Laporan

| # | Aksi | URL | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------------|-----|---------|
| 1.1 | Login warga | `/warga/login` | Masuk dashboard | `[✓]` | |
| 1.2 | Buat laporan baru (deskripsi sengaja minim) | `/warga/create-report` | Status = Menunggu | `[✓]` | ID: _______ |

---

### FASE 2 — Admin Pusat Minta Revisi Pertama

🔄 **Admin Pusat**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 2.1 | Login admin pusat | `/admin/login` | - | Masuk | `[✓]` | |
| 2.2 | Buka antrian validasi | `/admin/validation` | Laporan tampil | - | `[✓]` | |
| 2.3 | Pilih aksi **Perlu Revisi** | - | - | - | `[✓]` | |
| 2.4 | Isi catatan revisi ke-1 | - | Field catatan | "Tolong lengkapi lokasi pasti kerusakan." | `[✓]` | |
| 2.5 | Submit | POST `/admin/validation` action=revision | Flash | "Laporan Perlu Revisi" | `[✓]` | |

---

### FASE 3 — Warga Kirim Revisi Pertama

🔄 **Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                                                    |
|---|------|-----|------------|------------------|------|----------------------------------------------------------------------------|
| 3.1 | Cek notifikasi | `/warga/notifications` | Notif | "Laporan Perlu Revisi" + catatan ke-1 tampil | `[x]` |                                                                            |
| 3.2 | Buka detail laporan | `/warga/report-detail?id=...` | Status | "Perlu Revisi" | `[✓]` |                                                                            |
| 3.3 | Cek catatan admin ke-1 tampil | - | 📋 Catatan | Teks catatan ke-1 tampil | `[✓]` | ubah dulu  ukuran teks alasannya dan lokasi alasannya karena susah dilihat |
| 3.4 | Submit revisi pertama (lengkapi lokasi) | POST `/warga/revise-report` | Flash | Revisi berhasil dikirim | `[✓]` |                                                                            |
| 3.5 | Cek status laporan | `/warga/report-detail?id=...` | Status | Kembali ke **"Menunggu"** | `[✓]` |                                                                            |

---

### FASE 4 — Admin Pusat Minta Revisi Kedua

🔄 **Admin Pusat**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan                                                                                                          |
|---|------|-----|------------|------------------|-----|------------------------------------------------------------------------------------------------------------------|
| 4.1 | Cek antrian validasi | `/admin/validation` | Laporan | Laporan muncul kembali di antrian | `[✓]` |                                                                                                                  |
| 4.2 | Cek deskripsi sudah direvisi | - | 📋 Deskripsi | Konten revisi ke-1 tampil | `[✓]` | MAsalahnya tidak ada tanda bahwa tiket ini adalah hasil revisi, coba berikan tanda kalau ini adalah tiket revisi |
| 4.3 | Pilih **Perlu Revisi** lagi (ke-2) | - | - | Tombol revisi masih tersedia | `[✓]` |                                                                                                                  |
| 4.4 | Isi catatan revisi ke-2 | - | Field catatan | "Harap lampirkan foto yang lebih jelas." | `[✓]` |                                                                                                                  |
| 4.5 | Submit revisi ke-2 | POST revision | Flash | "Laporan Perlu Revisi" | `[✓]` |                                                                                                                  |

---

### FASE 5 — Warga Kirim Revisi Kedua

🔄 **Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 5.1 | Cek notifikasi | `/warga/notifications` | Notif ke-2 | "Laporan Perlu Revisi" (yang kedua) | `[✓]` | |
| 5.2 | Cek detail laporan | `/warga/report-detail?id=...` | Status | "Perlu Revisi" | `[✓]` | |
| 5.3 | ⚠️ Cek riwayat revisi | - | 📋 Riwayat | Ada daftar revisi sebelumnya (revisi ke-1 tercatat) | `[✓]` | |
| 5.4 | Cek catatan admin ke-2 tampil | - | 📋 Catatan | Teks catatan ke-2 tampil | `[✓]` | |
| 5.5 | Upload foto baru + submit revisi ke-2 | POST `/warga/revise-report` | Flash | Revisi berhasil | `[✓]` | |
| 5.6 | Cek status kembali ke Menunggu | - | Status | **"Menunggu"** | `[✓]` | |

---

### FASE 6 — Admin Approve & Lanjut Selesai

🔄 **Admin Pusat**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                                                |
|---|------|-----|------------|------------------|------|------------------------------------------------------------------------|
| 6.1 | Cek antrian validasi | `/admin/validation` | Laporan | Muncul kembali untuk ke-3 kalinya | `[✓]` |                                                                        |
| 6.2 | Cek foto baru dari revisi ke-2 | - | 📋 Foto | Foto terbaru tampil | `[x]` | Gak bisa karena dibagian revisi gak bisa ambil foto sama isi koordinat |
| 6.3 | **Approve laporan** | POST action=approve | Flash | "Laporan Divalidasi" | `[✓]` |                                                                        |

**Lanjut alur normal:**

| # | Aksi | Hasil Ekspektasi | ✓/✗  |
|---|------|------------------|------|
| 6.4 | Admin disposisi | Status = DIDISPOSISI + SLA dibuat | `[✓]` |
| 6.5 | Admin dinas tugaskan petugas | Status = DITUGASKAN | `[x]` |
| 6.6 | Petugas check-in + start + foto + selesai | Status = MENUNGGU_KONFIRMASI | `[✓]` |
| 6.7 | Warga konfirmasi selesai | Status = **SELESAI** | `[✓]` |

---

## ✅ Kriteria LULUS

- [✓] Sistem memperbolehkan admin meminta revisi lebih dari 1x
- [✓] Setiap permintaan revisi menghasilkan notifikasi baru ke warga
- [✓] Catatan revisi terbaru tampil di halaman detail warga
- [✓] Setelah tiap revisi, status laporan kembali ke MENUNGGU_VALIDASI
- [✓] Alur normal berjalan setelah akhirnya di-approve

**Hasil Akhir:** `[ ] LULUS` / `[x] GAGAL`  
**Catatan Bug:** coba baca di catatan yang aku buat, coba perbaiki _________________________________
