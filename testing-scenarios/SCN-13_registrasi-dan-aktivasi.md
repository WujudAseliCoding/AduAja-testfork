# SCN-13 — Registrasi Warga Baru → Aktivasi OTP → Login Pertama

**Status Akhir Akun:** `ACTIVE`  
**Aktor:** Warga baru (belum punya akun sama sekali)  
**Estimasi Waktu:** 10–15 menit  
**Prasyarat:** Email server berfungsi (atau mock SMTP). Siapkan email yang belum pernah didaftarkan.

---

## 🗺️ Alur

```
Buka halaman register
    → Isi form: NIK 16 digit, nama lengkap, email baru, password, konfirmasi password
    → Akun dibuat dengan status PENDING
    → OTP dikirim ke email
    → Buka email → salin kode OTP
    → Masukkan OTP di halaman verifikasi
    → Akun status menjadi ACTIVE
    → Login pertama kali berhasil
    → Cek dashboard warga baru (semua angka = 0)
```

---

## LANGKAH DETAIL

### FASE 1 — Uji Validasi Form Registrasi

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗                                                                          | Catatan |
|---|------|-----|------------|------------------|------------------------------------------------------------------------------|---------|
| 1.1 | Buka halaman register | `/warga/register` | Form tampil | Field: NIK, nama, email, password, konfirmasi tersedia | `[✓]`                                                                        | |
| 1.2 | ⚠️ Submit form kosong | POST `/warga/register` | Validasi | Form tidak tersubmit / error required di semua field | `[✗] Tidak ada pesan di semua field tapi cuman satu saja filed yang tampil pesan` | |
| 1.3 | ⚠️ NIK 15 digit (kurang 1) | Isi NIK: `123456789012345` | Error | "NIK harus 16 digit angka" | `[✓]`                                                                         | |
| 1.4 | ⚠️ NIK berisi huruf | Isi NIK: `1234ABCD12345678` | Error | Error validasi NIK | `[✓]`                                                                         | |
| 1.5 | ⚠️ Password kurang 8 karakter | Isi password: `abc123` (6 char) | Error | "Password minimal 8 karakter" | `[✓]`                                                                         | |
| 1.6 | ⚠️ Password tidak cocok | Password: `abcd1234` / Konfirmasi: `abcd5678` | Error | "Password tidak cocok" / "Konfirmasi password tidak sesuai" | `[✓]`                                                                         | |
| 1.7 | ⚠️ Email format salah | Isi: `bukanEmail` | Error | Error format email | `[✓]`                                                                         | |

---

### FASE 2 — Registrasi Berhasil

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗                                               | Catatan |
|---|------|-----|------------|------------------|---------------------------------------------------|---------|
| 2.1 | Isi semua field dengan data valid | `/warga/register` | - | - | `[✓]`                                             | Email: baru@test.com, NIK: 3201234567890001 |
| 2.2 | **Submit registrasi** | POST `/warga/register` | Redirect | Redirect ke halaman verifikasi OTP | `[✓]`                                             | |
| 2.3 | Cek halaman OTP | `/warga/verify-otp` | Form OTP | Form input kode OTP tampil | `[✓]`                                             | |
| 2.4 | Cek email inbox | Email baru@test.com | Email OTP | Email berisi kode OTP diterima | `[✓] tapi sangat lama membuat user gak nyaman menunggu` | |
| 2.5 | Cek akun di DB (opsional) | DB | Status akun | `status = 'PENDING'` saat belum verifikasi | `[✓]`                                              | |

---

### FASE 3 — Verifikasi OTP

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 3.1 | ⚠️ Masukkan OTP yang salah | POST `/warga/verify-otp` | Error | "Kode OTP tidak valid atau sudah kadaluarsa" | `[✓]` | |
| 3.2 | ⚠️ Masukkan OTP kosong | Submit kosong | Validasi | Error field required | `[✓]` | |
| 3.3 | Masukkan OTP yang benar (dari email) | POST `/warga/verify-otp` | Flash + redirect | "Akun berhasil diverifikasi! Silakan login." | `[✓]` | |
| 3.4 | Redirect ke halaman login | `/warga/login` | Halaman | Halaman login tampil | `[✓]` | |
| 3.5 | Cek akun di DB (opsional) | DB | Status akun | `status = 'ACTIVE'` setelah OTP berhasil | `[✓]` | |

---

### FASE 4 — Login Pertama Kali

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 4.1 | ⚠️ Login dengan password yang salah | POST `/warga/login` | Error | "Email atau password salah" | `[✓]` | |
| 4.2 | Login dengan email + password yang benar | POST `/warga/login` | Redirect | Masuk ke `/warga/dashboard` | `[✓]` | |
| 4.3 | Cek dashboard warga baru | `/warga/dashboard` | 📋 Statistik | Semua angka = 0 (belum ada laporan) | `[✓]` | |
| 4.4 | Cek profil tampil | `/warga/profile` | 📋 Data profil | Nama dan email tampil sesuai yang didaftarkan | `[✓]` | |

---

### FASE 5 — Uji Edge Case Registrasi Ulang

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗ | Catatan |
|---|------|-----|------------|------------------|-----|---------|
| 5.1 | ⚠️ Register ulang dengan email yang sama (ACTIVE) | `/warga/register` → POST | Error | "Email sudah terdaftar" | `[✓]` | |
| 5.2 | ⚠️ Register dengan email PENDING (belum OTP) | Daftar baru dengan email yg belum verifikasi | Perilaku sistem | Update data akun + kirim OTP baru (bukan error!) | `[✓]` | FR-WRG-02 |
| 5.3 | Verifikasi OTP baru (setelah register ulang dengan email PENDING) | `/warga/verify-otp` | Flash | OTP baru berfungsi | `[✓]` | |

---

## ✅ Kriteria LULUS

- [✓] Validasi form berfungsi (NIK 16 digit, password ≥8 char, email format)
- [✓] Setelah submit, akun dibuat dengan status PENDING
- [✓] OTP dikirim ke email
- [✓] OTP salah → error, OTP benar → akun ACTIVE
- [✓] Login berhasil setelah OTP diverifikasi
- [✓] Email ACTIVE tidak bisa register ulang
- [✓] Email PENDING bisa register ulang (update data + OTP baru)

**Hasil Akhir:** `[✓] LULUS` / `[ ] GAGAL`  
**Catatan Bug:** ada eror seperti ini waktu di encounter could not execute batch [Unique index or primary key violation: "PUBLIC.UK9Q63SNKA3MDH91AS4IO72ESPI INDEX PUBLIC.UK9Q63SNKA3MDH91AS4IO72ESPI_INDEX_4 ON PUBLIC.USERS(PHONE_NUMBER NULLS FIRST) VALUES ( /* 27 */ '081263492659' )"; SQL statement: /* insert for com.plr.aduaja.model.User */insert into users (account_status,agency_id,created_at,email,full_name,password_hash,phone_number,region_id,role,updated_ [23505-240]] [/* insert for com.plr.aduaja.model.User */insert into users (account_status,agency_id,created_at,email,full_name,password_hash,phone_number,region_id,role,updated_at,user_id) values (?,?,?,?,?,?,?,?,?,?,?)]; SQL [/* insert for com.plr.aduaja.model.User */insert into users (account_status,agency_id,created_at,email,full_name,password_hash,phone_number,region_id,role,updated_at,user_id) values (?,?,?,?,?,?,?,?,?,?,?)]; constraint [PUBLIC.UK9Q63SNKA3MDH91AS4IO72ESPI INDEX PUBLIC.UK9Q63SNKA3MDH91AS4IO72ESPI_INDEX_4]\
tapi ada juga pesan eror tidak muncul di semua form sewaktu tidak diisi dan dicoba kirim
