# CHECKLIST PENGUJIAN MANUAL â€” SISTEM ADUAJA
> Dibuat berdasarkan analisis kode sumber + SRS V1.0 Kelompok PLR  
> Tanggal: 2026-06-01 | Status: Draft untuk Pengujian Manual

---

## LEGENDA STATUS
| Simbol | Arti |
|--------|------|
| `[ ]` | Belum diuji |
| `[âœ“]` | LULUS |
| `[âœ—]` | GAGAL / Bug ditemukan |
| `[!]` | Perlu perhatian khusus |

---

## BAGIAN 1 â€” MODUL AUTENTIKASI WARGA

### 1.1 Registrasi Warga Baru
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 1.1.1 | Registrasi sukses | Buka `/warga/register`, isi semua field valid (NIK 16 digit, email baru, password â‰¥8 char, confirm password sama) | Redirect ke halaman OTP, email OTP terkirim | `[ ]` | |
| 1.1.2 | NIK kurang dari 16 digit | Isi NIK 15 digit â†’ submit | Muncul error "NIK harus 16 digit angka" | `[ ]` | |
| 1.1.3 | NIK berisi huruf | Isi NIK dengan huruf â†’ submit | Error validasi NIK | `[ ]` | |
| 1.1.4 | Password tidak match | Isi password â‰  confirm password â†’ submit | Error "Password tidak cocok" | `[ ]` | |
| 1.1.5 | Password kurang 8 karakter | Isi password 7 karakter â†’ submit | Error "Password minimal 8 karakter" | `[ ]` | |
| 1.1.6 | Email sudah terdaftar (ACTIVE) | Gunakan email yang sudah ada â†’ submit | Error "Email sudah terdaftar" | `[ ]` | |
| 1.1.7 | Email PENDING register ulang | Daftar dengan email yang statusnya PENDING | Update data + kirim OTP baru, redirect ke verify-otp | `[ ]` | FR-WRG-02 |
| 1.1.8 | Field kosong | Submit form kosong | Form tidak tersubmit / error required | `[ ]` | |

### 1.2 Verifikasi OTP
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 1.2.1 | OTP benar | Masukkan OTP yang dikirim ke email | Redirect ke login dengan pesan sukses, akun jadi ACTIVE | `[ ]` | |
| 1.2.2 | OTP salah | Masukkan OTP yang salah | Error "Kode OTP tidak valid atau sudah kadaluarsa" | `[ ]` | |
| 1.2.3 | OTP expired | Tunggu OTP expired â†’ masukkan | Error kadaluarsa | `[ ]` | |

### 1.3 Login Warga
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 1.3.1 | Login sukses | Email + password benar, akun ACTIVE | Redirect ke `/warga/dashboard` | `[ ]` | |
| 1.3.2 | Password salah | Email benar, password salah | Error "Email atau password salah" | `[ ]` | |
| 1.3.3 | Email tidak terdaftar | Email tidak ada di DB | Error "Email atau password salah" | `[ ]` | |
| 1.3.4 | Akun PENDING login | Login dengan akun belum verifikasi OTP | Warning + redirect ke halaman OTP | `[ ]` | |
| 1.3.5 | Akun SUSPENDED login | Login dengan akun suspended | Tidak bisa login | `[ ]` | |
| 1.3.6 | Role bukan WARGA | Login dengan email admin/petugas di form warga | Error "Akun ini bukan akun warga" | `[ ]` | |
| 1.3.7 | Brute force protection | Coba login salah >5x | Cek apakah ada rate limiting / lockout | `[!]` | Perlu dicek di AuthServiceImpl |

### 1.4 Lupa Password Warga
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 1.4.1 | Email terdaftar | Masukkan email valid â†’ kirim | OTP dikirim, redirect ke halaman verify | `[ ]` | |
| 1.4.2 | Email tidak terdaftar | Masukkan email tidak ada | Tetap redirect ke verify (security: tidak bocorkan info email) | `[ ]` | |
| 1.4.3 | Reset password sukses | OTP benar + password baru â‰¥8 char | Password berhasil diubah, redirect ke login | `[ ]` | |
| 1.4.4 | Password baru < 8 karakter | Submit password baru 7 char | Error "Password minimal 8 karakter" | `[ ]` | |
| 1.4.5 | Confirm password tidak match | newPassword â‰  confirmNewPassword | Error "Password baru dan konfirmasi tidak cocok" | `[ ]` | |
| 1.4.6 | OTP salah saat reset | Masukkan OTP salah | Error invalid OTP | `[ ]` | |

### 1.5 Logout Warga
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 1.5.1 | Logout normal | Klik logout | Session invalidate, redirect ke login | `[ ]` | |
| 1.5.2 | Akses dashboard setelah logout | Setelah logout â†’ akses `/warga/dashboard` langsung | Redirect ke login | `[ ]` | |

---

## BAGIAN 2 â€” PROFIL WARGA

| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 2.1 | Tampil profil | Akses `/warga/profile` saat login | Data nama, email, NIK, alamat tampil | `[ ]` | |
| 2.2 | Edit profil sukses | Ubah nama/nomor HP â†’ save | Profil berhasil diperbarui, session userName ikut terupdate | `[ ]` | |
| 2.3 | Upload foto profil | Upload file foto (jpg/png) | Foto terupload ke Supabase, URL tersimpan | `[ ]` | |
| 2.4 | Upload file bukan gambar | Upload file PDF/exe | Gagal / error handling | `[ ]` | |
| 2.5 | Ganti password sukses | Password lama benar + password baru â‰¥8 char | Password berhasil diubah | `[ ]` | |
| 2.6 | Ganti password lama salah | Masukkan password lama yang salah | Error "Password saat ini salah" | `[ ]` | |
| 2.7 | Ganti password baru < 8 char | Password baru 7 karakter | Error "Password baru minimal 8 karakter" | `[ ]` | |
| 2.8 | Akses profil tanpa login | Akses `/warga/profile` tanpa sesi | Redirect ke login | `[ ]` | |

---

## BAGIAN 3 â€” MODUL LAPORAN WARGA

### 3.1 Buat Laporan Baru
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 3.1.1 | Buat laporan sukses | Isi semua field (kategori, deskripsi, lokasi, foto, koordinat) â†’ submit | Laporan tersimpan, status MENUNGGU_VALIDASI, redirect ke detail, notif ke admin | `[ ]` | FR-WRG-10 |
| 3.1.2 | Foto dari kamera | Gunakan kamera browser untuk ambil foto | Foto ter-capture, bisa disubmit | `[ ]` | |
| 3.1.3 | Foto base64 terupload ke Supabase | Submit laporan dengan foto | URL Supabase tersimpan di DB (bukan base64 mentah) | `[ ]` | |
| 3.1.4 | Tanpa foto | Submit laporan tanpa foto | Apakah diizinkan? Cek validasi | `[!]` | Perlu dicek di template |
| 3.1.5 | Koordinat GPS dari browser | Klik "Gunakan Lokasi Saya" | Koordinat lat/lng terisi otomatis | `[ ]` | |
| 3.1.6 | Region/wilayah dipilih | Pilih wilayah dari dropdown | regionId tersimpan | `[ ]` | |
| 3.1.7 | Buat laporan tanpa login | Akses `/warga/create-report` tanpa sesi | Redirect ke login | `[ ]` | |
| 3.1.8 | Ticket number otomatis | Setelah laporan dibuat | Ticket number ter-generate (format unik) | `[ ]` | |

### 3.2 Riwayat Laporan
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 3.2.1 | Lihat semua riwayat | Akses `/warga/report-history` | Semua laporan milik user tampil | `[ ]` | |
| 3.2.2 | Filter berdasarkan status | Pilih filter "Menunggu" | Hanya laporan berstatus Menunggu tampil | `[ ]` | |
| 3.2.3 | Filter "Perlu Revisi" | Pilih filter Perlu Revisi | Laporan perlu revisi tampil | `[ ]` | |
| 3.2.4 | Filter "Ditolak" | Pilih filter Ditolak | Laporan ditolak tampil | `[ ]` | |
| 3.2.5 | Search by ticket number | Ketik nomor tiket di search box | Laporan terkait tampil | `[ ]` | |
| 3.2.6 | Pagination | Jika laporan > 10 | Navigasi halaman berfungsi | `[ ]` | |
| 3.2.7 | Laporan warga lain tidak tampil | Login sebagai warga A | Laporan warga B tidak muncul | `[ ]` | |

### 3.3 Detail Laporan
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 3.3.1 | Lihat detail laporan | Klik laporan dari riwayat | Detail lengkap tampil (ticket, status, foto, deskripsi, SLA deadline) | `[ ]` | |
| 3.3.2 | SLA deadline tampil | Laporan sudah di-disposisi | SLA deadline tampil dengan countdown | `[ ]` | |
| 3.3.3 | Laporan ID orang lain | Akses `/warga/report-detail?id=MILIK_ORANG_LAIN` | Redirect / akses ditolak | `[!]` | Bug potensial â€” cek di kode |

### 3.4 Revisi Laporan
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 3.4.1 | Revisi saat PERLU_REVISI | Laporan berstatus Perlu Revisi â†’ submit revisi | Status kembali ke MENUNGGU_VALIDASI | `[ ]` | FR-WRG-18, FR-WRG-19 |
| 3.4.2 | Revisi saat bukan PERLU_REVISI | Coba revisi laporan MENUNGGU/DIVALIDASI | Error "Revisi hanya dapat dilakukan saat status Perlu Revisi" | `[ ]` | |
| 3.4.3 | Revisi dengan foto baru | Upload foto baru saat revisi | Foto baru tersimpan | `[ ]` | |
| 3.4.4 | Revisi dengan koordinat baru | Update koordinat saat revisi | Koordinat baru tersimpan | `[ ]` | |

### 3.5 Batalkan Laporan
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 3.5.1 | Batalkan saat MENUNGGU_VALIDASI | Klik batalkan di laporan berstatus Menunggu | Status jadi DITOLAK "Dibatalkan oleh pelapor", redirect ke history | `[ ]` | FR-WRG-23 |
| 3.5.2 | Batalkan saat sudah DIVALIDASI | Coba batalkan laporan yang sudah divalidasi | Error "hanya dapat dibatalkan saat masih dalam antrian verifikasi" | `[ ]` | |
| 3.5.3 | Batalkan laporan orang lain | Manipulasi ID laporan milik orang lain | Error "tidak berwenang" | `[ ]` | |

### 3.6 Konfirmasi Laporan Selesai
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 3.6.1 | Konfirmasi pertama kali | Laporan berstatus MENUNGGU_KONFIRMASI â†’ klik Terima | Status jadi SELESAI | `[ ]` | FR-RSL-03 |
| 3.6.2 | Konfirmasi kedua kali (sudah locked) | Coba konfirmasi yang sudah pernah dikonfirmasi | Error "Konfirmasi laporan ini sudah pernah dilakukan" | `[ ]` | FR-RSL-07 |
| 3.6.3 | Konfirmasi laporan orang lain | Manipulasi ID | Error "Anda tidak berwenang" | `[ ]` | |
| 3.6.4 | Status jadi read-only setelah SELESAI | Lihat laporan SELESAI | Tombol aksi tidak muncul (isFinalStatus=true) | `[ ]` | FR-RSL-08 |

### 3.7 Ajukan Sengketa
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 3.7.1 | Ajukan sengketa dengan alasan | Laporan SELESAI â†’ ajukan sengketa + alasan | Status laporan jadi SENGKETA, sengketa tersimpan | `[ ]` | |
| 3.7.2 | Ajukan sengketa dengan foto bukti | Upload foto bukti sengketa | Foto terupload ke Supabase, URL tersimpan | `[ ]` | |
| 3.7.3 | Ajukan sengketa tanpa alasan | Submit form sengketa kosong | Validasi gagal | `[ ]` | |
| 3.7.4 | Sengketa laporan bukan milik sendiri | Manipulasi reportId | Error atau redirect | `[!]` | Perlu dicek validasi ownership |

### 3.8 Notifikasi Warga
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 3.8.1 | Notif muncul setelah laporan divalidasi | Admin validasi laporan | Notifikasi "Laporan Divalidasi" muncul di warga | `[ ]` | |
| 3.8.2 | Notif saat laporan ditolak | Admin tolak laporan | Notif "Laporan Ditolak" muncul | `[ ]` | |
| 3.8.3 | Notif saat perlu revisi | Admin minta revisi | Notif "Laporan Perlu Revisi" muncul | `[ ]` | |
| 3.8.4 | Mark all as read | Klik "Tandai semua dibaca" | Semua notif termark read | `[ ]` | |
| 3.8.5 | Filter notif belum dibaca | Pilih filter "belum-dibaca" | Hanya notif belum dibaca tampil | `[ ]` | |
| 3.8.6 | Badge unread count | Ada notif belum dibaca | Badge angka muncul di navbar | `[ ]` | |
## BAGIAN 4 â€” MODUL ADMIN PUSAT

### 4.1 Login Admin Pusat
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 4.1.1 | Login sukses | Email + password admin pusat â†’ login | Redirect ke `/admin/dashboard` | `[ ]` | |
| 4.1.2 | Akses dashboard tanpa login | Akses `/admin/dashboard` langsung | Redirect ke `/admin/login` | `[ ]` | |
| 4.1.3 | Warga coba akses admin | Session warga â†’ akses URL admin | Redirect ke login | `[ ]` | |

### 4.2 Dashboard Admin Pusat
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 4.2.1 | Statistik tampil | Akses dashboard | 4 angka statistik tampil: Laporan Masuk, Menunggu Validasi, Dalam Antrean Dinas, Selesai | `[ ]` | |
| 4.2.2 | Panel navigasi | Dashboard | 5 panel tampil: Antrean, Validasi, Merge, Disposisi, Sengketa | `[ ]` | |
| 4.2.3 | Antrean laporan (FIFO) | Lihat tab antrean | Laporan diurutkan dari yang terlama | `[ ]` | FR-ADM-01 |

### 4.3 Validasi Laporan
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 4.3.1 | Lihat antrian validasi | Akses `/admin/validation` | Laporan MENUNGGU_VALIDASI tampil | `[ ]` | |
| 4.3.2 | Approve laporan | Pilih laporan â†’ approve | Status â†’ DIVALIDASI, notif ke warga, redirect ke disposisi | `[ ]` | FR-ADM-05 |
| 4.3.3 | Minta revisi | Pilih laporan â†’ revision + isi catatan | Status â†’ PERLU_REVISI, notif ke warga berisi catatan | `[ ]` | FR-ADM-06 |
| 4.3.4 | Tolak laporan | Pilih laporan â†’ reject + isi alasan | Status â†’ DITOLAK, notif ke warga berisi alasan | `[ ]` | FR-ADM-07 |
| 4.3.5 | Tolak tanpa alasan | Submit reject tanpa isi reason | Cek apakah ada validasi wajib isi alasan | `[!]` | |
| 4.3.6 | Laporan ditolak muncul di riwayat | Setelah ditolak | Laporan tampil di tab "Laporan Ditolak" dashboard | `[ ]` | |
| 4.3.7 | Audit trail setelah validasi | Lihat detail laporan setelah approved | Log "divalidasi oleh [admin]" tampil di timeline | `[ ]` | FR-RSL-22 |

### 4.4 Merge Tiket Duplikat
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 4.4.1 | Lihat daftar tiket merge | Akses `/admin/merge` | Semua tiket MENUNGGU_VALIDASI tampil (kecuali child merge) | `[ ]` | FR-ADM-11 |
| 4.4.2 | Pilih 2+ tiket untuk merge | Centang 2 tiket â†’ pilih parent â†’ isi alasan | Merge berhasil | `[ ]` | FR-ADM-14 |
| 4.4.3 | Alasan merge < 20 karakter | Isi alasan kurang 20 char | Error "shortReason" | `[ ]` | |
| 4.4.4 | Pilih hanya 1 tiket | Centang 1 tiket â†’ submit | Tidak diproses | `[ ]` | |
| 4.4.5 | Parent tiket bukan dari yang dipilih | Submit merge tanpa pilih parent | Error "noParent" | `[ ]` | |
| 4.4.6 | Tiket sudah DIDISPOSISI tidak bisa di-merge | Coba merge tiket DIDISPOSISI | Error "blocked" | `[ ]` | |
| 4.4.7 | Child tiket tersembunyi di antrean | Setelah merge | Child tidak muncul di antrean validasi | `[ ]` | |
| 4.4.8 | Pisahkan tiket (unmerge) | Klik pisahkan di cluster | Cluster terpecah, tiket kembali ke antrean | `[ ]` | |
| 4.4.9 | Skor similaritas tampil | Cluster merge | Angka similarity score tampil | `[ ]` | |

### 4.5 Disposisi ke Dinas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 4.5.1 | Lihat laporan siap disposisi | Akses `/admin/disposisi` | Laporan berstatus DIVALIDASI tampil | `[ ]` | |
| 4.5.2 | Disposisi dengan pilih dinas | Pilih laporan â†’ pilih dinas â†’ set prioritas â†’ deadline â†’ instruksi â†’ submit | Status â†’ DIDISPOSISI, SLA record dibuat | `[ ]` | FR-DSP-01 |
| 4.5.3 | Prioritas Kritis = SLA 24 jam | Set prioritas Kritis | SLA deadline = now + 24h | `[ ]` | FR-DSP-03 |
| 4.5.4 | Prioritas Tinggi = SLA 48 jam | Set prioritas Tinggi | SLA deadline = now + 48h | `[ ]` | |
| 4.5.5 | Prioritas Sedang = SLA 72 jam | Set prioritas Sedang | SLA deadline = now + 72h | `[ ]` | |
| 4.5.6 | Prioritas Rendah = SLA 120 jam | Set prioritas Rendah | SLA deadline = now + 120h | `[ ]` | |
| 4.5.7 | Dinas difilter sesuai region laporan | Laporan dari wilayah X | Hanya dinas di wilayah X tampil | `[ ]` | FR-DSP-02 |
| 4.5.8 | Disposisi tanpa pilih dinas | Submit tanpa memilih dinas | Gagal atau dinas null | `[!]` | |
| 4.5.9 | SLA sudah ada tidak dibuat ulang | Disposisi laporan yang sudah punya SLA | SLA record tidak terduplikasi | `[ ]` | |

### 4.6 Kelola Sengketa (Admin Pusat)
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 4.6.1 | Lihat daftar sengketa | Akses `/admin/sengketa` | Sengketa pending (resolution=null) tampil | `[ ]` | FR-RSL-09 |
| 4.6.2 | Resolve: Tugaskan Kembali | Pilih sengketa â†’ keputusan "tugaskan_kembali" | Resolution = TUGASKAN_KEMBALI, status laporan â†’ DITUGASKAN | `[ ]` | FR-RSL-11 |
| 4.6.3 | Resolve: Tutup Laporan | Pilih sengketa â†’ keputusan lain | Resolution = TUTUP_LAPORAN, status laporan â†’ DITUTUP | `[ ]` | FR-RSL-12 |
| 4.6.4 | Sengketa filter per region | Admin pusat dengan regionId | Hanya sengketa di region terkait tampil | `[ ]` | |

### 4.7 Monitoring SLA
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 4.7.1 | Lihat halaman SLA | Akses `/admin/sla` | Statistik SLA tampil + daftar semua SLA record | `[ ]` | |
| 4.7.2 | Item terlambat tampil | Ada SLA yang melewati deadline | Muncul di daftar "lateItems" | `[ ]` | |
| 4.7.3 | Status SLA TERLAMBAT | SLA deadline terlewat | `currentStatus` = TERLAMBAT | `[ ]` | |

---

## BAGIAN 5 â€” MODUL ADMIN DINAS

### 5.1 Login Admin Dinas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 5.1.1 | Login admin dinas | Email + password admin dinas â†’ login | Redirect ke `/admin/dinas/dashboard` | `[ ]` | |
| 5.1.2 | Session agencyId tersimpan | Setelah login | Session memiliki `agencyId` dan `agencyName` | `[ ]` | |

### 5.2 Dashboard Admin Dinas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 5.2.1 | Statistik dinas tampil | Akses dashboard | Laporan Diterima, Tugas Baru, Dalam Penanganan, Selesai tampil | `[ ]` | |
| 5.2.2 | Laporan disposisi ke dinas ini | Dashboard | Hanya laporan yang didisposisi ke dinas ini tampil | `[ ]` | |
| 5.2.3 | Daftar petugas dinas ini | Dashboard | Hanya petugas milik dinas ini tampil | `[ ]` | |

### 5.3 Antrean Laporan Dinas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 5.3.1 | Lihat antrean | Akses `/admin/dinas/queue` | Daftar laporan yang didisposisi ke dinas ini | `[ ]` | |
| 5.3.2 | Pagination antrean | Jika laporan > 10 | Navigasi halaman berfungsi | `[ ]` | |

### 5.4 Penugasan Petugas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 5.4.1 | Lihat laporan belum ditugaskan | Akses `/admin/dinas/penugasan` | Laporan yang belum ada FieldTask tampil | `[ ]` | |
| 5.4.2 | Tugaskan petugas | Pilih laporan â†’ pilih petugas â†’ submit | FieldTask dibuat, status laporan â†’ DITUGASKAN | `[ ]` | FR-PTG-01 |
| 5.4.3 | Laporan sudah ditugaskan hilang | Setelah assign | Laporan tidak tampil lagi di antrean penugasan | `[ ]` | |
| 5.4.4 | Daftar petugas hanya dari dinas ini | Dropdown petugas | Hanya petugas dengan agencyId yang sama | `[ ]` | |

### 5.5 Monitoring Progress
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 5.5.1 | Lihat tugas sedang dikerjakan | Akses `/admin/dinas/progress` | Tugas berstatus SEDANG_DIKERJAKAN tampil | `[ ]` | |
| 5.5.2 | Pause SLA | POST `/admin/dinas/pause-sla` dengan taskId + reason | SLA status â†’ TERTUNDA, SlaPauseLog dibuat | `[ ]` | FR-JDA |
| 5.5.3 | Resume SLA | POST `/admin/dinas/resume-sla` dengan taskId | SLA dilanjutkan, deadline diperpanjang sesuai durasi pause | `[ ]` | |
| 5.5.4 | Deadline tampil | Lihat progress | Deadline SLA tampil | `[ ]` | |

### 5.6 Close Tiket
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 5.6.1 | Lihat tiket siap close | Akses `/admin/dinas/close` | Tiket berstatus SELESAI tampil | `[ ]` | |
| 5.6.2 | Close tiket | Submit close | Status laporan â†’ MENUNGGU_KONFIRMASI (cek di FieldTaskService) | `[ ]` | |

### 5.7 Kelola Sengketa (Admin Dinas)
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 5.7.1 | Lihat sengketa | Akses `/admin/dinas/sengketa` | Sengketa pending tampil | `[ ]` | |
| 5.7.2 | Terima sengketa + reassign petugas | Pilih petugas baru â†’ submit "diterima" | Petugas berganti, sengketa resolved TUGASKAN_KEMBALI | `[ ]` | |
| 5.7.3 | Tolak sengketa | Submit keputusan bukan "diterima" | Sengketa resolved TUTUP_LAPORAN | `[ ]` | |

### 5.8 Kelola Petugas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 5.8.1 | Buat akun petugas baru | Isi form buat petugas (nama, email, password â‰¥6 char) | Akun petugas dibuat, tersimpan dengan role PETUGAS | `[ ]` | |
| 5.8.2 | Password petugas < 6 char | Submit password 5 karakter | Error "Password minimal 6 karakter" | `[ ]` | |
| 5.8.3 | Update NIP + wilayah tugas petugas | Update profil petugas | NIP dan wilayah tersimpan | `[ ]` | |
| 5.8.4 | Petugas baru wajib ganti password | Login petugas baru | Diarahkan ke halaman ganti password paksa | `[ ]` | |

---

## BAGIAN 6 â€” MODUL PETUGAS LAPANGAN

### 6.1 Login Petugas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 6.1.1 | Login petugas | Email + password petugas â†’ login | Redirect ke dashboard petugas | `[ ]` | |
| 6.1.2 | Force change password pertama kali | Login petugas baru | Redirect ke `/petugas/change-password` | `[ ]` | |
| 6.1.3 | Confirm password tidak cocok saat force change | Submit dengan konfirmasi berbeda | Error "Konfirmasi password tidak cocok" | `[ ]` | |

### 6.2 Absensi (Check-in/out)
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 6.2.1 | Check-in dalam radius | Check-in dengan koordinat dalam radius 50km | Check-in berhasil, shift mulai | `[ ]` | FR-PTG-08 |
| 6.2.2 | Check-in di luar radius | Check-in dengan koordinat di luar radius | Error geofencing, tidak bisa check-in | `[ ]` | |
| 6.2.3 | Check-out | Klik check-out | Shift selesai, checkOutAt tersimpan | `[ ]` | |
| 6.2.4 | Istirahat (break) | Klik istirahat | Status shift â†’ ISTIRAHAT | `[ ]` | |
| 6.2.5 | Lanjut dari istirahat (resume) | Klik lanjut setelah istirahat | Status shift â†’ AKTIF | `[ ]` | |
| 6.2.6 | Akses daftar tugas sebelum check-in | Akses `/petugas/tasks` tanpa check-in | Redirect ke dashboard | `[ ]` | FR-PTG-09 |

### 6.3 Dashboard Petugas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 6.3.1 | Statistik tugas tampil | Akses dashboard | Selesai, Sedang, Baru, Tertunda tampil | `[ ]` | |
| 6.3.2 | Info absensi tampil | Dashboard | Status check-in, jam masuk, durasi kerja tampil | `[ ]` | |
| 6.3.3 | Nama dinas dari profil | Dashboard | Nama dinas dari UserProfile.domisiliRegion, bukan hardcoded | `[ ]` | |

### 6.4 Daftar Tugas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 6.4.1 | Tugas terkelompok | Akses `/petugas/tasks` | Tugas terbagi: Baru, Sedang Dikerjakan, Tertunda | `[ ]` | |
| 6.4.2 | Sorting cerdas (SLA + jarak) | Daftar tugas baru | Tugas urut berdasarkan SLA mendesak + jarak terdekat | `[ ]` | FR-PTG-10 |

### 6.5 Detail & Aksi Tugas
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 6.5.1 | Lihat detail tugas | Klik salah satu tugas | Detail tampil: deskripsi, kategori, lokasi, koordinat, status history | `[ ]` | |
| 6.5.2 | Koordinat dari laporan (bukan petugas) | Lihat lokasi di detail tugas | Koordinat mengacu ke lokasi laporan, bukan lokasi officer | `[ ]` | FIX-1 |
| 6.5.3 | Mulai tugas dalam radius 10km | POST start dengan koordinat dekat lokasi | Tugas mulai, status â†’ SEDANG_DIKERJAKAN | `[ ]` | FR-PTG-18 |
| 6.5.4 | Mulai tugas di luar radius 10km | POST start dengan koordinat jauh | Error jarak gagal | `[ ]` | |
| 6.5.5 | Selesaikan tugas | POST action=complete | Status tugas â†’ SELESAI | `[ ]` | |
| 6.5.6 | Ajukan penundaan | POST action=postpone + alasan + estimasi waktu | Pengajuan penundaan tersimpan, menunggu persetujuan admin | `[ ]` | FR-PTG-27 |
| 6.5.7 | Penundaan tidak langsung TERTUNDA | Setelah submit postpone | Status tetap belum TERTUNDA sampai admin approve | `[ ]` | FIX-8 |
| 6.5.8 | Lapor balik invalid | POST action=reportback + deskripsi | Log invalid tersimpan, admin perlu review | `[ ]` | |
| 6.5.9 | Eskalasi tugas | POST action=escalation + deskripsi | Eskalasi tersimpan | `[ ]` | |

### 6.6 Eksekusi Tugas (Upload Foto Bukti)
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 6.6.1 | Upload foto SEBELUM | Akses `/petugas/task-execution` â†’ ambil foto â†’ save | Evidence tipe SEBELUM tersimpan | `[ ]` | FR-PTG-19 |
| 6.6.2 | Redirect ke step AFTER | Setelah upload SEBELUM | Otomatis ke step=after | `[ ]` | |
| 6.6.3 | Upload foto SESUDAH + selesaikan | Upload foto SESUDAH â†’ complete | Evidence SESUDAH tersimpan, tugas SELESAI, redirect ke dashboard | `[ ]` | |
| 6.6.4 | Watermark pada foto bukti | Cek foto yang tersimpan | Foto memiliki watermark (ditangani di service layer) | `[ ]` | FIX-6 |
| 6.6.5 | Selesaikan tanpa foto SESUDAH | Submit complete tanpa foto | Tugas tetap bisa selesai (foto opsional saat complete) | `[ ]` | |

### 6.7 Riwayat Tugas & Statistik
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 6.7.1 | Lihat riwayat | Akses `/petugas/history` | Tugas SELESAI tampil beserta foto before/after count | `[ ]` | |
| 6.7.2 | Detail riwayat | Klik salah satu riwayat | Foto before & after tampil, durasi pengerjaan tampil | `[ ]` | |
| 6.7.3 | Statistik performa | Akses `/petugas/reports` | Total tugas, selesai, pending, jam kerja, completion rate tampil | `[ ]` | |
| 6.7.4 | Filter performa per minggu | Pilih period=week | Grafik per hari 7 hari terakhir | `[ ]` | |
| 6.7.5 | Filter performa per bulan | Pilih period=month | Grafik per minggu 30 hari terakhir | `[ ]` | |

### 6.8 Riwayat Absensi
| No | Test Case | Langkah | Expected Result | Status | Catatan |
|----|-----------|---------|-----------------|--------|---------|
| 6.8.1 | Lihat riwayat absensi | Akses `/petugas/attendance-history` | Semua shift tersimpan tampil | `[ ]` | |
| 6.8.2 | Data kosong jika belum pernah check-in | Petugas baru belum pernah check-in | List kosong tanpa error | `[ ]` | |
## BAGIAN 7 â€” SKENARIO END-TO-END (ALUR LENGKAP)

### 7.1 Alur Normal: Laporan Berhasil Diselesaikan
```
Warga buat laporan â†’ Admin pusat validasi â†’ Disposisi ke dinas â†’
Admin dinas tugaskan petugas â†’ Petugas check-in â†’ Petugas start tugas â†’
Upload foto sebelum â†’ Upload foto sesudah â†’ Complete tugas â†’
Admin dinas close tiket â†’ Warga konfirmasi â†’ Status SELESAI
```
| No | Step | Expected | Status |
|----|------|----------|--------|
| 7.1.1 | Warga submit laporan | Status MENUNGGU_VALIDASI, notif ke admin | `[ ]` |
| 7.1.2 | Admin approve | Status DIVALIDASI | `[ ]` |
| 7.1.3 | Admin disposisi | Status DIDISPOSISI, SLA terbuat | `[ ]` |
| 7.1.4 | Admin dinas tugaskan petugas | Status DITUGASKAN, FieldTask terbuat | `[ ]` |
| 7.1.5 | Petugas check-in | Shift aktif | `[ ]` |
| 7.1.6 | Petugas start tugas | Status SEDANG_DIKERJAKAN | `[ ]` |
| 7.1.7 | Petugas upload foto SEBELUM | Evidence SEBELUM tersimpan | `[ ]` |
| 7.1.8 | Petugas upload foto SESUDAH + selesai | Status task SELESAI | `[ ]` |
| 7.1.9 | Admin dinas close tiket | Status laporan â†’ MENUNGGU_KONFIRMASI | `[ ]` |
| 7.1.10 | Warga terima konfirmasi | Status â†’ SELESAI | `[ ]` |
| 7.1.11 | SLA status â†’ SELESAI | SLA diselesaikan sebelum deadline | `[ ]` |

### 7.2 Alur: Laporan Ditolak
| No | Step | Expected | Status |
|----|------|----------|--------|
| 7.2.1 | Warga submit laporan | Status MENUNGGU_VALIDASI | `[ ]` |
| 7.2.2 | Admin pusat tolak + isi alasan | Status DITOLAK, notif warga berisi alasan | `[ ]` |
| 7.2.3 | Laporan tampil di tab "Ditolak" warga | Status label "Ditolak" | `[ ]` |
| 7.2.4 | Warga tidak bisa aksi lagi | Tombol revisi/batalkan tidak muncul | `[ ]` |

### 7.3 Alur: Laporan Perlu Revisi â†’ Revisi Dikirim
| No | Step | Expected | Status |
|----|------|----------|--------|
| 7.3.1 | Admin minta revisi + catatan | Status PERLU_REVISI, notif berisi catatan | `[ ]` |
| 7.3.2 | Warga lihat catatan revisi | Catatan admin tampil di detail laporan | `[ ]` |
| 7.3.3 | Warga submit revisi | Status â†’ MENUNGGU_VALIDASI | `[ ]` |
| 7.3.4 | Admin validasi ulang | Laporan muncul kembali di antrean validasi | `[ ]` |

### 7.4 Alur: SLA Terlewat (Terlambat)
| No | Step | Expected | Status |
|----|------|----------|--------|
| 7.4.1 | Disposisi dengan SLA deadline dekat | SLA record dibuat | `[ ]` |
| 7.4.2 | Waktu melebihi deadline (cek scheduler) | `checkAndUpdateOverdueSla()` mengubah status â†’ TERLAMBAT | `[ ]` |
| 7.4.3 | SLA monitoring admin | Laporan muncul di daftar "lateItems" | `[ ]` |
| 7.4.4 | Status terlambat tampil di warga | Countdown di detail laporan warga | `[ ]` |

### 7.5 Alur: Sengketa
| No | Step | Expected | Status |
|----|------|----------|--------|
| 7.5.1 | Laporan status SELESAI | Warga tidak puas | `[ ]` |
| 7.5.2 | Warga ajukan sengketa + alasan + foto | Status laporan â†’ SENGKETA | `[ ]` |
| 7.5.3 | Admin pusat/dinas lihat sengketa | Sengketa tampil di panel sengketa | `[ ]` |
| 7.5.4 | Resolve: Tugaskan kembali | Status laporan â†’ DITUGASKAN, petugas baru | `[ ]` |
| 7.5.5 | Resolve: Tutup laporan | Status laporan â†’ DITUTUP | `[ ]` |
| 7.5.6 | Warga tidak bisa ajukan sengketa lagi | Setelah resolved, laporan DITUTUP | `[ ]` |

### 7.6 Alur: Tiket Duplikat Digabung
| No | Step | Expected | Status |
|----|------|----------|--------|
| 7.6.1 | 2 warga laporkan masalah sama lokasi | 2 laporan MENUNGGU_VALIDASI | `[ ]` |
| 7.6.2 | Admin merge kedua tiket | MergeRecord dibuat, child tersembunyi | `[ ]` |
| 7.6.3 | Admin validasi parent | Parent divalidasi, child otomatis tergabung | `[ ]` |
| 7.6.4 | Disposisi hanya untuk parent | Hanya 1 disposisi yang dibuat | `[ ]` |

### 7.7 Alur: Penundaan Tugas (Dengan Persetujuan)
| No | Step | Expected | Status |
|----|------|----------|--------|
| 7.7.1 | Petugas ajukan penundaan | TaskPostponement record dibuat | `[ ]` |
| 7.7.2 | Status tugas belum TERTUNDA | Masih SEDANG_DIKERJAKAN sampai admin approve | `[ ]` |
| 7.7.3 | Admin dinas approve penundaan | Status â†’ TERTUNDA | `[ ]` |
| 7.7.4 | SLA di-pause oleh admin dinas | SLA status â†’ TERTUNDA, pause log dibuat | `[ ]` |
| 7.7.5 | Admin resume SLA | Deadline diperpanjang sejumlah durasi pause | `[ ]` |

### 7.8 Alur: Koreksi Koordinat Laporan
| No | Test Case | Expected | Status |
|----|-----------|----------|--------|
| 7.8.1 | Petugas koreksi koordinat laporan | Koordinat laporan diperbarui | `[ ]` |
| 7.8.2 | Koreksi ke-2 diblokir | Field `coordinateCorrected=true` â†’ error | `[ ]` | FR-PTG-17 |

---

## BAGIAN 8 â€” PENGUJIAN KEAMANAN & EDGE CASE

### 8.1 Akses Kontrol
| No | Test Case | Expected | Status |
|----|-----------|----------|--------|
| 8.1.1 | Warga akses URL admin | Redirect ke `/admin/login` | `[ ]` |
| 8.1.2 | Admin akses URL warga | Cek perilaku, kemungkinan loop redirect | `[!]` |
| 8.1.3 | Petugas akses URL admin | Redirect ke login | `[ ]` |
| 8.1.4 | Akses semua URL tanpa session | Redirect ke halaman login masing-masing | `[ ]` |
| 8.1.5 | Admin dinas tanpa agencyId | Cek fallback behavior | `[!]` |

### 8.2 Validasi Input
| No | Test Case | Expected | Status |
|----|-----------|----------|--------|
| 8.2.1 | Script injection di deskripsi laporan | `<script>alert('xss')</script>` di field | Thymeleaf auto-escape, tidak execute | `[ ]` |
| 8.2.2 | Input sangat panjang | Text field 10.000+ karakter | Tidak crash, truncated atau error | `[!]` |
| 8.2.3 | Koordinat tidak valid (string) | Submit lat/lng berupa huruf | Parse error ditangkap | `[ ]` |

### 8.3 Session & Concurrent
| No | Test Case | Expected | Status |
|----|-----------|----------|--------|
| 8.3.1 | Session expired | Tunggu session habis â†’ akses halaman | Redirect ke login | `[ ]` |
| 8.3.2 | Login dari 2 tab berbeda | Buka 2 tab, login di tab 1 | Apakah session konflik? | `[!]` |

---

## BAGIAN 9 â€” PENGUJIAN UI & UX

### 9.1 Responsivitas
| No | Test Case | Expected | Status |
|----|-----------|----------|--------|
| 9.1.1 | Tampilan mobile (375px) | Semua halaman warga readable di mobile | `[ ]` |
| 9.1.2 | Tampilan tablet (768px) | Layout tidak broken | `[ ]` |
| 9.1.3 | Tampilan desktop (1920px) | Layout full-width normal | `[ ]` |

### 9.2 Pesan & Feedback
| No | Test Case | Expected | Status |
|----|-----------|----------|--------|
| 9.2.1 | Flash message success | Setelah aksi berhasil | Pesan hijau muncul dan hilang | `[ ]` |
| 9.2.2 | Flash message error | Setelah aksi gagal | Pesan merah muncul | `[ ]` |
| 9.2.3 | Loading state saat submit | Submit form berat | Ada indikator loading | `[ ]` |

### 9.3 Navigasi
| No | Test Case | Expected | Status |
|----|-----------|----------|--------|
| 9.3.1 | Navbar warga | Dashboard, Laporan Baru, Riwayat, Notifikasi, Profil | `[ ]` |
| 9.3.2 | Navbar admin pusat | Antrean, Validasi, Merge, Disposisi, Sengketa, SLA | `[ ]` |
| 9.3.3 | Navbar petugas | Dashboard, Tugas, Riwayat, Laporan, Absensi | `[ ]` |
| 9.3.4 | Breadcrumb/back button | Navigasi mundur berfungsi | `[ ]` |

---

## BAGIAN 10 â€” KESESUAIAN DENGAN SRS

### 10.1 Functional Requirements Warga (FR-WRG)
| Kode FR | Deskripsi | Diimplementasikan | Berfungsi | Catatan |
|---------|-----------|-------------------|-----------|---------|
| FR-WRG-01 | Registrasi akun warga | âœ“ | `[ ]` | |
| FR-WRG-02 | Re-registrasi email PENDING | âœ“ | `[ ]` | |
| FR-WRG-10 | Buat laporan dengan foto + GPS | âœ“ | `[ ]` | |
| FR-WRG-18 | Revisi laporan status PERLU_REVISI | âœ“ | `[ ]` | |
| FR-WRG-19 | Status kembali MENUNGGU setelah revisi | âœ“ | `[ ]` | |
| FR-WRG-23 | Batalkan laporan saat MENUNGGU_VALIDASI | âœ“ | `[ ]` | |

### 10.2 Functional Requirements Admin Pusat (FR-ADM)
| Kode FR | Deskripsi | Diimplementasikan | Berfungsi | Catatan |
|---------|-----------|-------------------|-----------|---------|
| FR-ADM-01 | Antrean laporan FIFO | âœ“ | `[ ]` | |
| FR-ADM-05 | Approve laporan | âœ“ | `[ ]` | |
| FR-ADM-06 | Minta revisi laporan | âœ“ | `[ ]` | |
| FR-ADM-07 | Tolak laporan + alasan | âœ“ | `[ ]` | |
| FR-ADM-11 | Deteksi laporan duplikat | âœ“ | `[ ]` | Similarity score |
| FR-ADM-14 | Merge tiket duplikat | âœ“ | `[ ]` | |

### 10.3 Functional Requirements Disposisi (FR-DSP)
| Kode FR | Deskripsi | Diimplementasikan | Berfungsi | Catatan |
|---------|-----------|-------------------|-----------|---------|
| FR-DSP-01 | Disposisi ke dinas | âœ“ | `[ ]` | |
| FR-DSP-02 | Filter dinas per region | âœ“ | `[ ]` | |
| FR-DSP-03 | SLA berdasarkan prioritas | âœ“ | `[ ]` | |

### 10.4 Functional Requirements Petugas (FR-PTG)
| Kode FR | Deskripsi | Diimplementasikan | Berfungsi | Catatan |
|---------|-----------|-------------------|-----------|---------|
| FR-PTG-01 | Penugasan petugas dari admin dinas | âœ“ | `[ ]` | |
| FR-PTG-08 | Geofencing check-in | âœ“ | `[ ]` | |
| FR-PTG-09 | Gate: akses tugas hanya jika check-in | âœ“ | `[ ]` | |
| FR-PTG-10 | Sorting tugas (SLA + GPS) | âœ“ | `[ ]` | |
| FR-PTG-17 | Koreksi koordinat max 1x | âœ“ | `[ ]` | |
| FR-PTG-18 | Validasi jarak sebelum mulai tugas | âœ“ | `[ ]` | |
| FR-PTG-19 | Upload foto bukti before/after | âœ“ | `[ ]` | |
| FR-PTG-27 | Penundaan tugas dengan approval | âœ“ | `[ ]` | |

### 10.5 Functional Requirements Resolusi (FR-RSL)
| Kode FR | Deskripsi | Diimplementasikan | Berfungsi | Catatan |
|---------|-----------|-------------------|-----------|---------|
| FR-RSL-03 | Konfirmasi hanya oleh pembuat laporan | âœ“ | `[ ]` | |
| FR-RSL-07 | Konfirmasi hanya bisa 1x (locked) | âœ“ | `[ ]` | |
| FR-RSL-08 | Status final = read-only | âœ“ | `[ ]` | |
| FR-RSL-09 | Panel sengketa admin | âœ“ | `[ ]` | |
| FR-RSL-11 | Resolve sengketa: tugaskan kembali | âœ“ | `[ ]` | |
| FR-RSL-12 | Resolve sengketa: tutup laporan | âœ“ | `[ ]` | |
| FR-RSL-22 | Audit trail / timeline tiket | âœ“ | `[ ]` | |

---

## BAGIAN 11 â€” BUG YANG PERLU DIVERIFIKASI

> Berdasarkan analisis kode, berikut area yang berpotensi bug atau perlu verifikasi khusus:

| No | Area | Deskripsi Potensi Bug | Cara Test | Status |
|----|------|----------------------|-----------|--------|
| B-01 | `WargaController.wargaReportDetail` | Tidak ada validasi ownership â€” warga bisa akses detail laporan milik orang lain via manipulasi ID | Akses `/warga/report-detail?id=LAPORAN_ORANG_LAIN` | `[ ]` |
| B-02 | `AdminDinasController.adminDinasPauseSla` | Setelah `pauseSla()`, task status di-set TERTUNDA tapi tidak di-save ke DB (`getTaskById()` tidak trigger save) | Pause SLA â†’ cek DB apakah task status benar-benar TERTUNDA | `[ ]` |
| B-03 | `AdminPusatController` disposisi redirect | Setelah disposisi sukses, redirect ke `?tab=queue` bukan ke tab disposisi | Submit disposisi â†’ perhatikan tab yang aktif setelah redirect | `[ ]` |
| B-04 | SLA tidak dibuat jika laporan tidak ada prioritas | Jika admin tidak set prioritas saat disposisi, SLA pakai default 120h | Disposisi tanpa pilih prioritas â†’ cek SLA record | `[ ]` |
| B-05 | `WargaController.disputeReport` | Tidak ada validasi bahwa warga hanya bisa sengketa laporan miliknya sendiri | Manipulasi reportId saat ajukan sengketa | `[ ]` |
| B-06 | `AdminDinasController.adminDinasSengketaPost` | Hardcoded fallback email `admin.pu@aduaja.go.id` jika session tidak ada adminId | Test sengketa tanpa session yang valid | `[!]` |
| B-07 | `ConfirmationService.respond` | Jika tidak ada ConfirmationRequest, fallback langsung update ke SELESAI tanpa membuat ConfirmationRequest | Close tiket dari admin dinas â†’ cek apakah ConfirmationRequest terbuat | `[ ]` |
| B-08 | Petugas bisa akses task-detail orang lain | Tidak ada validasi bahwa task.officer.userId == sessionUserId | Akses `/petugas/task-detail?id=TUGAS_PETUGAS_LAIN` | `[ ]` |

---

## BAGIAN 12 â€” CHECKLIST PERSIAPAN DATA UJI

Sebelum mulai testing, pastikan data berikut tersedia di database:

| No | Data yang Dibutuhkan | Keterangan |
|----|---------------------|-----------|
| D-01 | 1 akun Warga ACTIVE | Untuk testing semua skenario warga |
| D-02 | 1 akun Admin Pusat ACTIVE | Dengan/tanpa regionId untuk test filter |
| D-03 | 1 akun Admin Dinas ACTIVE | Dengan agencyId yang valid |
| D-04 | 1 akun Petugas ACTIVE | Terdaftar di dinas yang sama dengan Admin Dinas |
| D-05 | Minimal 3 kategori laporan | Aktif (`isActive=true`) |
| D-06 | Minimal 2 region/wilayah | Untuk test filter region |
| D-07 | Minimal 1 Agency (Dinas) | Terhubung dengan Admin Dinas |
| D-08 | Email server tersambung | Untuk test OTP (atau mock SMTP) |
| D-09 | Supabase terkonfigurasi | Untuk test upload foto |

---

## RINGKASAN TOTAL TEST CASE

| Bagian | Judul | Jumlah TC |
|--------|-------|-----------|
| 1 | Autentikasi Warga | 25 |
| 2 | Profil Warga | 8 |
| 3 | Laporan Warga | 34 |
| 4 | Admin Pusat | 32 |
| 5 | Admin Dinas | 23 |
| 6 | Petugas | 30 |
| 7 | End-to-End Scenarios | 40 |
| 8 | Keamanan & Edge Case | 10 |
| 9 | UI & UX | 12 |
| 10 | Kesesuaian SRS | 25 |
| 11 | Bug Verification | 8 |
| **TOTAL** | | **~247 test case** |

---

*Checklist ini dibuat berdasarkan analisis kode sumber AduAja dan SRS V1.0 Kelompok PLR*  
*Dibuat: 2026-06-01 | Reviewer: _____________ | Tanggal Mulai Testing: _____________*
