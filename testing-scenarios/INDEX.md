# 🗂️ INDEX SKENARIO PENGUJIAN — SISTEM ADUAJA

> Setiap file = satu skenario lengkap. Ikuti langkah secara berurutan, bolak-balik halaman sesuai instruksi.

---

## Peta Status Laporan

```
[MENUNGGU_VALIDASI]
    ├── → [PERLU_REVISI]   → warga revisi → [MENUNGGU_VALIDASI] (loop)
    ├── → [DITOLAK]        ← SELESAI (final)
    └── → [DIVALIDASI]
              └── → [DIDISPOSISI]
                        └── → [DITUGASKAN]
                                  └── → [SEDANG_DIKERJAKAN]
                                            ├── → [TERTUNDA]   → resume → [SEDANG_DIKERJAKAN]
                                            └── → [SELESAI task] → [MENUNGGU_KONFIRMASI laporan]
                                                                          ├── TERIMA → [SELESAI]
                                                                          ├── TOLAK  → [SENGKETA]
                                                                          └── TIMEOUT → [DITUTUP]
[SENGKETA]
    ├── → resolve TUGASKAN_KEMBALI → [DITUGASKAN]
    └── → resolve TUTUP_LAPORAN   → [DITUTUP]
```

---

## Daftar Skenario

| File | Nama Skenario | Status Akhir | Aktor Terlibat |
|------|--------------|-------------|----------------|
| [SCN-01](./SCN-01_happy-path-selesai.md) | Happy Path — Laporan Selesai Normal | SELESAI | Warga + Admin Pusat + Admin Dinas + Petugas |
| [SCN-02](./SCN-02_laporan-ditolak.md) | Laporan Langsung Ditolak | DITOLAK | Warga + Admin Pusat |
| [SCN-03](./SCN-03_revisi-lalu-selesai.md) | Revisi 1x → Disetujui → Selesai | SELESAI | Warga + Admin Pusat + Admin Dinas + Petugas |
| [SCN-04](./SCN-04_revisi-lalu-ditolak.md) | Revisi → Ditolak (Masih Kurang) | DITOLAK | Warga + Admin Pusat |
| [SCN-05](./SCN-05_revisi-berulang.md) | Revisi Berulang 2x (Loop Validasi) | SELESAI | Warga + Admin Pusat + Admin Dinas + Petugas |
| [SCN-06](./SCN-06_sengketa-tugaskan-kembali.md) | Sengketa → Petugas Baru Ditugaskan | SELESAI | Warga + Admin + Petugas |
| [SCN-07](./SCN-07_sengketa-tutup.md) | Sengketa → Ditutup Admin | DITUTUP | Warga + Admin |
| [SCN-08](./SCN-08_konfirmasi-timeout.md) | Konfirmasi Timeout → Ditutup Otomatis | DITUTUP | Warga + Admin + Petugas |
| [SCN-09](./SCN-09_sla-terlambat.md) | SLA Terlewat (Laporan Terlambat) | TERLAMBAT | Admin Pusat + Admin Dinas + Petugas |
| [SCN-10](./SCN-10_tugas-tertunda.md) | Petugas Ajukan Penundaan → Resume | SELESAI | Petugas + Admin Dinas |
| [SCN-11](./SCN-11_merge-duplikat.md) | Merge 2 Tiket Duplikat → Selesai | SELESAI | 2 Warga + Admin Pusat + Admin Dinas + Petugas |
| [SCN-12](./SCN-12_batalkan-laporan.md) | Warga Batalkan Laporan Sendiri | DITOLAK | Warga |
| [SCN-13](./SCN-13_registrasi-dan-aktivasi.md) | Registrasi Warga Baru → Aktivasi OTP | ACTIVE | Warga baru |
| [SCN-14](./SCN-14_lupa-password.md) | Lupa Password → Reset via OTP | - | Warga |
| [SCN-15](./SCN-15_petugas-baru-onboarding.md) | Admin Dinas Buat Akun Petugas Baru | ACTIVE | Admin Dinas + Petugas baru |

---

## Legenda

| Simbol | Arti |
|--------|------|
| `[ ]` | Belum diuji |
| `[✓]` | LULUS — hasil sesuai ekspektasi |
| `[✗]` | GAGAL — catat bug di kolom Catatan |
| `⚠️` | Perhatikan khusus — area rawan bug |
| `🔄` | Pindah halaman / role |
| `📋` | Cek data yang tampil |
| `📸` | Cek visual / screenshot |

---

## Cara Menggunakan

1. **Pilih skenario** dari tabel di atas
2. **Buka file skenario** yang sesuai
3. **Ikuti setiap langkah berurutan** — jangan loncat
4. **Centang** `[ ]` → `[✓]` jika berhasil, `[✗]` jika gagal
5. **Catat bug** di kolom catatan dengan format: `BUG: [deskripsi singkat]`
6. **Lanjut ke skenario berikutnya** setelah satu skenario selesai

> 💡 **Tips:** Reset data antar skenario jika diperlukan agar hasil tidak saling mempengaruhi
