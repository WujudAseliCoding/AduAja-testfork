# SCN-01 — Happy Path: Laporan Selesai Normal

**Status Akhir Laporan:** `SELESAI`  
**Aktor:** Warga → Admin Pusat → Admin Dinas → Petugas → Warga  
**Estimasi Waktu Testing:** 30–45 menit  
**Prasyarat:** Akun warga, admin pusat, admin dinas, petugas sudah ada. Minimal 1 kategori laporan aktif. Petugas sudah terdaftar di dinas yang sama dengan admin dinas.

---

## 🗺️ Alur Skenario

```
Warga buat laporan
    → Admin Pusat validasi (approve)
    → Admin Pusat disposisi ke dinas
    → Admin Dinas tugaskan petugas
    → Petugas check-in
    → Petugas mulai tugas
    → Petugas upload foto sebelum
    → Petugas upload foto sesudah + selesaikan
    → (Otomatis) Status laporan → MENUNGGU_KONFIRMASI
    → Warga konfirmasi selesai
    → Status laporan → SELESAI
```

---

## LANGKAH DETAIL

### FASE 1 — Warga Membuat Laporan

**Login sebagai Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 1.1 | Buka halaman login warga | `/warga/login` | Form login tampil | Form login dan register tampil | `[✓]` | |
| 1.2 | Login dengan kredensial warga | POST `/warga/login` | Redirect setelah login | Masuk ke dashboard warga | `[✓]` | |
| 1.3 | Cek dashboard | `/warga/dashboard` | 📋 Statistik laporan | Angka total/menunggu/diproses/selesai/ditolak tampil | `[✓]` | |

**Buat Laporan Baru**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                          |
|---|------|-----|------------|------------------|------|--------------------------------------------------|
| 1.4 | Klik "Buat Laporan" | `/warga/create-report` | Form laporan tampil | Kategori, deskripsi, peta, kamera tersedia | `[✓]` |                                                  |
| 1.5 | Pilih kategori | - | Dropdown kategori | Daftar kategori aktif tampil | `[✓]` |                                                  |
| 1.6 | Isi deskripsi | - | Field deskripsi | Bisa diketik | `[✓]` |                                                  |
| 1.7 | Klik "Gunakan Lokasi Saya" | - | Koordinat lat/lng | Koordinat terisi otomatis dari GPS browser | `[✓]` |                                                  |
| 1.8 | Isi location hint / patokan | - | Field patokan | Bisa diketik | `[✓]` |                                                  |
| 1.9 | Ambil foto via kamera atau upload | - | Preview foto | Foto tampil di preview sebelum submit | `[✓]` |                                                  |
| 1.10 | Pilih wilayah (region) | - | Dropdown region | Daftar wilayah tampil | `[x]` | Tidak perlu karena sudah otomatis deteksi lokasi |
| 1.11 | Submit laporan | POST `/warga/create-report` | Flash message + redirect | "Laporan berhasil dikirim!", redirect ke detail laporan | `[✓]` |                                                  |

**Verifikasi Setelah Submit**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗   | Catatan                                                                                                        |
|---|------|-----|------------|------------------|-------|----------------------------------------------------------------------------------------------------------------|
| 1.12 | Cek halaman detail laporan | `/warga/report-detail?id=...` | 📋 Status laporan | Status = **"Menunggu"** (MENUNGGU_VALIDASI) | `[✓]` |                                                                                                                |
| 1.13 | Cek ticket number | - | Nomor tiket | Ticket number ter-generate (format unik) | `[✓]` |                                                                                                                |
| 1.14 | Cek foto tersimpan | - | Foto di detail | Foto laporan tampil (URL Supabase, bukan base64) | `[x]` | di h2-console sudah ada linknya yang bisa aku download gambarnya tapi di bucket supabase tidak ada/belum masuk |
| 1.15 | Cek riwayat laporan | `/warga/report-history` | 📋 Daftar laporan | Laporan baru muncul dengan status "Menunggu" | `[✓]` |                                                                                                                |
| 1.16 | Cek dashboard stats berubah | `/warga/dashboard` | Angka "Menunggu" | Bertambah 1 | `[✓]` |                                                                                                                |

---

### FASE 2 — Admin Pusat Validasi

🔄 **Ganti ke akun Admin Pusat** (buka tab baru atau logout warga)

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗   | Catatan                                                                                  |
|---|------|-----|------------|------------------|-------|------------------------------------------------------------------------------------------|
| 2.1 | Login admin pusat | `/admin/login` | Login berhasil | Masuk ke dashboard | `[✓]` |                                                                                          |
| 2.2 | Cek notifikasi laporan baru | Dashboard | 📋 Statistik "Laporan Masuk" | Bertambah | `[✓]` |                                                                                          |
| 2.3 | Buka panel validasi | `/admin/validation` | Daftar laporan | Laporan dari warga tampil di antrian | `[✓]` |                                                                                          |
| 2.4 | Klik laporan yang baru dibuat | `/admin/validation?id=...` | 📋 Detail laporan | Foto, koordinat, deskripsi, kategori tampil | `[✓]` |                                                                                          |
| 2.5 | Lihat foto laporan | - | Foto tampil | Foto dari Supabase ter-render | `[x]` |                                                                                          |
| 2.6 | Lihat koordinat di peta | - | Peta / koordinat | Koordinat benar sesuai yang disubmit warga | `[✓]` |                                                                                          |
| 2.7 | **Approve laporan** | POST `/admin/validation` action=approve | Flash message | "Laporan Divalidasi", redirect ke tab disposisi | `[x]` | Sudah redirect ke tab disposisi, cuma tidak ada flash message langsung ke tab disposisi  |
| 2.8 | Cek laporan hilang dari antrian validasi | `/admin/validation` | Daftar laporan | Laporan tidak ada lagi di antrian | `[✓]` |                                                                                          |

🔄 **Kembali ke halaman Warga** (buka tab warga atau login ulang)

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 2.9 | Cek notifikasi warga | `/warga/notifications` | 📋 Notifikasi baru | "Laporan Divalidasi" muncul | `[✓]` | |
| 2.10 | Cek detail laporan | `/warga/report-detail?id=...` | Status laporan | Status berubah jadi **"Divalidasi"** | `[✓]` | |

---

### FASE 3 — Admin Pusat Disposisi ke Dinas

🔄 **Kembali ke Admin Pusat**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗   | Catatan                                                |
|---|------|-----|------------|------------------|-------|--------------------------------------------------------|
| 3.1 | Buka panel disposisi | `/admin/disposisi` | Daftar laporan tervalidasi | Laporan tadi tampil | `[✓]` |                                                        |
| 3.2 | Pilih laporan | - | Detail laporan | Info laporan tampil di panel kanan | `[✓]` |                                                        |
| 3.3 | Pilih dinas tujuan | - | Dropdown dinas | Daftar dinas di wilayah laporan tampil | `[✓]` |                                                        |
| 3.4 | Set prioritas = **"Tinggi"** | - | Pilih prioritas | Tersedia: Kritis/Tinggi/Sedang/Rendah | `[✓]` |                                                        |
| 3.5 | Set deadline | - | Input datetime | Bisa dipilih tanggal + jam | `[✓]` |                                                        |
| 3.6 | Isi instruksi/catatan | - | Textarea instruksi | Bisa diisi | `[✓]` |                                                        |
| 3.7 | **Submit disposisi** | POST `/admin/disposisi` | Flash message | "Laporan berhasil didisposisikan ke dinas" | `[x]` | Tidak ada flash message tapi laporan terkirim ke dinas |
| 3.8 | ⚠️ Cek SLA record dibuat | DB / `/admin/sla` | SLA monitoring | SLA muncul dengan deadline sesuai prioritas Tinggi = 48 jam | `[x]` | Tidak ada SLA muncul ( SLA belum ada )                 |

🔄 **Cek halaman Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan                                                                              |
|---|------|-----|------------|------------------|-----|--------------------------------------------------------------------------------------|
| 3.9 | Cek detail laporan warga | `/warga/report-detail?id=...` | Status laporan | Status berubah ke **"Didisposisi"** | `[✓]` |                                                                                      |
| 3.10 | Cek SLA deadline tampil | - | 📋 Countdown SLA | Batas waktu SLA tampil di halaman detail | `[✓]` | Sudah tampil di detail halaman warga, tetapi di detail halaman admin pusat belum ada |

---

### FASE 4 — Admin Dinas Tugaskan Petugas

🔄 **Login sebagai Admin Dinas**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                                                                                                                                                                                                                                                                                                                                                                                     |
|---|------|-----|------------|------------------|------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 4.1 | Login admin dinas | `/admin/login` | Login berhasil | Dashboard admin dinas muncul | `[✓]` |                                                                                                                                                                                                                                                                                                                                                                                                             |
| 4.2 | Cek dashboard dinas | `/admin/dinas/dashboard` | Statistik "Laporan Diterima" | Bertambah 1 | `[✓]` |                                                                                                                                                                                                                                                                                                                                                                                                             |
| 4.3 | Buka halaman penugasan | `/admin/dinas/penugasan` | Daftar laporan belum ditugaskan | Laporan dari disposisi tampil | `[✓]` |                                                                                                                                                                                                                                                                                                                                                                                                             |
| 4.4 | Pilih laporan | - | Detail laporan | Foto, deskripsi, lokasi, instruksi admin tampil | `[✓]` |                                                                                                                                                                                                                                                                                                                                                                                                             |
| 4.5 | Lihat daftar petugas tersedia | - | Dropdown petugas | Hanya petugas dinas ini yang tampil | `[x]` | Sudah ada daftar petugas hanya saja petugas yang check in itu seharusnya dia beri lokasi dulu, jika lokasi sama baru petugas itu namanya ditampilkan di daftar petugas, yang sekarang jika petugas check in maka namanya bisa dipilih padahal lokasi tidak sesuai, seharusnya tidak boleh muncul nama dia sebelum dia konfirmasi dulu lokasinya, jika sesuai tengan lokasi laporan baru ditampilkan namanya |
| 4.6 | Pilih petugas | - | Pilih 1 petugas | Nama petugas terpilih | `[✓]` |                                                                                                                                                                                                                                                                                                                                                                                                             |
| 4.7 | **Submit penugasan** | POST `/admin/dinas/penugasan` | Halaman terefresh | Laporan hilang dari daftar belum ditugaskan | `[✓]` |                                                                                                                                                                                                                                                                                                                                                                                                             |

🔄 **Cek halaman Warga lagi**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗   | Catatan                                                              |
|---|------|-----|------------|------------------|-------|----------------------------------------------------------------------|
| 4.8 | Cek status di riwayat warga | `/warga/report-history` | Status | Berubah ke **"Ditugaskan"** | `[x]` | Walaupun sudah ditugaskan statusnya masih didisposisi, coba perbaiki |

---

### FASE 5 — Petugas Kerjakan Tugas

🔄 **Login sebagai Petugas**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                                                                                                 |
|---|------|-----|------------|------------------|------|-------------------------------------------------------------------------------------------------------------------------|
| 5.1 | Login petugas | `/petugas/login` | Login | Masuk ke dashboard petugas | `[✓]` |                                                                                                                         |
| 5.2 | Cek dashboard petugas | `/petugas/dashboard` | Statistik tugas | Angka "Tugas Baru" = 1 | `[✓]` |                                                                                                                         |
| 5.3 | Cek tidak bisa akses tugas sebelum check-in | `/petugas/tasks` | Redirect | Diarahkan kembali ke dashboard | `[v]` |                                                                                                                         |
| 5.4 | **Check-in** (dengan koordinat) | POST `/petugas/dashboard` checkIn=true | - | Check-in berhasil, status "Siap Bertugas" | `[✓]` |                                                                                                                         |
| 5.5 | Verifikasi status absensi | `/petugas/dashboard` | 📋 Info absensi | Jam check-in tampil, status "Siap Bertugas" | `[✓]` |                                                                                                                         |
| 5.6 | Buka daftar tugas | `/petugas/tasks` | Daftar tugas | Tugas muncul di tab "Tugas Baru" | `[✓]` |                                                                                                                         |
| 5.7 | Klik tugas | `/petugas/task-detail?id=...` | Detail tugas | Deskripsi, kategori, lokasi laporan, koordinat tampil | `[✓]` |                                                                                                                         |
| 5.8 | Cek koordinat = lokasi laporan | - | 📋 Koordinat | Koordinat mengarah ke lokasi laporan (bukan kantor petugas) | `[✓]` | Sudah pas hanya saja waktu klik buka di google maps agak melenceng, padahal di detail tugas jarak dari anda sudah valid |
| 5.9 | **Mulai tugas** (dengan koordinat) | POST `/petugas/task-action` action=start | Flash/redirect | Tugas mulai, status → SEDANG_DIKERJAKAN | `[✓]` |                                                                                                                         |
| 5.10 | Cek tugas pindah ke tab "Sedang Dikerjakan" | `/petugas/tasks` | Tab in-progress | Tugas muncul di tab sedang dikerjakan | `[✓]` |                                                                                                                         |

**Upload Bukti Foto**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 5.11 | Buka halaman eksekusi tugas | `/petugas/task-execution?id=...&step=before` | Form upload foto | Step "Sebelum" tampil | `[✓]` | |
| 5.12 | Ambil/upload foto SEBELUM | - | Preview foto | Foto tampil di preview | `[✓]` | |
| 5.13 | **Submit foto sebelum** | POST action=save | Redirect ke step after | Redirect ke `step=after` | `[✓]` | |
| 5.14 | Upload foto SESUDAH | - | Preview foto | Foto tampil di preview | `[✓]` | |
| 5.15 | **Submit selesaikan tugas** | POST action=complete | Redirect dashboard | Redirect ke `/petugas/dashboard` | `[✓]` | |

---

### FASE 6 — Konfirmasi Warga & Status Final

🔄 **Cek Admin Dinas**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 6.1 | Cek tugas di tab close | `/admin/dinas/close` | Daftar tiket siap close | Tugas muncul (status FieldTask = SELESAI) | `[✓]` | |

🔄 **Cek Warga**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗  | Catatan                                                                                 |
|---|------|-----|------------|------------------|------|-----------------------------------------------------------------------------------------|
| 6.2 | Cek detail laporan | `/warga/report-detail?id=...` | Status laporan | Status = **"Menunggu Konfirmasi"** | `[✓]` |                                                                                         |
| 6.3 | Lihat tombol konfirmasi | - | 📸 Tombol aksi | Tombol "Konfirmasi Selesai" muncul | `[✓]` |                                                                                         |
| 6.4 | Cek deadline konfirmasi | - | Countdown | Batas 72 jam tampil | `[✓]` |                                                                                         |
| 6.5 | **Klik Konfirmasi Selesai** | POST `/warga/confirm-report` | Flash message | "Terima kasih! Laporan telah dikonfirmasi selesai." | `[✓]` |                                                                                         |
| 6.6 | Cek status laporan berubah | `/warga/report-detail?id=...` | 📋 Status | Status = **"Selesai"** | `[✓]` |                                                                                         |
| 6.7 | Cek tombol aksi hilang | - | 📸 Tombol | Tombol konfirmasi/sengketa tidak bisa diklik lagi (locked) | `[✓]` |                                                                                         |
| 6.8 | Cek dashboard warga | `/warga/dashboard` | Angka "Selesai" | Bertambah 1 | `[✓]` |                                                                                         |
| 6.9 | Cek SLA | `/admin/sla` (login admin) | SLA status | SLA status = SELESAI | `[x]` | Laporan yang sudah selesai hilang tidak ada riwayatnya, jadi statusnya gak bisa dilihat |
| 6.10 | Cek riwayat petugas | `/petugas/history` | Tugas selesai | Tugas muncul di riwayat dengan foto before/after | `[✓]` |                                                                                         |

---

## ✅ Kriteria LULUS Skenario

- [x] Status laporan mengikuti alur: MENUNGGU → DIVALIDASI → DIDISPOSISI → DITUGASKAN → SEDANG_DIKERJAKAN → MENUNGGU_KONFIRMASI → SELESAI |di halaman admin pusat laporan yang sudah selesai hilang, jadi status selesai tidak bisa dilihat|
- [✓] Setiap perubahan status ternotifikasi ke warga
- [✓] SLA dibuat saat disposisi dan diselesaikan dengan benar
- [✓] Foto bukti (before/after) tersimpan dengan watermark
- [✓] Konfirmasi hanya bisa dilakukan 1x (locked setelah dikonfirmasi)

**Hasil Akhir:** `[ ] LULUS` / `[x] GAGAL`  
**Catatan Bug:** 
coba kamu baca di penjelasannku, itu ada beberapa yang belum sesuai seperti di halaman admin pusat tidak ada riwayat laporan yang sudah selesai dikerjakan jadi kita tidak tau statusnya padahal sudah selesai, juga aku tidak tau ini masalah browserku atau gimana tapi kalau halaman warga/admin dll aku biarkan beberapa menit pas aku klik langsung logout
_________________________________
