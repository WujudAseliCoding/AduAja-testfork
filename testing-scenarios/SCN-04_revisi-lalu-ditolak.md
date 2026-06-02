# SCN-04 — Revisi → Lalu Ditolak

**Status Akhir Laporan:** `DITOLAK`  
**Aktor:** Warga → Admin Pusat → Warga (revisi) → Admin Pusat (tolak)  
**Estimasi Waktu:** 15 menit

---

## 🗺️ Alur

```
Warga buat laporan
    → Admin: PERLU REVISI
    → Warga kirim revisi
    → Admin menilai revisi masih kurang → TOLAK
    → Status = DITOLAK (final)
```

---

## LANGKAH DETAIL

| # | Aksi | URL | Hasil Ekspektasi | ✓/✗                                                                                | Catatan |
|---|------|-----|------------------|------------------------------------------------------------------------------------|---------|
| 1.1 | Warga buat laporan | `/warga/create-report` | Status = Menunggu | `[✓]`                                                                              | ID: _______ |
| 1.2 | Admin login → panel validasi | `/admin/validation` | Laporan tampil | `[✓]`                                                                              | |
| 1.3 | Admin pilih **Perlu Revisi** + isi catatan | POST revision | Status = PERLU_REVISI | `[✓]`                                                                              | |
| 1.4 | Warga cek notifikasi | `/warga/notifications` | Notif "Perlu Revisi" diterima | `[✓]`                                                                              | |
| 1.5 | Warga buka detail laporan | `/warga/report-detail` | Status "Perlu Revisi", catatan admin tampil | `[✓]`                                                                              | |
| 1.6 | Warga kirim revisi | POST `/warga/revise-report` | Status kembali "Menunggu", flash sukses | `[✗] form masih gak berfungsi dengan benar tapi bisa dikirim meskipun perubahan kosong` | |
| 1.7 | Admin cek antrian validasi | `/admin/validation` | Laporan muncul kembali | `[✓]`                                                                               | |
| 1.8 | ⚠️ Admin pilih **Tolak** + isi alasan baru | POST reject | "Laporan Ditolak" | `[✓]`                                                                               | |
| 1.9 | Warga cek notifikasi | `/warga/notifications` | Notif "Laporan Ditolak" muncul | `[✓]`                                                                               | |
| 1.10 | Warga cek detail laporan | `/warga/report-detail` | Status = **"Ditolak"** | `[✓]`                                                                               | |
| 1.11 | Cek tidak ada tombol revisi lagi | - | Tombol revisi tidak ada (sudah ditolak) | `[✓]`                                                                               | |

---

## ✅ Kriteria LULUS

- [✓] Laporan bisa ditolak meskipun sebelumnya sudah di-request revisi
- [✓] Status akhir = DITOLAK
- [✓] Warga mendapat notifikasi tiap perubahan status

**Hasil Akhir:** `[ ] LULUS` / `[✓] GAGAL`  
**Catatan Bug:** Masih ada bug form revisi yang tidak benar.

---

# SCN-05 — Revisi Berulang 2x (Loop Validasi)

**Status Akhir Laporan:** `SELESAI`  
**Aktor:** Warga → Admin Pusat (2x minta revisi) → Warga (2x revisi) → Admin Pusat (approve ke-3)  
**Estimasi Waktu:** 20 menit

---

## 🗺️ Alur

```
Warga buat laporan
    → Admin: PERLU REVISI (ke-1)
    → Warga revisi ke-1 → status Menunggu
    → Admin: PERLU REVISI (ke-2)
    → Warga revisi ke-2 → status Menunggu
    → Admin: APPROVE
    → (lanjut disposisi dst)
```

---

## LANGKAH DETAIL

| # | Aksi | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|------------------|-----|-------|
| 1.1 | Warga buat laporan | Status = Menunggu | `[✓]` | ID: _______ |
| 1.2 | Admin minta revisi pertama | Status = PERLU_REVISI | `[✓]` | |
| 1.3 | Warga kirim revisi pertama | Status = MENUNGGU_VALIDASI | `[✓]` | |
| 1.4 | Admin minta revisi kedua | Status = PERLU_REVISI (lagi) | `[✓]` | |
| 1.5 | Warga buka detail laporan | 📋 Status = "Perlu Revisi" | `[✓]` | |
| 1.6 | ⚠️ Cek riwayat revisi tampil | Daftar revisi di detail laporan | Ada 1 revisi sebelumnya di daftar | `[✓]` | |
| 1.7 | Warga kirim revisi kedua | Status = MENUNGGU_VALIDASI | `[✓]` | |
| 1.8 | Admin approve | Status = DIVALIDASI | `[✓]` | |
| 1.9 | Lanjut hingga SELESAI (singkat) | Disposisi → Penugasan → Dikerjakan → Konfirmasi | Status = **SELESAI** | `[✓]` | |

---

## ✅ Kriteria LULUS

- [✓] Loop revisi berfungsi tanpa batasan (sistem tidak memblokir revisi ke-2)
- [✓] Riwayat revisi tercatat
- [✓] Setelah revisi kedua, alur normal berjalan

**Hasil Akhir:** `[✓] LULUS` / `[] GAGAL`  
**Catatan Bug:** Kadang kalau misalkan direfresh, kita bisa langsung masuk ke detail laporan dan langsung bisa revisi tanpa masukkan alasan
                Masih ada masalah pada form reivis laporan yang tidak bisa berfungsi dengan benar

