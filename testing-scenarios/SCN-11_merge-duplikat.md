# SCN-11 — Merge 2 Tiket Duplikat → Selesai

**Status Akhir Laporan:** Parent = `SELESAI`, Child = disembunyikan  
**Aktor:** Warga A + Warga B (submit laporan serupa) → Admin Pusat (merge) → Admin Dinas + Petugas → SELESAI  
**Estimasi Waktu:** 40 menit  
**Prasyarat:** 2 akun warga. Laporan dibuat di lokasi dan kategori yang mirip.

---

## 🗺️ Alur

```
Warga A & Warga B submit laporan yang mirip (lokasi + kategori sama)
    → Admin Pusat buka panel Merge
    → Sistem deteksi similarity score
    → Admin centang kedua tiket → pilih parent → isi alasan merge
    → Merge berhasil: child tersembunyi, parent tetap aktif
    → Admin validasi parent → disposisi → (alur normal)
    → Parent laporan = SELESAI
    → Warga B lihat laporan child-nya (status merged)
```

---

## LANGKAH DETAIL

### FASE 1 — 2 Warga Submit Laporan Serupa

| # | Aktor | Aksi | Hasil Ekspektasi | ✓/✗ | Catatan                                  |
|---|-------|------|------------------|-----|------------------------------------------|
| 1.1 | Warga A | Login + buat laporan | Kategori: Jalan. Lokasi: [koordinat X]. Deskripsi: "Jalan berlubang besar dekat SD" | `[✓]` | ID_A: _______                            |
| 1.2 | Warga B | Login + buat laporan | Kategori: Jalan. Lokasi: [koordinat X±0.001]. Deskripsi: "Jalan bolong dekat sekolah dasar" | `[✓]` | ID_B: _______                            |
| 1.3 | Cek keduanya di admin | `/admin/validation` | Kedua laporan tampil | Status keduanya = MENUNGGU_VALIDASI | `[STATUSNYA SEBENARNYA MENADI DITERIMA]` | |

---

### FASE 2 — Admin Deteksi & Merge

🔄 **Admin Pusat**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 2.1 | Buka panel merge | `/admin/merge` | Daftar tiket | Kedua laporan tampil | `[✓]` | |
| 2.2 | Cek similarity score | - | 📋 Skor | Skor similarity tampil (angka 0–100 atau persen) | `[✓]` | |
| 2.3 | Centang laporan A | - | Checkbox | Tercentang | `[✓]` | |
| 2.4 | Centang laporan B | - | Checkbox | Tercentang | `[✓]` | |
| 2.5 | ⚠️ Submit merge tanpa isi alasan | POST merge reason="" | Error/validasi | "Alasan minimal 20 karakter" | `[✓]` | |
| 2.6 | ⚠️ Submit merge tanpa pilih parent | POST merge tanpa parentId | Error | "noParent" — pilih tiket utama | `[✓]` | |
| 2.7 | Pilih laporan A sebagai parent | - | Radio parent | A terpilih sebagai parent | `[✓]` | |
| 2.8 | Isi alasan merge ≥ 20 karakter | - | Field alasan | Terisi | `[✓]` | |
| 2.9 | **Submit merge** | POST `/admin/merge` | Flash message | "Tiket berhasil digabungkan" | `[✓]` | |

---

### FASE 3 — Verifikasi Hasil Merge

| # | Aktor | Aksi | URL | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|-------|------|-----|------------------|-----|---------|
| 3.1 | Admin Pusat | Cek panel merge | `/admin/merge` | Laporan B (child) **tidak muncul** di daftar utama | `[✓]` | |
| 3.2 | Admin Pusat | Cek antrian validasi | `/admin/validation` | Hanya laporan A (parent) yang tampil | `[✓]` | |
| 3.3 | Warga B | Cek detail laporan B | `/warga/report-detail?id=ID_B` | Status = "Digabung" atau ada info "laporan ini telah digabung" | `[ ]` | |
| 3.4 | Warga B | Cek riwayat laporan | `/warga/report-history` | Laporan B tampil dengan status merged | `[✗]` | |

---

### FASE 4 — Validasi Parent & Lanjut Normal

🔄 **Admin Pusat**

| # | Aksi | Hasil Ekspektasi | ✓/✗                                                                                       |
|---|------|------------------|-------------------------------------------------------------------------------------------|
| 4.1 | Approve laporan A (parent) | Status = DIVALIDASI | `[✓]`                                                                                     |
| 4.2 | Disposisi laporan A ke dinas | Hanya 1 disposisi (bukan 2) | `[✗]` Cuman hanya satu yang terikirim yaitu si parent                                     |
| 4.3 | Admin dinas tugaskan petugas | 1 FieldTask untuk laporan A | `[✓]`                                                                                     |
| 4.4 | Petugas kerjakan → selesai | Status task = SELESAI | `[✓]Tapi halaman petugas tidak dapat melihat foto laporan atau seluruh deskripsi laporan` |
| 4.5 | Warga A konfirmasi | Status laporan A = **SELESAI** | `[✓] tapi satus di riwayat lapoan masih salah karena gak lengkap atau tidak seusia SRS`   |

---

### FASE 5 — Uji Pisah Tiket (Unmerge)

| # | Aktor | Aksi | URL | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|-------|------|-----|------------------|-----|---------|
| 5.1 | Admin | Buat merge baru (ulang dari fase 1) | `/admin/merge` | 2 tiket baru dimerge | `[✓]` | Gunakan laporan baru |
| 5.2 | Admin | Klik "Pisahkan" / unmerge di cluster | `/admin/merge` | Cluster terpecah | `[✓]` | |
| 5.3 | Admin | Cek kedua laporan kembali di antrian | `/admin/validation` | Keduanya tampil sebagai independen | `[✓]` | |

---

## ✅ Kriteria LULUS

- [✓] Skor similarity ter-deteksi
- [✓] Merge membutuhkan alasan ≥ 20 karakter dan parent terpilih
- [✓] Child tiket tersembunyi dari antrian setelah merge
- [✓] Hanya 1 disposisi dibuat (untuk parent saja)
- [✗] Warga B masih bisa melihat status laporan B di riwayatnya
- [✓] Unmerge berfungsi dan kedua tiket kembali ke antrian

**Hasil Akhir:** `[ ] LULUS` / `[✓] GAGAL`  
**Catatan Bug:** Masih Banyak Masalah terutama pada saat menggabungkan laporan, status dari laporan warga baik dia yang jadi parent maupun tidak parent tidak berubah dengan benar. dan sewaktu di dispossikan laporan yang digabung, laporan child malah tidak ikut dikirimkan
