# Tabel Interaksi UI Admin Dinas

Dokumen ini merangkum objek layar, tindakan, serta respons sistem pada halaman Admin Dinas.

## `dinas-dashboard.html`

| Objek Layar | Tindakan | Deskripsi & Respons Sistem |
| --- | --- | --- |
| Tab "Penugasan Petugas" | Klik | Menampilkan daftar laporan yang menunggu penugasan petugas lapangan. |
| Tab "Sengketa" | Klik | Menampilkan ringkasan resolusi sengketa dan tombol masuk panel sengketa. |
| Kartu Statistik | Read-only | Menampilkan ringkasan statistik (jumlah laporan, status, dll.) dengan ikon dan warna. |
| Tombol "Lihat Detail" (Penugasan) | Klik | Membuka halaman detail penugasan untuk laporan terpilih. |
| Tombol "Buka Panel Resolusi Sengketa" | Klik | Navigasi ke halaman sengketa dinas. |
| Tombol "Logout" | Klik | Mengakhiri sesi admin dan keluar dari sistem. |

## `dinas-queue.html`

| Objek Layar | Tindakan | Deskripsi & Respons Sistem |
| --- | --- | --- |
| Search Bar "Cari berdasarkan ID Tiket atau Judul..." | Ketik | Menyaring daftar antrean berdasarkan ID atau judul (input UI). |
| Dropdown Status | Pilih | Memfilter tiket berdasarkan status (Belum Ditindaklanjuti, Dalam Penanganan, dll.). |
| Banner Peringatan SLA | Read-only | Menampilkan jumlah laporan terlambat SLA jika ada. |
| Tabel Antrean Laporan | Read-only | Menampilkan detail tiket: ID, judul, kategori, pelapor, wilayah, disposisi, status, deadline. |
| Tombol "Detail" | Klik | Membuka halaman detail antrean dinas untuk tiket tersebut. |
| Pagination "Sebelumnya/Selanjutnya" | Klik | Navigasi halaman data antrean laporan. |

## `penugasan-petugas.html`

| Objek Layar | Tindakan | Deskripsi & Respons Sistem |
| --- | --- | --- |
| Daftar Laporan Disposisi | Klik | Memilih laporan untuk ditugaskan dan membuka detail di panel kanan. |
| Search Bar Petugas | Ketik | Menyaring daftar petugas berdasarkan nama, NIP, wilayah, atau kontak. |
| Radio Pilih Petugas | Klik | Menetapkan petugas lapangan untuk laporan terpilih (hanya yang "Tersedia"). |
| Tombol "Tugaskan Petugas" | Klik | Mengirim penugasan petugas untuk tiket terpilih. |
| Tombol "Batal" | Klik | Membatalkan proses penugasan dan kembali ke halaman penugasan. |
| Panel Informasi Laporan | Read-only | Menampilkan detail laporan (ID, prioritas, kategori, wilayah, deadline, judul). |
| Foto Bukti Pelapor | Read-only | Menampilkan foto bukti untuk verifikasi visual. |
| Box "Instruksi Admin Pusat" | Read-only | Menampilkan arahan admin pusat untuk penanganan laporan. |
| Box Peringatan Validasi Wilayah | Read-only | Menyampaikan aturan validasi wilayah petugas terhadap lokasi laporan. |

## `progress-update.html`

| Objek Layar | Tindakan | Deskripsi & Respons Sistem |
| --- | --- | --- |
| Daftar Tiket "Dalam Penanganan" | Klik | Memilih tiket untuk melihat detail dan update progress. |
| Search Bar Tiket | Ketik | Menyaring tiket berdasarkan ID atau judul. |
| Panel Detail Tiket | Read-only | Menampilkan informasi tiket terpilih (ID, prioritas, pelapor, deadline, judul). |
| Riwayat Update Progress | Read-only | Menampilkan daftar update sebelumnya (tanggal, petugas, keterangan, estimasi). |
| Form "Keterangan Update" | Ketik | Mengisi keterangan progres (minimal 10 karakter). |
| Field "Estimasi Waktu Penyelesaian" | Pilih | Mengisi estimasi tanggal/waktu penyelesaian (opsional). |
| Tombol "Kirim Update Progress" | Klik | Mengirim update progres untuk tiket terpilih. |
| Tombol "Batal" | Klik | Membatalkan input update progress. |
| Info Notifikasi | Read-only | Menjelaskan bahwa update akan dikirim ke pelapor. |

## `close-ticket.html`

| Objek Layar | Tindakan | Deskripsi & Respons Sistem |
| --- | --- | --- |
| Daftar "Siap Ditutup" | Klik | Memilih tiket untuk dilakukan penutupan. |
| Search Bar Tiket | Ketik | Menyaring tiket siap ditutup berdasarkan ID atau judul. |
| Panel Detail Tiket | Read-only | Menampilkan info tiket (ID, prioritas, pelapor, wilayah, judul). |
| Form "Keterangan Penyelesaian" | Ketik | Mengisi catatan penyelesaian (minimal 20 karakter). |
| Upload "Foto Bukti Penyelesaian" | Upload | Mengunggah 1-5 foto bukti penutupan tiket. |
| Tombol "Tutup Tiket dan Kirim Verifikasi" | Klik | Mengirim penutupan tiket ke admin pusat untuk verifikasi akhir. |
| Tombol "Batal" | Klik | Membatalkan proses penutupan tiket. |

## `sengketa-dinas.html`

| Objek Layar | Tindakan | Deskripsi & Respons Sistem |
| --- | --- | --- |
| Tabel Daftar Sengketa | Read-only | Menampilkan daftar sengketa: ID, tiket terkait, judul, pelapor, prioritas, status, tanggal. |
| Tombol "Lihat Detail" | Klik | Membuka detail sengketa yang dipilih. |
| Panel Informasi Sengketa | Read-only | Menampilkan info sengketa (ID, tiket terkait, status, prioritas, pelapor, tanggal, judul). |
| Perbandingan Foto Bukti | Read-only | Menampilkan foto bukti perbaikan vs foto komplain warga. |
| Form "Catatan Resolusi" | Ketik | Mengisi alasan keputusan untuk dikirim ke warga. |
| Tombol "Sengketa Diterima - Perbaiki Ulang" | Klik | Membuka modal penugasan ulang petugas. |
| Tombol "Sengketa Ditolak - Tiket Selesai" | Klik | Menolak sengketa dan menyelesaikan tiket. |
| Tombol "Perlu Tinjauan Lanjutan" | Klik | Menandai sengketa untuk review lebih lanjut. |
| Modal "Tugaskan Petugas Baru" | Pilih & Ketik | Memilih petugas pengganti dan mengisi catatan perbaikan ulang. |
| Tombol "Tugaskan Sekarang" | Klik | Mengirim penugasan ulang petugas. |
| Tombol "Batal" (Modal) | Klik | Menutup modal penugasan ulang. |

