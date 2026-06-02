# SCN-02 — Laporan Langsung Ditolak

**Status Akhir Laporan:** `DITOLAK`  
**Aktor:** Warga → Admin Pusat → Warga  
**Estimasi Waktu:** 10–15 menit  
**Prasyarat:** Akun warga + admin pusat aktif.

---

## 🗺️ Alur Skenario

```
Warga buat laporan
    → Admin Pusat TOLAK + isi alasan
    → Warga terima notifikasi "Ditolak"
    → Status laporan = DITOLAK (final, tidak bisa diapa-apakan lagi)
```

---

## LANGKAH DETAIL

### FASE 1 — Warga Buat Laporan

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 1.1 | Login warga | `/warga/login` | - | Masuk dashboard | `[✓]` | |
| 1.2 | Buat laporan baru (isi semua field) | `/warga/create-report` | - | Laporan tersubmit | `[✓]` | |
| 1.3 | Catat ID/ticket laporan | `/warga/report-detail?id=...` | Ticket number | Status = "Menunggu" | `[✓]` | CATAT ID: _______ |

---

### FASE 2 — Admin Pusat Tolak Laporan

🔄 **Ganti ke Admin Pusat**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi                             | ✓/✗  | Catatan                                                                                                                                                                                                                   |
|---|------|-----|------------|----------------------------------------------|------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2.1 | Login admin pusat | `/admin/login` | - | Masuk dashboard                              | `[✓]` |                                                                                                                                                                                                                           |
| 2.2 | Buka panel validasi | `/admin/validation` | Laporan warga | Laporan tampil di antrian                    | `[✓]` |                                                                                                                                                                                                                           |
| 2.3 | Pilih laporan tersebut | - | Detail laporan | Semua info tampil                            | `[✓]` |                                                                                                                                                                                                                           |
| 2.4 | Pilih aksi **"Tolak"** | - | Radio/button Tolak | Tersedia                                     | `[✓]` | Harus isi alasan dulu baru bisa ditolak                                                                                                                                                                                   |
| 2.5 | ⚠️ Submit TANPA mengisi alasan | POST `/admin/validation` action=reject reason="" | Perilaku sistem | Apakah diblokir atau diproses tanpa alasan?  | `[x]` | Potensi bug ( aku gak ngerti maksud tanelnya tapi admin gak bisa tolak tanpa alasan, jadi harus isi alasan dulu baru ditolak, dan juga saat ditolak kembali redirect ke dashboard utama yang ditolak masuk ke tab ditolak |
| 2.6 | Isi alasan penolakan | - | Field alasan | Bisa diisi teks                              | `[✓]` |                                                                                                                                                                                                                           |
| 2.7 | **Submit penolakan dengan alasan** | POST `/admin/validation` | Flash message | "Laporan Ditolak"                            | `[x]` | saat laporan ditolak malah redirect ke halaman gak jelas dan juga tidak ada flasg message                                                                                                                                 |
| 2.8 | Cek laporan hilang dari antrian | `/admin/validation` | Daftar laporan | Laporan tidak ada                            | `[✓]` |                                                                                                                                                                                                                           |
| 2.9 | Cek laporan muncul di daftar ditolak | Dashboard admin → tab Ditolak | Riwayat ditolak | Laporan tampil dengan alasan penolakan       | `[✓]` |                                                                                                                                                                                                                           |

---

### FASE 3 — Verifikasi di Sisi Warga

🔄 **Kembali ke Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                       |
|---|------|-----|------------|------------------|------|-----------------------------------------------|
| 3.1 | Cek notifikasi | `/warga/notifications` | 📋 Notif baru | "Laporan Ditolak" muncul dengan alasan | `[✓]` |                                               |
| 3.2 | Cek detail laporan | `/warga/report-detail?id=...` | 📋 Status | Status = **"Ditolak"** (label merah) | `[✓]` |                                               |
| 3.3 | Cek alasan penolakan tampil | - | 📋 Alasan | Teks alasan penolakan dari admin tampil | `[✓]` |                                               |
| 3.4 | Cek tombol aksi diblokir | - | 📸 Tombol | Tidak ada tombol "Revisi", "Batalkan", atau "Konfirmasi" | `[✓]` |                                               |
| 3.5 | ⚠️ Coba batalkan laporan yang sudah ditolak | POST `/warga/withdraw-report` id=... | Response | Error "hanya dapat dibatalkan saat masih dalam antrian" | `[x]` | tiba tiba logout dan kembali ke halaman utama |
| 3.6 | Cek riwayat laporan | `/warga/report-history` | Filter "Ditolak" | Laporan muncul di tab filter Ditolak | `[✓]` |                                               |
| 3.7 | Cek dashboard stats | `/warga/dashboard` | Angka "Ditolak" | Bertambah 1 | `[✓]` |                                               |

---

## ✅ Kriteria LULUS

- [✓] Status laporan = DITOLAK setelah admin tolak
- [✓] Alasan penolakan tampil di halaman detail warga
- [✓] Notifikasi "Laporan Ditolak" diterima warga
- [✓] Tidak ada tombol aksi yang bisa diklik setelah ditolak
- [✓] Laporan muncul di tab "Ditolak" filter riwayat

**Hasil Akhir:** `[✓] LULUS` / `[ ] GAGAL`  
**Catatan Bug:** Coba cek 2.7 itu ada error_________________________________
