# SCN-14 — Lupa Password → Reset via OTP

**Aktor:** Warga (punya akun ACTIVE)  
**Estimasi Waktu:** 10 menit

---

## LANGKAH DETAIL

| # | Aksi | URL | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------------|-----|---------|
| 1.1 | Buka halaman login | `/warga/login` | Link "Lupa Password" ada | `[✓]` | |
| 1.2 | Klik "Lupa Password" | `/warga/forgot-password` | Form input email | `[✗]` | |
| 1.3 | ⚠️ Masukkan email tidak terdaftar | POST `/warga/forgot-password` | Redirect ke verify (tidak bocorkan info) | `[]` | Security: jangan bilang "email tidak ada" |
| 1.4 | Masukkan email terdaftar | POST `/warga/forgot-password` | OTP terkirim, redirect ke verify | `[ ]` | |
| 1.5 | Cek email | Inbox | Email OTP reset password | `[ ]` | |
| 1.6 | ⚠️ Masukkan OTP salah | `/warga/verify-otp-reset` | Error OTP | `[ ]` | |
| 1.7 | Masukkan OTP benar | - | Form password baru muncul | `[ ]` | |
| 1.8 | ⚠️ Password baru < 8 karakter | - | Error | "Password minimal 8 karakter" | `[ ]` | |
| 1.9 | ⚠️ Confirm password tidak cocok | - | Error | "Password baru tidak cocok" | `[ ]` | |
| 1.10 | Isi password baru valid + confirm | POST reset | Flash sukses, redirect login | `[ ]` | |
| 1.11 | Login dengan password lama | `/warga/login` | Error | Login gagal (password lama tidak valid) | `[ ]` | |
| 1.12 | Login dengan password baru | `/warga/login` | Berhasil masuk dashboard | `[ ]` | |

---

## ✅ Kriteria LULUS

- [ ] OTP dikirim ke email yang terdaftar
- [ ] Email tidak terdaftar tidak memberi pesan error yang jelas
- [ ] Password baru berhasil diset dan bisa login
- [ ] Password lama tidak bisa dipakai lagi

**Hasil Akhir:** `[ ] LULUS` / `[✓] GAGAL`  
**Catatan Bug:** Tidak bisa masuk ke halaman lupa password sehingga tidak dapat melakukan testing dengan benar. 


---

