# SCN-15 — Admin Dinas Buat Akun Petugas Baru + Onboarding

**Aktor:** Admin Dinas → Petugas Baru  
**Estimasi Waktu:** 15 menit  
**Prasyarat:** Admin Dinas sudah login. Ada minimal 1 Region di database.

---

## 🗺️ Alur

```
Admin Dinas buka halaman kelola petugas
    → Buat akun petugas baru (nama, email, password sementara)
    → Set NIP + wilayah tugas petugas
    → Petugas login dengan password sementara
    → Sistem paksa ganti password (force change)
    → Petugas ganti password → login ulang
    → Petugas check-in → bisa akses daftar tugas
```

---

## LANGKAH DETAIL

### FASE 1 — Admin Dinas Buat Akun Petugas

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 1.1 | Login admin dinas | `/admin/login` | - | Masuk dashboard dinas | `[✓]` | |
| 1.2 | Buka halaman kelola petugas | `/admin/dinas/petugas` | Daftar petugas | Petugas dinas ini tampil | `[✓]` | |
| 1.3 | ⚠️ Submit form buat petugas dengan password 5 karakter | POST `/admin/dinas/petugas/create` | Error flash | "Password minimal 6 karakter" | `[✓]` | |
| 1.4 | ⚠️ Submit form tanpa email | POST create | Error | Validasi email required | `[✓]` | |
| 1.5 | ⚠️ Submit form dengan email yang sudah dipakai akun lain | POST create | Error | "Email sudah terdaftar" | `[✓]` | |
| 1.6 | Isi form valid: nama, email baru, password ≥6 char | - | Form valid | - | `[✓]` | Email: petugas.baru@test.com |
| 1.7 | **Submit buat petugas** | POST `/admin/dinas/petugas/create` | Flash | "Petugas [nama] berhasil dibuat" | `[✓]` | |
| 1.8 | Cek daftar petugas | `/admin/dinas/petugas` | Daftar | Petugas baru muncul di tabel | `[✓]` | |
| 1.9 | Cek status petugas | - | Kolom status | "Selesai Shift" (belum check-in) | `[✓]` | |

---

### FASE 2 — Admin Dinas Set Profil Petugas

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 2.1 | Klik edit profil petugas baru | `/admin/dinas/petugas` | Form edit profil | Field NIP + wilayah tugas tampil | `[✓]` | |
| 2.2 | Isi NIP petugas | - | Field NIP | Bisa diisi | `[✓]` | |
| 2.3 | Pilih wilayah tugas dari dropdown | - | Dropdown region | Daftar region tampil | `[✓]` | |
| 2.4 | **Submit update profil** | POST `/admin/dinas/petugas/update-profile` | Flash | "Profil petugas berhasil diperbarui" | `[✓]` | |
| 2.5 | Cek NIP + wilayah tampil di tabel | `/admin/dinas/petugas` | 📋 Data petugas | NIP dan wilayah tugas tampil | `[✓]` | |

---

### FASE 3 — Petugas Login Pertama Kali (Force Change Password)

🔄 **Petugas Baru**

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 3.1 | Login dengan email + password sementara yang dibuat admin | `/petugas/login` | Redirect | **Diarahkan ke `/petugas/change-password`** (BUKAN dashboard) | `[✗]` | Ini force change! |
| 3.2 | ⚠️ Coba akses dashboard langsung | `/petugas/dashboard` | Redirect | Diarahkan kembali ke `/petugas/change-password` | `[✗]` | |
| 3.3 | ⚠️ Coba akses halaman tugas langsung | `/petugas/tasks` | Redirect | Diarahkan ke login atau change-password | `[✗]` | |
| 3.4 | ⚠️ Isi password baru ≠ konfirmasi | POST `/petugas/change-password` | Error flash | "Konfirmasi password tidak cocok." | `[✗]` | |
| 3.5 | Isi password baru yang valid + konfirmasi sama | - | Form valid | - | `[✗]` | Password: `passwordBaru123` |
| 3.6 | **Submit ganti password** | POST `/petugas/change-password` | Flash + redirect | "Password berhasil diubah. Silakan login dengan password baru." → redirect ke `/petugas/login` | `[✗]` | |

---

### FASE 4 — Login Ulang dengan Password Baru

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 4.1 | ⚠️ Login dengan password LAMA (sementara) | POST `/petugas/login` | Error | Login gagal | `[✗]` | |
| 4.2 | Login dengan password BARU | POST `/petugas/login` | Redirect | Masuk ke `/petugas/dashboard` | `[✗]` | |
| 4.3 | Cek nama dinas di dashboard | `/petugas/dashboard` | 📋 Nama dinas | Nama dinas sesuai profil (bukan hardcoded "Dinas Pekerjaan Umum") | `[✗]` | FIX-10 |
| 4.4 | Cek statistik dashboard | - | Angka tugas | Semua = 0 (petugas baru, belum ada tugas) | `[✗]` | |
| 4.5 | ⚠️ Coba akses daftar tugas sebelum check-in | `/petugas/tasks` | Redirect | Diarahkan ke dashboard (belum check-in) | `[✗]` | FR-PTG-09 |

---

### FASE 5 — Check-in & Verifikasi Akses

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 5.1 | Klik **Check-In** di dashboard | POST `/petugas/dashboard` checkIn=true | - | Dengan koordinat yang valid (dalam radius 50km) | `[ ]` | |
| 5.2 | Cek status absensi berubah | `/petugas/dashboard` | 📋 Status | Status = "Siap Bertugas", jam check-in tampil | `[ ]` | |
| 5.3 | Akses daftar tugas setelah check-in | `/petugas/tasks` | Halaman tugas | Halaman tampil (tidak redirect ke dashboard) | `[ ]` | |
| 5.4 | Cek daftar tugas kosong | - | Tab Baru | Belum ada tugas (petugas baru) | `[ ]` | |

---

### FASE 6 — Admin Dinas Tugaskan Tugas ke Petugas Baru

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 6.1 | Admin dinas buka penugasan (ada laporan menunggu) | `/admin/dinas/penugasan` | Dropdown petugas | Petugas baru tampil di dropdown | `[ ]` | |
| 6.2 | Tugaskan laporan ke petugas baru | POST `/admin/dinas/penugasan` | - | FieldTask terbuat | `[ ]` | |
| 6.3 | Petugas cek daftar tugas | `/petugas/tasks` | Tab "Tugas Baru" | Tugas muncul | `[ ]` | |

---

## ✅ Kriteria LULUS

- [✓] Admin dinas bisa buat akun petugas
- [✓] Password minimal 6 karakter divalidasi
- [✗] Petugas baru dipaksa ganti password saat login pertama
- [✗] Tidak bisa akses halaman lain sebelum ganti password
- [✗] Setelah ganti password, login normal berjalan
- [✗] Petugas tidak bisa akses daftar tugas sebelum check-in
- ✗ Petugas baru muncul di dropdown penugasan admin dinas

**Hasil Akhir:** `[ ] LULUS` / `[✓] GAGAL`  
**Catatan Bug:**1.  Masih ada bug dropdown pilihan daerah petugas, kaena harusnya admin dinas yang sesuai dengan daerahnya gak usah lagi memilih daerah asal petgaus, conothnya admin dinas medan yah perugasnya tetap aja orang medan
                2. Masih ada masalah saat coba daftar karena nomor telpon warga dan petugas dipaksa harus beda, misalkan aku mau buat nomor telpon petugas sama dengan nomor telpon warga, maka akan muncul pesan peringatan, karena skenarionya bagaimana jika petugas bisa punya aakun warga juga ya kan
                3. Masih belum bisa masuk dengan kun petugas yang baru. initnya testing tidak bisa dijalankan dengan benar
