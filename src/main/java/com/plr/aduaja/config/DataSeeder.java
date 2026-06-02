package com.plr.aduaja.config;

import com.plr.aduaja.model.*;
import com.plr.aduaja.model.Report.ReportStatus;
import com.plr.aduaja.model.Region.RegionLevel;
import com.plr.aduaja.repository.*;
import com.plr.aduaja.service.ImageMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportCategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ReportCategory catJalan, catLampu, catTaman, catKebersihan;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // ==============================
        // 1. REGIONS (3 lokasi)
        // ==============================
        Region kotaMedan = makeRegion("Kota Medan", RegionLevel.KOTA, null);
        Region kecMedanBaru = makeRegion("Kecamatan Medan Baru", RegionLevel.KECAMATAN, kotaMedan);

        Region kotaPekanbaru = makeRegion("Kota Pekanbaru", RegionLevel.KOTA, null);
        Region kecTampan = makeRegion("Kecamatan Tampan", RegionLevel.KECAMATAN, kotaPekanbaru);

        Region kotaTanjungpinang = makeRegion("Kota Tanjungpinang", RegionLevel.KOTA, null);
        Region kecBukitBestari = makeRegion("Kecamatan Bukit Bestari", RegionLevel.KECAMATAN, kotaTanjungpinang);

        // ==============================
        // 2. CATEGORIES
        // ==============================
        catJalan = makeCategory("Kerusakan Jalan/Infrastruktur", 72, "Laporan kerusakan jalan, lubang, retak");
        catLampu = makeCategory("Fasilitas Penerangan Jalan", 48, "Laporan lampu PJU mati, rusak");
        catTaman = makeCategory("Pemeliharaan Taman/Ruang Publik", 96, "Laporan taman rusak, rumput tidak terawat");
        catKebersihan = makeCategory("Penanganan Kebersihan/Sampah", 72, "Laporan sampah berserakan, TPS penuh");

        // ==============================
        // 3. AGENCIES (3 per lokasi: PU, LH, Perhubungan)
        // ==============================
        Agency puMedan = makeAgency("Dinas PU Kota Medan", kotaMedan, "pu@medankota.go.id");
        Agency lhMedan = makeAgency("Dinas LH Kota Medan", kotaMedan, "lh@medankota.go.id");
        Agency perhubunganMedan = makeAgency("Dinas Perhubungan Kota Medan", kotaMedan, "perhubungan@medankota.go.id");

        Agency puPekanbaru = makeAgency("Dinas PU Kota Pekanbaru", kotaPekanbaru, "pu@pekanbaru.go.id");
        Agency lhPekanbaru = makeAgency("Dinas LH Kota Pekanbaru", kotaPekanbaru, "lh@pekanbaru.go.id");
        Agency perhubunganPekanbaru = makeAgency("Dinas Perhubungan Kota Pekanbaru", kotaPekanbaru, "perhubungan@pekanbaru.go.id");

        Agency puTanjungpinang = makeAgency("Dinas PU Kota Tanjungpinang", kotaTanjungpinang, "pu@tanjungpinang.go.id");
        Agency lhTanjungpinang = makeAgency("Dinas LH Kota Tanjungpinang", kotaTanjungpinang, "lh@tanjungpinang.go.id");
        Agency perhubunganTanjungpinang = makeAgency("Dinas Perhubungan Kota Tanjungpinang", kotaTanjungpinang, "perhubungan@tanjungpinang.go.id");

        // ==============================
        // 4. WARGA (5 pelapor)
        // ==============================
        User warga1 = makeUser("Budi Santoso", "budi.santoso@email.com", "081111111111", "warga123", User.Role.WARGA, null);
        User warga2 = makeUser("Sari Dewi", "sari.dewi@email.com", "082222222222", "warga123", User.Role.WARGA, null);
        User warga3 = makeUser("Agus Setiawan", "agus.setiawan@email.com", "083333333333", "warga123", User.Role.WARGA, null);
        User warga4 = makeUser("Rina Anggraini", "rina.anggraini@email.com", "084444444444", "warga123", User.Role.WARGA, null);
        User warga5 = makeUser("Doni Prasetyo", "doni.prasetyo@email.com", "085555555555", "warga123", User.Role.WARGA, null);

        // ==============================
        // 5. ADMIN DINAS (3 per lokasi = 9)
        // ==============================
        makeUser("Admin PU Medan", "admin.pu.medan@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, puMedan);
        makeUser("Admin LH Medan", "admin.lh.medan@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, lhMedan);
        makeUser("Admin Perhubungan Medan", "admin.perhubungan.medan@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, perhubunganMedan);
        makeUser("Admin PU Pekanbaru", "admin.pu.pekanbaru@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, puPekanbaru);
        makeUser("Admin LH Pekanbaru", "admin.lh.pekanbaru@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, lhPekanbaru);
        makeUser("Admin Perhubungan Pekanbaru", "admin.perhubungan.pekanbaru@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, perhubunganPekanbaru);
        makeUser("Admin PU Tanjungpinang", "admin.pu.tanjungpinang@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, puTanjungpinang);
        makeUser("Admin LH Tanjungpinang", "admin.lh.tanjungpinang@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, lhTanjungpinang);
        makeUser("Admin Perhubungan Tanjungpinang", "admin.perhubungan.tanjungpinang@aduaja.go.id", null, "admin123", User.Role.ADMIN_DINAS, perhubunganTanjungpinang);

        // ==============================
        // 6. ADMIN PUSAT (1 per region)
        // ==============================
        makeUser("Admin Pusat Medan", "admin.pusat.medan@aduaja.go.id", null, "admin123", User.Role.ADMIN_PUSAT, null, kotaMedan);
        makeUser("Admin Pusat Pekanbaru", "admin.pusat.pekanbaru@aduaja.go.id", null, "admin123", User.Role.ADMIN_PUSAT, null, kotaPekanbaru);
        makeUser("Admin Pusat Tanjungpinang", "admin.pusat.tanjungpinang@aduaja.go.id", null, "admin123", User.Role.ADMIN_PUSAT, null, kotaTanjungpinang);

        // ==============================
        // 7. PETUGAS (3 per lokasi = 9, 1 per agency)
        // ==============================
        // Medan
        User petMedan1 = makeUser("Ahmad Fauzi", "ahmad.fauzi@aduaja.go.id", "081234567890", "petugas123", User.Role.PETUGAS, puMedan);
        User petMedan2 = makeUser("Rizal Harahap", "rizal.harahap@aduaja.go.id", "082345678901", "petugas123", User.Role.PETUGAS, lhMedan);
        User petMedan3 = makeUser("Dewi Sartika", "dewi.sartika@aduaja.go.id", "083456789012", "petugas123", User.Role.PETUGAS, perhubunganMedan);
        // Pekanbaru
        User petPekanbaru1 = makeUser("Budi Hartono", "budi.hartono@aduaja.go.id", "084567890123", "petugas123", User.Role.PETUGAS, puPekanbaru);
        User petPekanbaru2 = makeUser("Siti Aminah", "siti.aminah@aduaja.go.id", "085678901234", "petugas123", User.Role.PETUGAS, lhPekanbaru);
        User petPekanbaru3 = makeUser("Joko Susilo", "joko.susilo@aduaja.go.id", "086789012345", "petugas123", User.Role.PETUGAS, perhubunganPekanbaru);
        // Tanjungpinang
        User petTanjungpinang1 = makeUser("Maria Simanjuntak", "maria.simanjuntak@aduaja.go.id", "087890123456", "petugas123", User.Role.PETUGAS, puTanjungpinang);
        User petTanjungpinang2 = makeUser("Andi Pratama", "andi.pratama@aduaja.go.id", "088901234567", "petugas123", User.Role.PETUGAS, lhTanjungpinang);
        User petTanjungpinang3 = makeUser("Lisa Kusuma", "lisa.kusuma@aduaja.go.id", "089012345678", "petugas123", User.Role.PETUGAS, perhubunganTanjungpinang);

        // ==============================
        // 8. REPORTS (2 per lokasi, DIVALIDASI)
        //    Semua DIVALIDASI agar siap disposisi oleh admin pusat
        // ==============================
        // Medan
        makeReport("ADJ-2026-00001", "Jalan berlubang besar diameter \u00B150cm di Jl. Sudirman", catJalan,
                    kecMedanBaru, warga1, "Jl. Sudirman, Medan", new BigDecimal("3.58910000"), new BigDecimal("98.67380000"));
        makeReport("ADJ-2026-00002", "Tumpukan sampah di pinggir Jl. Gajah Mada belum diangkut 1 minggu", catKebersihan,
                    kecMedanBaru, warga2, "Jl. Gajah Mada, Medan", new BigDecimal("3.58230000"), new BigDecimal("98.67010000"));
        // Pekanbaru
        makeReport("ADJ-2026-00003", "Tiang lampu PJU mati total di Simpang Tiga", catLampu,
                    kecTampan, warga3, "Simpang Tiga, Pekanbaru", new BigDecimal("0.50710000"), new BigDecimal("101.44780000"));
        makeReport("ADJ-2026-00004", "Taman kota tidak terawat, rumput tinggi dan bangku rusak", catTaman,
                    kecTampan, warga4, "Jl. Sudirman, Pekanbaru", new BigDecimal("0.51120000"), new BigDecimal("101.44560000"));
        // Tanjungpinang
        makeReport("ADJ-2026-00005", "Jalan rusak parah di Jl. Merdeka, banyak lubang besar", catJalan,
                    kecBukitBestari, warga5, "Jl. Merdeka, Tanjungpinang", new BigDecimal("0.91790000"), new BigDecimal("104.45620000"));
        makeReport("ADJ-2026-00006", "Sampah berserakan di pasar tradisional", catKebersihan,
                    kecBukitBestari, warga1, "Pasar Baru, Tanjungpinang", new BigDecimal("0.92110000"), new BigDecimal("104.45980000"));

        System.out.println("=== Data Seeder: Initial data loaded successfully ===");
    }

    // ==============================
    // HELPER METHODS
    // ==============================
    private Region makeRegion(String name, RegionLevel level, Region parent) {
        Region r = new Region();
        r.setRegionName(name);
        r.setRegionLevel(level);
        if (parent != null) r.setParentRegion(parent);
        return regionRepository.save(r);
    }

    private ReportCategory makeCategory(String name, int slaHours, String desc) {
        ReportCategory c = new ReportCategory();
        c.setCategoryName(name);
        c.setSlaDurationHours(slaHours);
        c.setDescription(desc);
        return categoryRepository.save(c);
    }

    private Agency makeAgency(String name, Region region, String email) {
        Agency a = new Agency();
        a.setAgencyName(name);
        a.setRegion(region);
        a.setContactEmail(email);
        a.setIsActive(true);
        return agencyRepository.save(a);
    }

    private User makeUser(String fullName, String email, String phone, String rawPassword,
                          User.Role role, Agency agency) {
        return makeUser(fullName, email, phone, rawPassword, role, agency, null);
    }

    private User makeUser(String fullName, String email, String phone, String rawPassword,
                          User.Role role, Agency agency, Region region) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        if (phone != null) u.setPhoneNumber(phone);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setAccountStatus(User.AccountStatus.ACTIVE);
        if (agency != null) u.setAgency(agency);
        if (region != null) u.setRegion(region);
        return userRepository.save(u);
    }

    private Report makeReport(String ticketNumber, String desc, ReportCategory category,
                              Region region, User reporter, String locationHint,
                              BigDecimal lat, BigDecimal lng) {
        Report r = new Report();
        r.setTicketNumber(ticketNumber);
        r.setDescription(desc);
        r.setCategory(category);
        r.setRegion(region);
        r.setStatus(ReportStatus.DITERIMA);
        r.setReporter(reporter);
        r.setLocationHint(locationHint);
        r.setLatitude(lat);
        r.setLongitude(lng);
        r.setSubmittedAt(LocalDateTime.now().minusDays((long)(1 + Math.random() * 14)));
        r.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(r);
    }
}
