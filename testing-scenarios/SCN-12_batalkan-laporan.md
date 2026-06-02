# SCN-12 — Warga Batalkan Laporan Sendiri

**Status Akhir Laporan:** `DITOLAK` (oleh warga sendiri)  
**Aktor:** Warga  
**Estimasi Waktu:** 10 menit  
**Prasyarat:** Laporan dalam status MENUNGGU_VALIDASI.

---

## 🗺️ Alur

```
Warga buat laporan → status MENUNGGU_VALIDASI
    → Warga batalkan laporan
    → Status = DITOLAK ("Dibatalkan oleh pelapor")
    → [Tidak bisa aksi apapun lagi]
```

---

## LANGKAH DETAIL

| # | Aksi | URL | Yang Dicek | Hasil Ekspektasi | ✓/✗                                                                                                                                                             | Catatan |
|---|------|-----|------------|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| 1.1 | Warga login + buat laporan | `/warga/create-report` | - | Status = Menunggu | `[✓]`                                                                                                                                                           | ID: _______ |
| 1.2 | Buka detail laporan | `/warga/report-detail?id=...` | 📸 Tombol | Tombol "Batalkan" tersedia | `[✓]`                                                                                                                                                           | |
| 1.3 | ⚠️ Coba batalkan laporan milik orang lain | POST `/warga/withdraw-report` id=LAPORAN_ORANG_LAIN | Error | "Anda tidak berwenang membatalkan laporan ini" | `[✓] Tapi gak ada pesan peringatan sih, mending dilarang aja mengunnjungi laporan orang lain kaerna aku bisa kunjjungi laporan orang lain tanpa login`          | Bug B-05 potensial |
| 1.4 | **Batalkan laporan sendiri** | POST `/warga/withdraw-report` id=ID_SENDIRI | Flash message | "Laporan berhasil dibatalkan." | `[✗] tidak ada pesan, dan statusnya malah disebutkan dibatalkan oleh admin harusnya bukan admin tapi di halaman admin sudah benar dibilang dibatalkan oleh wrga` | |
| 1.5 | Cek status laporan | `/warga/report-detail?id=...` | 📋 Status | Status = **"Ditolak"** | `[✓]`                                                                                                                                                           | |
| 1.6 | Cek alasan penolakan | - | 📋 Alasan | "Dibatalkan oleh pelapor" | `[✗]`                                                                                                                                                           | |
| 1.7 | Cek tombol aksi hilang | - | 📸 Tombol | Tidak ada tombol lagi | `[✓]`                                                                                                                                                           | |
| 1.8 | ⚠️ Coba batalkan laporan yang sudah DIVALIDASI | Login admin → approve laporan → login warga → coba batalkan | Error | "hanya dapat dibatalkan saat masih dalam antrian verifikasi" | `[✗] Tapi tombol batalkan menjadi hilang saat sudah divalidasi`                                                                                                 | |
| 1.9 | Cek di antrian admin | Login admin → `/admin/validation` | Daftar antrian | Laporan sudah tidak ada di antrian admin | `[✗] Masih ada tapi statusnya mejnadi didisposisi`                                                                                                              | |
| 1.10 | Cek riwayat warga | `/warga/report-history` | Filter ditolak | Laporan tampil dengan status "Ditolak" | `[✓]`                                                                                                                                                            | |

---

## ✅ Kriteria LULUS

- [✓] Pembatalan berhasil saat status MENUNGGU_VALIDASI
- [✗] Status berubah ke DITOLAK dengan alasan "Dibatalkan oleh pelapor"
- [✓] Tidak bisa batalkan laporan yang sudah DIVALIDASI atau lebih jauh
- [✓] Tidak bisa batalkan laporan milik orang lain

**Hasil Akhir:** `[ ] LULUS` / `[✓] GAGAL`  
**Catatan Bug:** Masih banyak bug untuk status laporan yang gak benar sesuai dengan kriteria SRS harusnya statusnya begini
Berdasarkan dokumen *Software Requirements Specification* (SRS) Sistem AduAja V1.0, berikut adalah seluruh status laporan beserta kondisi yang mendasarinya secara lengkap dan berurutan:
### Status Laporan AduAja yang seharusnya
| Status Laporan                                      | Kondisi dan Keterangan                                                                                                                                                                                                                             |
| :-------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1. Menunggu Verifikasi**                          | Status awal yang ditetapkan secara otomatis setelah Warga berhasil mengirimkan laporan baru. Status ini juga berlaku ketika Warga mengirimkan ulang laporan setelah melakukan revisi. Warga dapat membatalkan laporan hanya pada status ini.       |
| **2. Ditolak**                                      | Keputusan validasi yang diambil oleh Administrator Pusat. Admin wajib mengisi alasan teknis penolakan.                                                                                                                                             |
| **3. Menunggu Revisi (Direvisi)**                   | Keputusan validasi yang diambil oleh Administrator Pusat. Status ini memungkinkan Warga untuk membuka akses pengubahan data (foto, lokasi, deskripsi) laporan sebelum dikirim ulang ke status "Menunggu Verifikasi".                               |
| **4. Diterima**                                     | Keputusan validasi yang diambil oleh Administrator Pusat. Laporan yang berstatus "Diterima" dapat diteruskan ke proses disposisi (pemilihan dinas tujuan).                                                                                         |
| **5. Tergabung**                                    | Status yang ditetapkan pada *Child Ticket* (laporan turunan) yang digabungkan ke dalam *Parent Ticket* (tiket utama) dalam fitur *Merge Ticket* oleh Admin Pusat. Tiket ini dihapus dari antrean tugas utama.                                      |
| **6. Dalam Peninjauan**                             | Status yang ditetapkan oleh Administrator Pusat segera setelah mengkonfirmasi penerusan (disposisi) laporan ke Dinas Tujuan. Status ini juga merupakan salah satu opsi keputusan validasi awal oleh Admin Pusat.                                   |
| **7. Ditugaskan**                                   | Status yang ditetapkan oleh Administrator instansi (Admin Dinas) segera setelah mengonfirmasi pengerahan Penanggung Jawab Lapangan/Personel.                                                                                                       |
| **8. Sedang Berjalan (Sedang Dikerjakan/Diproses)** | Status yang ditetapkan ketika Petugas Lapangan menerima/memulai tugas, atau ketika Admin Dinas mengonfirmasi fungsionalitas pemulihan waktu (*Lanjutkan Waktu*) dari status "Tertunda".                                                            |
| **9. Tertunda**                                     | Status yang ditetapkan oleh Admin Dinas (dengan alasan) atau disetujui dari pengajuan penundaan Petugas. Status ini akan menghentikan sementara hitung mundur SLA.                                                                                 |
| **10. Terlambat**                                   | Status yang secara otomatis dideteksi dan diubah oleh sistem apabila kalkulasi durasi pengerjaan telah melewati batas waktu target penyelesaian (SLA).                                                                                             |
| **11. Menunggu Validasi**                           | Status yang ditetapkan ketika Penanggung Jawab Lapangan menandai tugas operasional selesai, dan menunggu konfirmasi (Terima/Tolak) dari Warga Pelapor.                                                                                             |
| **12. Sengketa**                                    | Status yang ditetapkan ketika Warga Pelapor menolak hasil perbaikan dan mengonfirmasi penolakan, sehingga laporan diteruskan ke antrean khusus Administrator instansi terkait untuk ditinjau ulang.                                                |
| **13. Dalam Evaluasi Sengketa**                     | Status otomatis untuk *Parent Ticket* (tiket gabungan) apabila terdeteksi minimal satu penolakan ("Sengketa") dari salah satu Warga di dalam grup tiket tersebut.                                                                                  |
| **14. Selesai Otomatis**                            | Status yang dieksekusi secara otomatis oleh sistem jika batas waktu konfirmasi Warga (3x24 jam hari kerja) telah habis dan Warga tidak memberikan respons.                                                                                         |
| **15. Selesai**                                     | Status akhir yang ditetapkan ketika Warga mengonfirmasi persetujuan atas hasil pekerjaan atau ketika *Parent Ticket* telah disetujui oleh seluruh Warga dalam grup atau melewati batas waktu otomatis. Status ini terkunci permanen (*read-only*). |



---
