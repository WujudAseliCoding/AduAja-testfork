package com.plr.aduaja.controller;

import lombok.extern.slf4j.Slf4j;
import com.plr.aduaja.dto.DispositionDTO;
import com.plr.aduaja.dto.MergeDTO;
import com.plr.aduaja.model.*;
import com.plr.aduaja.model.Report.ReportStatus;
import com.plr.aduaja.repository.ReportCategoryRepository;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class AdminPusatController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @Autowired
    private FieldTaskService fieldTaskService;

    @Autowired
    private DispositionService dispositionService;

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private MergeRecordService mergeRecordService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportCategoryRepository reportCategoryRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SlaRecordService slaRecordService;

    @Autowired
    private SlaMonitoringService slaMonitoringService;

    @Autowired
    private AuditLogService auditLogService;

    // ==========================================
    // ADMIN PUSAT — DASHBOARD
    // ==========================================

    @GetMapping("/admin/home")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            Model model,
            HttpSession session,
            @RequestParam(value = "role", required = false, defaultValue = "admin_pusat") String role,
            @RequestParam(value = "tab", required = false, defaultValue = "queue") String tab,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        // SESSION CHECK — semua halaman admin harus login
        String sessionUserId = ControllerHelper.requireAnyAdminSession(session);
        if (sessionUserId == null) return "redirect:/admin/login";
        model.addAttribute("userRole", role);

        String regionId = ControllerHelper.getSessionRegionId(session);

        if ("admin_dinas".equalsIgnoreCase(role)) {
            model.addAttribute("dinasName", "Dinas Pekerjaan Umum");
            long diterima = reportService.countByStatus(Report.ReportStatus.DALAM_PENINJAUAN);
            long diproses = fieldTaskService.countByStatus(FieldTask.TaskStatus.SEDANG_DIKERJAKAN);
            long baru = fieldTaskService.countByStatus(FieldTask.TaskStatus.BARU);
            long selesai = fieldTaskService.countByStatus(FieldTask.TaskStatus.SELESAI);
            List<Map<String, Object>> dinasStats = new ArrayList<>();
            dinasStats.add(Map.of("title", "Laporan Diterima", "value", diterima,
                    "icon", "inbox", "bgColor", "bg-blue-100", "color", "text-blue-600"));
            dinasStats.add(Map.of("title", "Tugas Baru", "value", baru,
                    "icon", "inbox", "bgColor", "bg-indigo-100", "color", "text-indigo-600"));
            dinasStats.add(Map.of("title", "Dalam Penanganan", "value", diproses,
                    "icon", "wrench", "bgColor", "bg-yellow-100", "color", "text-yellow-600"));
            dinasStats.add(Map.of("title", "Selesai", "value", selesai,
                    "icon", "check-circle", "bgColor", "bg-green-100", "color", "text-green-600"));
            model.addAttribute("stats", dinasStats);
            List<Map<String, Object>> pendingAssignments = new ArrayList<>();
            List<Disposition> allDisp = dispositionService.getAllDispositions();
            for (Disposition d : allDisp) {
                if (d.getReport() != null) {
                    List<FieldTask> existingTasks = fieldTaskService.getTasksByReport(d.getReport().getReportId());
                    if (existingTasks.isEmpty()) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", d.getReport().getReportId());
                        m.put("judul", d.getReport().getTicketNumber() != null ? d.getReport().getTicketNumber() : "Laporan");
                        m.put("kategori", d.getReport().getCategory() != null ? d.getReport().getCategory().getCategoryName() : "Lainnya");
                        m.put("prioritas", "Sedang");
                        m.put("slaStatus", "-");
                        pendingAssignments.add(m);
                    }
                }
            }
            model.addAttribute("pendingAssignments", pendingAssignments);
            List<User> realPetugas = userService.findByRole(User.Role.PETUGAS);
            List<Map<String, Object>> petugasList = realPetugas.stream().map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getUserId());
                m.put("nama", p.getFullName());
                m.put("nip", "-");
                m.put("statusKetersediaan", "Tersedia");
                m.put("wilayahTugas", "-");
                m.put("tugasAktif", (int) fieldTaskService.getTasksByOfficerAndStatus(p.getUserId(), FieldTask.TaskStatus.SEDANG_DIKERJAKAN).size());
                m.put("kontak", p.getEmail());
                return m;
            }).collect(Collectors.toList());
            model.addAttribute("availablePetugas", petugasList.isEmpty() ? new ArrayList<>() : petugasList);
        } else {
            long laporanMasuk = regionId != null ? reportService.countByStatusAndRegion(Report.ReportStatus.MENUNGGU_VERIFIKASI, regionId) : reportService.countByStatus(Report.ReportStatus.MENUNGGU_VERIFIKASI);
            long menungguValidasi = laporanMasuk;
            long dalamAntreanDinas = regionId != null ? reportService.countByStatusAndRegion(Report.ReportStatus.DITERIMA, regionId) : reportService.countByStatus(Report.ReportStatus.DITERIMA);
            long selesaiHariIni = regionId != null ? reportService.countByStatusAndRegion(Report.ReportStatus.SELESAI, regionId) : reportService.countByStatus(Report.ReportStatus.SELESAI);
            List<Map<String, Object>> stats = new ArrayList<>();
            stats.add(Map.of("title", "Laporan Masuk", "value", laporanMasuk,
                    "icon", "file", "bgColor", "bg-blue-100", "color", "text-blue-600"));
            stats.add(Map.of("title", "Menunggu Konfirmasi Warga", "value", menungguValidasi,
                    "icon", "clock", "bgColor", "bg-yellow-100", "color", "text-yellow-600"));
            stats.add(Map.of("title", "Dalam Antrean Dinas", "value", dalamAntreanDinas,
                    "icon", "alert-triangle", "bgColor", "bg-red-100", "color", "text-red-600"));
            stats.add(Map.of("title", "Selesai Hari Ini", "value", selesaiHariIni,
                    "icon", "check-circle", "bgColor", "bg-green-100", "color", "text-green-600"));
            model.addAttribute("stats", stats);
        }

        List<Map<String, Object>> panels = new ArrayList<>();
        panels.add(Map.of("title", "Antrean Laporan", "description", "Daftar laporan masuk yang perlu divalidasi", "icon", "file", "color", "bg-blue-100 text-blue-600", "href", "/admin/laporan-queue"));
        panels.add(Map.of("title", "Validasi Laporan", "description", "Periksa dan putuskan kelayakan laporan", "icon", "check-circle-2", "color", "bg-green-100 text-green-600", "href", "/admin/validation"));
        panels.add(Map.of("title", "Merge Tiket Duplikat", "description", "Deteksi dan gabungkan laporan serupa", "icon", "git-merge", "color", "bg-yellow-100 text-yellow-600", "href", "/admin/merge"));
        panels.add(Map.of("title", "Disposisi ke Dinas", "description", "Kirim laporan ke dinas terkait", "icon", "send", "color", "bg-purple-100 text-purple-600", "href", "/admin/disposisi"));
        panels.add(Map.of("title", "Sengketa", "description", "Kelola banding dan resolusi sengketa", "icon", "scale", "color", "bg-orange-100 text-orange-600", "href", "/admin/sengketa"));
        model.addAttribute("panels", panels);

        Map<String, Object> queueResult = getQueueList(regionId, page, size);
        model.addAttribute("queueReports", queueResult.get("items"));
        model.addAttribute("currentPage", queueResult.get("page"));
        model.addAttribute("totalPages", queueResult.get("totalPages"));
        model.addAttribute("totalItems", queueResult.get("totalItems"));
        model.addAttribute("pageSize", size);

        // Riwayat laporan yang ditolak (Issue 4)
        List<Report> rejectedReports = regionId != null ? reportService.getReportsByStatusAndRegion(Report.ReportStatus.DITOLAK, regionId) : reportService.getReportsByStatus(Report.ReportStatus.DITOLAK);
        List<Map<String, Object>> rejectedList = rejectedReports.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getReportId());
            m.put("judul", r.getTicketNumber() != null ? r.getTicketNumber() : "Laporan");
            m.put("kategori", r.getCategory() != null ? r.getCategory().getCategoryName() : "Lainnya");
            m.put("pelapor", r.getReporter() != null ? r.getReporter().getFullName() : "-");
            m.put("wilayah", r.getLocationHint() != null ? r.getLocationHint() : "-");
            m.put("tanggalMasuk", toDateStr(r.getSubmittedAt()));
            m.put("alasanDitolak", r.getRejectionReason() != null ? r.getRejectionReason() :
                    (r.getAdminNotes() != null ? r.getAdminNotes() : "-"));
            return m;
        }).collect(Collectors.toList());
        model.addAttribute("rejectedReports", rejectedList);
        model.addAttribute("rejectedCount", rejectedList.size());

        List<Map<String, Object>> validationReports = getAdminValidationList(regionId);
        model.addAttribute("validationReports", validationReports);

        Map<String, Object> selected = null;
        if (id != null) {
            selected = validationReports.stream()
                    .filter(r -> id.equals(String.valueOf(r.get("id"))))
                    .findFirst().orElse(null);
        }
        model.addAttribute("selectedReport", selected);

        List<MergeRecord> activeMerges = getActiveMerges();
        Set<String> mergedChildIds = getAllActiveChildIds();
        List<Report> mergeCandidates = regionId != null ? reportService.getReportsByStatusAndRegion(Report.ReportStatus.MENUNGGU_VERIFIKASI, regionId) : reportService.getReportsByStatus(Report.ReportStatus.MENUNGGU_VERIFIKASI);
        List<Map<String, Object>> mergeTickets = mergeCandidates.stream()
            .filter(r -> !mergedChildIds.contains(r.getReportId()))
            .map(this::toMergeTicketMap)
            .collect(Collectors.toList());
        model.addAttribute("mergeTickets", mergeTickets);
        model.addAttribute("clusters", buildMergeClusters(activeMerges));
        model.addAttribute("selectedTickets", new ArrayList<>());
        model.addAttribute("hiddenChildCount", mergedChildIds.size());

        List<Map<String, Object>> disposisiReports = new ArrayList<>();
        List<Report> validated = regionId != null ? reportService.getReportsByStatusAndRegion(Report.ReportStatus.DITERIMA, regionId) : reportService.getReportsByStatus(Report.ReportStatus.DITERIMA);
        for (Report r : validated) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getReportId());
            m.put("judul", r.getTicketNumber() != null ? r.getTicketNumber() : "Laporan #" + r.getReportId().substring(0, 8));
            m.put("kategori", r.getCategory() != null ? r.getCategory().getCategoryName() : "Lainnya");
            m.put("pelapor", r.getReporter() != null ? r.getReporter().getFullName() : "-");
            m.put("wilayah", r.getLocationHint() != null ? r.getLocationHint() : "-");
            m.put("status", "Tervalidasi");
            m.put("prioritasSistem", "Sedang");
            m.put("dinasRekomendasi", "Dinas Terkait");
            m.put("foto", r.getPhotoBase64() != null ? r.getPhotoBase64() : dummyReportImage());
            disposisiReports.add(m);
        }
        model.addAttribute("disposisiReports", disposisiReports);

        Map<String, Object> selectedDisposition = null;
        if ("disposisi".equalsIgnoreCase(tab)) {
            if (id != null && !id.trim().isEmpty()) {
                String targetId = id.trim();
                for (Map<String, Object> r : disposisiReports) {
                    Object rid = r.get("id");
                    if (rid != null && targetId.equals(String.valueOf(rid))) {
                        selectedDisposition = r;
                        break;
                    }
                }
                if (selectedDisposition == null && !disposisiReports.isEmpty()) {
                    selectedDisposition = disposisiReports.get(0);
                }
            } else if (!disposisiReports.isEmpty()) {
                selectedDisposition = disposisiReports.get(0);
            }
        }
        model.addAttribute("selectedDisposition", selectedDisposition);

        // filter daftar dinas hanya yang beroperasi di region laporan terpilih
        List<Agency> realAgencies;
        if (selectedDisposition != null) {
            String rptId = String.valueOf(selectedDisposition.get("id"));
            if (rptId != null && !rptId.isBlank()) {
                Report rpt = reportService.findById(rptId).orElse(null);
                if (rpt != null && rpt.getRegion() != null) {
                    realAgencies = agencyService.getActiveAgenciesByRegion(rpt.getRegion().getRegionId());
                } else {
                    realAgencies = agencyService.getActiveAgencies();
                }
            } else {
                realAgencies = agencyService.getActiveAgencies();
            }
        } else {
            realAgencies = agencyService.getActiveAgencies();
        }
        List<Map<String, Object>> dinasList = realAgencies.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getAgencyId());
            m.put("name", a.getAgencyName());
            m.put("kategori", a.getContactEmail() != null ? List.of(a.getContactEmail()) : List.of("Lainnya"));
            return m;
        }).collect(Collectors.toList());
        model.addAttribute("dinasList", dinasList);

        return "admin/dashboard";
    }

    // ==========================================
    // ADMIN PUSAT — QUEUE DETAIL & DISPOSISI DETAIL
    // ==========================================

    @GetMapping("/admin/dashboard/detail")
    public String adminQueueDetail(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";
        String regionId = ControllerHelper.getSessionRegionId(session);
        Map<String, Object> selectedReport = null;
        boolean isInDisposisi = false;

        if (id != null && !id.trim().isEmpty()) {
            List<Map<String, Object>> validationReports = getAdminValidationList(regionId);
            for (Map<String, Object> r : validationReports) {
                if (id.trim().equals(String.valueOf(r.get("id")))) {
                    selectedReport = r;
                    break;
                }
            }
            if (selectedReport == null) {
                Report r = reportService.findById(id.trim()).orElse(null);
                if (r != null && r.getStatus() == ReportStatus.DITERIMA) {
                    selectedReport = toAdminValidationMap(r);
                    selectedReport.put("status", "Tervalidasi");
                    isInDisposisi = true;
                } else if (r != null && r.getStatus() == ReportStatus.DITOLAK) {
                    selectedReport = toAdminValidationMap(r);
                    selectedReport.put("status", "Ditolak");
                } else if (r != null) {
                    selectedReport = toAdminValidationMap(r);
                }
            }
        }

        model.addAttribute("selectedReport", selectedReport);
        model.addAttribute("isInDisposisi", isInDisposisi);

        // Audit Trail / Log Jejak Digital
        // Render komponen linimasa vertikal dari audit log tiket yang dipilih
        List<Map<String, Object>> auditLogs = new ArrayList<>();
        if (id != null && !id.trim().isEmpty()) {
            try {
                List<AuditLog> logs = auditLogService.getLogsByReport(id.trim());
                auditLogs = logs.stream().map(log -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("logId", log.getLogId());
                    entry.put("actor", log.getActor() != null ? log.getActor().getFullName() : "Sistem");
                    entry.put("actionType", log.getActionType() != null ? log.getActionType() : "-");
                    entry.put("oldValue", log.getOldValue() != null ? log.getOldValue() : "-");
                    entry.put("newValue", log.getNewValue() != null ? log.getNewValue() : "-");
                    entry.put("loggedAt", log.getLoggedAt() != null
                            ? log.getLoggedAt().format(ControllerHelper.DATETIME_FMT) : "-");
                    return entry;
                }).collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Gagal memuat audit log untuk tiket {}: {}", id, e.getMessage());
            }
        }
        model.addAttribute("auditLogs", auditLogs);

        return "admin/queue-detail";
    }

    @GetMapping("/admin/dashboard/disposisi-detail")
    public String adminDisposisiDetail(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";
        Map<String, Object> selectedDisposition = null;
        if (id != null && !id.trim().isEmpty()) {
            Report r = reportService.findById(id.trim()).orElse(null);
            if (r != null) {
                selectedDisposition = new HashMap<>();
                selectedDisposition.put("id", r.getReportId());
                selectedDisposition.put("judul", r.getTicketNumber() != null
                        ? r.getTicketNumber()
                        : "Laporan #" + r.getReportId().substring(0, 8));
                selectedDisposition.put("kategori", r.getCategory() != null
                        ? r.getCategory().getCategoryName() : "Lainnya");
                selectedDisposition.put("pelapor", r.getReporter() != null
                        ? r.getReporter().getFullName() : "-");
                selectedDisposition.put("wilayah", r.getLocationHint() != null
                        ? r.getLocationHint() : "-");
                selectedDisposition.put("deskripsi", r.getDescription() != null
                        ? r.getDescription() : "-");
                selectedDisposition.put("status", r.getStatus() != null
                        ? r.getStatus().name() : "TIDAK_DIKETAHUI");
                selectedDisposition.put("prioritasSistem", "Sedang");
                selectedDisposition.put("dinasRekomendasi", "");
                selectedDisposition.put("instruksiAdmin", "");
                selectedDisposition.put("deadline", "");
                selectedDisposition.put("foto", r.getPhotoBase64() != null
                        ? r.getPhotoBase64() : dummyReportImage());

                final Map<String, Object> dispMap = selectedDisposition;
                dispositionService.getDispositionByReportId(r.getReportId()).ifPresent(d -> {
                    if (d.getTargetAgency() != null) {
                        dispMap.put("dinasRekomendasi", d.getTargetAgency().getAgencyName());
                    }
                    if (d.getPriority() != null) {
                        dispMap.put("prioritasSistem", d.getPriority());
                    }
                    if (d.getDeadline() != null) {
                        dispMap.put("deadline", d.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
                    }
                    if (d.getInstructions() != null) {
                        dispMap.put("instruksiAdmin", d.getInstructions());
                    } else if (d.getNotes() != null) {
                        dispMap.put("instruksiAdmin", d.getNotes());
                    }
                });
            }
        }

        String reportRegionId = null;
        if (selectedDisposition != null && id != null) {
            Report r = reportService.findById(id.trim()).orElse(null);
            if (r != null && r.getRegion() != null) {
                reportRegionId = r.getRegion().getRegionId();
            }
        }
        if (reportRegionId == null) {
            reportRegionId = ControllerHelper.getSessionRegionId(session);
        }

        List<Agency> realAgencies = reportRegionId != null
                ? agencyService.getActiveAgenciesByRegion(reportRegionId)
                : agencyService.getActiveAgencies();
        List<Map<String, Object>> dinasList = realAgencies.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getAgencyId());
            m.put("name", a.getAgencyName());
            m.put("kategori", a.getContactEmail() != null ? List.of(a.getContactEmail()) : List.of("Lainnya"));
            return m;
        }).collect(Collectors.toList());
        model.addAttribute("dinasList", dinasList);
        model.addAttribute("selectedDisposition", selectedDisposition);
        return "admin/disposisi-detail";
    }

    @GetMapping("/admin/disposisi-detail")
    public String adminDisposisiDetailDirect(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        return adminDisposisiDetail(model, session, id);
    }

    // ==========================================
    // ADMIN PUSAT — LAPORAN QUEUE
    // ==========================================

    @GetMapping("/admin/laporan-queue")
    public String adminLaporanQueue(
            Model model,
            HttpSession session,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        // SESSION CHECK
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

        String regionId = ControllerHelper.getSessionRegionId(session);
        Map<String, Object> queueResult = getQueueList(regionId, page, size);
        model.addAttribute("queueReports", queueResult.get("items"));
        model.addAttribute("currentPage", queueResult.get("page"));
        model.addAttribute("totalPages", queueResult.get("totalPages"));
        model.addAttribute("totalItems", queueResult.get("totalItems"));
        model.addAttribute("pageSize", size);
        return "admin/laporan-queue";
    }

    // ==========================================
    // ADMIN PUSAT — VALIDASI
    // ==========================================

    @GetMapping("/admin/validation")
    public String adminValidationPanel(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        // SESSION CHECK
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

        String regionId = ControllerHelper.getSessionRegionId(session);
        List<Map<String, Object>> reports = getAdminValidationList(regionId);
        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", reports.size());
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 1);
        model.addAttribute("totalItems", reports.size());
        model.addAttribute("pageSize", reports.size());

        Map<String, Object> selected = null;
        if (id != null && !reports.isEmpty()) {
            selected = reports.stream()
                    .filter(r -> r.get("id").equals(id))
                    .findFirst().orElse(reports.get(0));
        }
        model.addAttribute("selectedReport", selected);
        return "admin/validation-panel";
    }

    @PostMapping("/admin/validation")
    public String adminValidationPost(
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "rejectionReason", required = false) String rejectionReason
    ) {
        // SESSION CHECK
        String adminId = ControllerHelper.requireAnyAdminSession(session);
        if (adminId == null) return "redirect:/admin/login";

        String ticketId = id != null ? id.trim() : null;
        String normalizedAction = action != null ? action.trim().toLowerCase() : null;
        // DRY: gabungkan reason dan rejectionReason dalam satu variabel
        String note = (reason != null && !reason.trim().isEmpty())
                ? reason.trim()
                : (rejectionReason != null && !rejectionReason.trim().isEmpty() ? rejectionReason.trim() : null);

        if (ticketId == null || ticketId.isEmpty() || normalizedAction == null) {
            return "redirect:/admin/validation";
        }

        // Cegah validasi langsung pada child ticket yang sudah digabungkan
        java.util.Optional<Report> targetReport = reportService.findById(ticketId);
        if (targetReport.isPresent() && targetReport.get().getStatus() == Report.ReportStatus.TERGABUNG) {
            redirectAttributes.addFlashAttribute("error", "Laporan yang sudah digabungkan tidak dapat divalidasi secara langsung.");
            return "redirect:/admin/validation";
        }

        // Default redirect ke halaman validation
        String redirectUrl = "redirect:/admin/validation";

        try {
            ReportStatus newStatus;
            String notifTitle;
            String notifMsg;

            if ("approved".equals(normalizedAction) || "approve".equals(normalizedAction)) {
                newStatus = ReportStatus.DITERIMA;
                notifTitle = "Laporan Divalidasi";
                notifMsg = "Laporan Anda telah divalidasi dan akan segera diteruskan ke dinas terkait.";
                // FIX SCN-01 (2.7): Redirect ke panel disposisi setelah approve
                redirectUrl = "redirect:/admin/disposisi?id=" + ticketId;
            } else if ("revision".equals(normalizedAction)) {
                newStatus = ReportStatus.MENUNGGU_REVISI;
                notifTitle = "Laporan Perlu Revisi";
                notifMsg = "Laporan Anda perlu direvisi." + (note != null ? " Catatan: " + note : "");
                // FIX SCN-03 (2.6): Redirect ke validation panel (bukan URL kosong)
                redirectUrl = "redirect:/admin/validation";
            } else {
                // rejected
                newStatus = ReportStatus.DITOLAK;
                notifTitle = "Laporan Ditolak";
                notifMsg = "Laporan Anda ditolak." + (note != null ? " Alasan: " + note : "");
                // FIX SCN-02 (2.7): Redirect ke halaman validation setelah tolak
                redirectUrl = "redirect:/admin/validation";
            }

            // Cegah tolak/revisi pada parent ticket yang memiliki child aktif
            if ((newStatus == ReportStatus.DITOLAK || newStatus == ReportStatus.MENUNGGU_REVISI)
                && targetReport.isPresent()
                && reportRepository.countByParentReportReportId(ticketId) > 0) {
                redirectAttributes.addFlashAttribute("error", "Laporan ini memiliki child ticket yang digabungkan. Tidak dapat ditolak atau direvisi. Setujui laporan untuk melanjutkan proses.");
                return "redirect:/admin/validation";
            }

            // FIX SCN-03 (3.3): Simpan note/alasan sebagai rejectionReason agar warga bisa lihat catatan admin
            Report r;
            if (newStatus == ReportStatus.DITOLAK || newStatus == ReportStatus.MENUNGGU_REVISI) {
                // Simpan note sebagai KEDUA field: rejectionReason (tampil di halaman warga) DAN adminNotes
                r = reportService.updateStatus(ticketId, newStatus, note, note, adminId);
            } else {
                r = reportService.updateStatus(ticketId, newStatus, note, adminId);
            }

            if (r != null && r.getReporter() != null) {
                notificationService.createNotification(
                        r.getReporter().getUserId(), notifTitle, notifMsg, "REPORT", r.getReportId()
                );
            }
            redirectAttributes.addFlashAttribute("success", notifTitle);
        } catch (Exception e) {
            log.error("Gagal validasi laporan {}: {}", ticketId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal memproses validasi: " + e.getMessage());
        }
        return redirectUrl;
    }

    @GetMapping("/admin/validation-panel")
    public String adminValidationPanelAlias(Model model,
                                            @RequestParam(value = "id", required = false) String id) {
        return "redirect:/admin/validation" + (id != null ? "?id=" + id : "");
    }

    @PostMapping("/admin/validation-panel")
    public String adminValidationPanelAliasPost(
            @RequestParam(value = "id", required = false) String id
    ) {
        return "redirect:/admin/validation" + (id != null ? "?id=" + id : "");
    }

    // ==========================================
    // ADMIN PUSAT — MERGE TIKET
    // ==========================================

    @GetMapping("/admin/merge")
    public String adminMergeTicketPanel(Model model, HttpSession session) {
        // SESSION CHECK
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";
        String regionId = ControllerHelper.getSessionRegionId(session);
        List<MergeRecord> activeMerges = getActiveMerges();
        Set<String> mergedChildIds = getAllActiveChildIds();
        List<Report> mergeCandidates = regionId != null ? reportService.getReportsByStatusAndRegion(Report.ReportStatus.MENUNGGU_VERIFIKASI, regionId) : reportService.getReportsByStatus(Report.ReportStatus.MENUNGGU_VERIFIKASI);
        List<Map<String, Object>> mergeTickets = mergeCandidates.stream()
                .filter(r -> !mergedChildIds.contains(r.getReportId()))
                .map(this::toMergeTicketMap)
                .collect(Collectors.toList());
        model.addAttribute("mergeTickets", mergeTickets);
        model.addAttribute("clusters", buildMergeClusters(activeMerges));
        model.addAttribute("selectedTickets", new ArrayList<>());
        model.addAttribute("hiddenChildCount", mergedChildIds.size());
        return "admin/merge-ticket-panel";
    }

    @GetMapping("/admin/merge-ticket-panel")
    public String adminMergeTicketPanelAlias(Model model, HttpSession session) {
        return adminMergeTicketPanel(model, session);
    }

    @PostMapping("/admin/merge")
    public String adminMergeTicketPost(
            @RequestParam(value = "selectedTickets", required = false) List<String> selectedTickets,
            @RequestParam(value = "clusterIndex", required = false) Integer clusterIndex,
            @RequestParam(value = "primaryTicket", required = false) String primaryTicket,
            @RequestParam(value = "mergeReason", required = false) String mergeReason,
            HttpSession session
    ) {
        if (clusterIndex != null) {
            List<MergeRecord> activeMerges = getActiveMerges();
            List<List<MergeRecord>> clusters = buildMergeRecordClusters(activeMerges);
            if (clusterIndex >= 0 && clusterIndex < clusters.size()) {
                for (MergeRecord mr : clusters.get(clusterIndex)) {
                    mergeRecordService.cancelMerge(mr.getMergeId());
                }
                return "redirect:/admin/dashboard?tab=merge&separated=true";
            }
            return "redirect:/admin/dashboard?tab=merge";
        }

        if (selectedTickets != null && selectedTickets.size() >= 2 && mergeReason != null && !mergeReason.trim().isEmpty()) {
            if (mergeReason.trim().length() < 20) {
                return "redirect:/admin/dashboard?tab=merge&mergeError=shortReason";
            }

            List<String> blockedTickets = new ArrayList<>();
            for (String tid : selectedTickets) {
                Report r = reportService.findById(tid).orElse(null);
                if (r == null || isMergeBlocked(r.getStatus())) {
                    blockedTickets.add(tid);
                }
            }
            if (!blockedTickets.isEmpty()) {
                return "redirect:/admin/dashboard?tab=merge&mergeError=blocked&blocked=" + String.join(",", blockedTickets);
            }

            if (primaryTicket == null || primaryTicket.trim().isEmpty()) {
                return "redirect:/admin/dashboard?tab=merge&mergeError=noParent";
            }
            if (!selectedTickets.contains(primaryTicket)) {
                return "redirect:/admin/dashboard?tab=merge&mergeError=invalidParent";
            }

            String adminId = ControllerHelper.requireAnyAdminSession(session);
            if (adminId == null) {
                adminId = userService.getUserByEmail("admin@aduaja.go.id")
                        .map(User::getUserId).orElse(null);
            }
            if (adminId == null) {
                return "redirect:/admin/dashboard?tab=merge&mergeError=unauthorized";
            }

            for (String tid : selectedTickets) {
                if (tid.equals(primaryTicket)) continue;
                MergeDTO dto = new MergeDTO();
                dto.setPrimaryReportId(primaryTicket);
                dto.setMergedReportId(tid);
                dto.setReason(mergeReason.trim());
                dto.setSimilarityScore(0);
                try {
                    mergeRecordService.createMerge(dto, adminId);
                } catch (Exception e) {
                    log.error("Gagal merge tiket {} ke {}: {}", tid, primaryTicket, e.getMessage(), e);
                }
            }

            return "redirect:/admin/dashboard?tab=merge&merged=true";
        }

        return "redirect:/admin/dashboard?tab=merge";
    }

    @PostMapping("/admin/merge-ticket-panel")
    public String adminMergeTicketPanelAliasPost() {
        return "redirect:/admin/dashboard?tab=merge";
    }

    // ==========================================
    // ADMIN PUSAT — DISPOSISI
    // ==========================================

    @GetMapping("/admin/disposisi")
    public String adminDisposisiPanel(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        // SESSION CHECK
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

        String regionId = ControllerHelper.getSessionRegionId(session);
        List<Map<String, Object>> reports = new ArrayList<>();
        List<Report> validated = regionId != null ? reportService.getReportsByStatusAndRegion(Report.ReportStatus.DITERIMA, regionId) : reportService.getReportsByStatus(Report.ReportStatus.DITERIMA);
        if (!validated.isEmpty()) {
            for (Report r : validated) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getReportId());
                m.put("judul", r.getTicketNumber() != null ? r.getTicketNumber() : "Laporan #" + r.getReportId().substring(0, 8));
                m.put("kategori", r.getCategory() != null ? r.getCategory().getCategoryName() : "Lainnya");
                m.put("pelapor", r.getReporter() != null ? r.getReporter().getFullName() : "-");
                m.put("wilayah", r.getLocationHint() != null ? r.getLocationHint() : "-");
                m.put("status", "Tervalidasi");
                m.put("prioritasSistem", "Sedang");
                m.put("dinasRekomendasi", "Dinas Terkait");
                m.put("foto", r.getPhotoBase64() != null ? r.getPhotoBase64() : dummyReportImage());
                reports.add(m);
            }
        }

        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", reports.size());

        Map<String, Object> selected = null;
        if (id != null) {
            selected = reports.stream()
                    .filter(r -> r.get("id").equals(id))
                    .findFirst().orElse(reports.isEmpty() ? null : reports.get(0));
        }
        model.addAttribute("selectedReport", selected);

        // filter daftar dinas hanya yang beroperasi di region laporan terpilih
        List<Map<String, Object>> dinasList = new ArrayList<>();
        try {
            List<Agency> realAgencies;
            if (selected != null && id != null) {
                Report rpt = reportService.findById(id).orElse(null);
                if (rpt != null && rpt.getRegion() != null) {
                    realAgencies = agencyService.getActiveAgenciesByRegion(rpt.getRegion().getRegionId());
                } else {
                    realAgencies = agencyService.getActiveAgencies();
                }
            } else {
                realAgencies = agencyService.getActiveAgencies();
            }
            if (realAgencies != null) {
                dinasList = realAgencies.stream().map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getAgencyId());
                    m.put("name", a.getAgencyName());
                    m.put("kategori", a.getContactEmail() != null ? List.of(a.getContactEmail()) : List.of("Lainnya"));
                    return m;
                }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Gagal memuat daftar dinas: {}", e.getMessage(), e);
        }
        model.addAttribute("dinasList", dinasList);
        return "admin/disposisi-panel";
    }

    @PostMapping("/admin/disposisi")
    public String adminDisposisiPost(
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "dinasId", required = false) String dinasId,
            @RequestParam(value = "catatan", required = false) String catatan,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "deadline", required = false) String deadline,
            @RequestParam(value = "instructions", required = false) String instructions
    ) {
        // SESSION CHECK
        String adminId = ControllerHelper.requireAnyAdminSession(session);
        if (adminId == null) return "redirect:/admin/login";

        String ticketId = id != null ? id.trim() : null;

        try {
            if (ticketId != null && !ticketId.isEmpty()) {
                // Parse deadline if provided
                LocalDateTime deadlineDt = null;
                if (deadline != null && !deadline.isBlank()) {
                    deadlineDt = LocalDateTime.parse(deadline, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                }

                // Prefer explicit 'instructions' over generic 'catatan'
                String notes = (instructions != null && !instructions.isBlank()) ? instructions : catatan;

                // Persist priority to report and create SLA record if needed
                final String finalPriority = (priority != null && !priority.isBlank()) ? priority.trim() : null;
                if (finalPriority != null) {
                    Report rpt = reportService.findById(ticketId).orElse(null);
                    if (rpt != null) {
                        rpt.setPriority(finalPriority);
                        // Buat SLA record sesuai prioritas jika belum ada
                        boolean slaExists = slaRecordService.findByReportId(rpt.getReportId()).isPresent();
                        if (!slaExists) {
                            int durationHours = switch (finalPriority) {
                                case "Kritis" -> 24;
                                case "Tinggi" -> 48;
                                case "Sedang" -> 72;
                                default -> 120; // Rendah atau default
                            };
                            slaRecordService.createSlaRecord(rpt.getReportId(), durationHours);
                        }
                        // Try to persist priority by calling service update (will save entity)
                        reportService.updateStatus(ticketId, rpt.getStatus(), null, adminId);
                    }
                }

                // Create disposition with available metadata (notes, priority, deadline, instructions)
                dispositionService.createDisposition(ticketId, adminId, dinasId, notes, finalPriority, deadlineDt, instructions);
                Report rptUpdated = reportService.updateStatus(ticketId, Report.ReportStatus.DALAM_PENINJAUAN, notes, adminId);
                // FIX SCN-01 (3.7): Kirim notifikasi ke warga bahwa laporan sudah didisposisi
                if (rptUpdated != null && rptUpdated.getReporter() != null) {
                    notificationService.createNotification(
                        rptUpdated.getReporter().getUserId(),
                        "Laporan Didisposisi",
                        "Laporan Anda nomor " + ticketId + " telah diteruskan ke dinas terkait untuk ditangani.",
                        "REPORT", rptUpdated.getReportId()
                    );
                }
                redirectAttributes.addFlashAttribute("success", "Laporan berhasil didisposisikan ke dinas.");
            }
        } catch (Exception e) {
            log.error("Gagal disposisi laporan {}: {}", ticketId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal disposisi: " + e.getMessage());
        }

        // FIX SCN-01 (3.7): Redirect ke tab disposisi setelah berhasil disposisi
        return "redirect:/admin/disposisi";
    }

    @GetMapping("/admin/disposisi-panel")
    public String adminDisposisiPanelAlias(Model model, HttpSession session,
                                           @RequestParam(value = "id", required = false) String id) {
        return adminDisposisiPanel(model, session, id);
    }

    @PostMapping("/admin/disposisi-panel")
    public String adminDisposisiPanelAliasPost(
            @RequestParam(value = "id", required = false) String id
    ) {
        return "redirect:/admin/disposisi" + (id != null ? "?id=" + id : "");
    }

    // ==========================================
    // ADMIN PUSAT — SENGKETA
    // ==========================================

    @GetMapping("/admin/sengketa")
    public String adminSengketaPanel(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        // SESSION CHECK
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";
        String regionId = ControllerHelper.getSessionRegionId(session);
        List<DisputeRecord> realDisputes = disputeService.getPendingDisputes();
        if (regionId != null) {
            realDisputes = realDisputes.stream()
                .filter(d -> d.getReport() != null && d.getReport().getRegion() != null
                    && regionId.equals(d.getReport().getRegion().getRegionId()))
                .collect(Collectors.toList());
        }
        // DRY: gunakan konstanta DATE_FMT dari ControllerHelper
        List<Map<String, Object>> disputes = realDisputes.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getDisputeId());
            m.put("ticketId", d.getReport() != null ? d.getReport().getReportId() : "-");
            m.put("judul", d.getReasonText() != null ? d.getReasonText() : "Sengketa #" + d.getDisputeId().substring(0, 8));
            m.put("statusSengketa", d.getResolution() == null ? "Menunggu Tinjauan" : "Selesai");
            m.put("prioritas", "Sedang");
            m.put("tanggalSengketa", d.getFiledAt() != null ? d.getFiledAt().format(ControllerHelper.DATE_FMT) : "-");
            m.put("pelapor", d.getReport() != null && d.getReport().getReporter() != null
                    ? d.getReport().getReporter().getFullName() : "-");
            m.put("tanggalLaporan", d.getReport() != null && d.getReport().getSubmittedAt() != null
                    ? d.getReport().getSubmittedAt().format(ControllerHelper.DATE_FMT) : "-");
            m.put("tanggalSelesai", d.getResolvedAt() != null ? d.getResolvedAt().format(ControllerHelper.DATE_FMT) : "-");
            m.put("statusSebelum", "Selesai");
            m.put("alasanSengketa", d.getReasonText() != null ? d.getReasonText() : "-");
            String evidence = d.getEvidencePhotoUrl();
            m.put("fotoBuktiSengketa", (evidence != null && !evidence.isBlank()) ? evidence : dummyReportImage());
            m.put("fotoBuktiPerbaikan", dummyReportImage());
            m.put("keteranganDinas", d.getResolutionNotes() != null ? d.getResolutionNotes() : "-");
            m.put("dinas", "Dinas Terkait");
            m.put("petugasId", "-");
            m.put("petugasNama", "-");
            return m;
        }).collect(Collectors.toList());
        model.addAttribute("disputes", disputes);

        Map<String, Object> selected = null;
        if (id != null) {
            selected = disputes.stream()
                    .filter(d -> d.get("id").equals(id))
                    .findFirst().orElse(null);
        }
        if (selected == null && !disputes.isEmpty()) {
            selected = disputes.get(0);
        }
        model.addAttribute("selectedDispute", selected);
        return "admin/sengketa-panel";
    }

    @PostMapping("/admin/sengketa")
    public String adminSengketaPost(
            HttpSession session,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "keputusan", required = false) String keputusan,
            @RequestParam(value = "catatan", required = false) String catatan
    ) {
        String adminId = ControllerHelper.requireAnyAdminSession(session);
        if (adminId == null) return "redirect:/admin/login";

        try {
            DisputeRecord.ResolutionType resolution = "tugaskan_kembali".equalsIgnoreCase(keputusan)
                    ? DisputeRecord.ResolutionType.TUGASKAN_KEMBALI
                    : DisputeRecord.ResolutionType.TUTUP_LAPORAN;
            disputeService.resolveDispute(id, resolution, adminId, catatan);
        } catch (Exception e) {
            log.error("Gagal resolusi sengketa {}: {}", id, e.getMessage(), e);
        }
        return "redirect:/admin/sengketa" + (id != null ? "?id=" + id : "");
    }

    @GetMapping("/admin/sengketa-panel")
    public String adminSengketaPanelAlias(Model model, HttpSession session,
                                          @RequestParam(value = "id", required = false) String id) {
        return adminSengketaPanel(model, session, id);
    }

    @PostMapping("/admin/sengketa-panel")
    public String adminSengketaPanelAliasPost(
            @RequestParam(value = "id", required = false) String id
    ) {
        return "redirect:/admin/sengketa" + (id != null ? "?id=" + id : "");
    }

    // ==========================================
    // ADMIN PUSAT — SLA MONITORING
    // ==========================================

    @GetMapping("/admin/sla")
    public String slaPanel(Model model, HttpSession session) {
        // SESSION CHECK
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

        String regionId = ControllerHelper.getSessionRegionId(session);
        model.addAttribute("slaStats", slaMonitoringService.getSlaStatistics());
        model.addAttribute("lateItems", slaMonitoringService.getLateItems());
        if (regionId != null) {
            List<SlaRecord> allSla = slaRecordService.getAllRecords();
            allSla = allSla.stream()
                .filter(s -> s.getReport() != null && s.getReport().getRegion() != null
                    && regionId.equals(s.getReport().getRegion().getRegionId()))
                .collect(Collectors.toList());
            model.addAttribute("allSla", allSla);
        } else {
            model.addAttribute("allSla", slaRecordService.getAllRecords());
        }

        // Overdue tickets needing review
        List<SlaRecord> allRecords = slaRecordService.getAllRecords();
        List<Map<String, Object>> overdueForReview = allRecords.stream()
            .filter(s -> s.getCurrentStatus() == SlaRecord.SlaStatus.TERLAMBAT && !s.isOverdueReviewed())
            .map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("slaId", s.getSlaId());
                m.put("reportId", s.getReport() != null ? s.getReport().getReportId() : "-");
                m.put("ticketNumber", s.getReport() != null ? s.getReport().getTicketNumber() : "-");
                m.put("deadline", s.getSlaDeadlineAt() != null ? s.getSlaDeadlineAt().format(ControllerHelper.DATETIME_FMT) : "-");
                m.put("reportStatus", s.getReport() != null && s.getReport().getStatus() != null ? toStatusLabel(s.getReport().getStatus()) : "-");
                return m;
            }).collect(Collectors.toList());
        model.addAttribute("overdueForReview", overdueForReview);

        return "admin/sla";
    }

    // Review overdue SLA ticket
    @PostMapping("/admin/sla/review-overdue")
    public String reviewOverdueSla(
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestParam("slaId") String slaId,
            @RequestParam(value = "notes", required = false) String notes
    ) {
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";
        try {
            slaRecordService.markOverdueReviewed(slaId, notes);
            redirectAttributes.addFlashAttribute("success", "Review overdue SLA dicatat.");
        } catch (Exception e) {
            log.error("Gagal review overdue SLA {}: {}", slaId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal mereview: " + e.getMessage());
        }
        return "redirect:/admin/sla";
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private String toStatusLabel(Report.ReportStatus status) {
        if (status == null) return "Menunggu";
        return switch (status) {
            case MENUNGGU_VERIFIKASI -> "Menunggu Verifikasi";
            case MENUNGGU_REVISI -> "Menunggu Revisi";
            case DITOLAK -> "Ditolak";
            case DITERIMA -> "Diterima";
            case TERGABUNG -> "Tergabung";
            case DALAM_PENINJAUAN -> "Dalam Peninjauan";
            case DITUGASKAN -> "Ditugaskan";
            case SEDANG_BERJALAN -> "Sedang Berjalan";
            case TERTUNDA -> "Tertunda";
            case TERLAMBAT -> "Terlambat";
            case MENUNGGU_VALIDASI -> "Menunggu Konfirmasi Warga";
            case SENGKETA -> "Disengketakan";
            case DALAM_EVALUASI_SENGKETA -> "Dalam Evaluasi Sengketa";
            case SELESAI_OTOMATIS -> "Selesai Otomatis";
            case SELESAI -> "Selesai";
        };
    }

    private String toDateStr(LocalDateTime dt) {
        if (dt == null) return "-";
        // DRY: gunakan konstanta dari ControllerHelper
        return dt.format(ControllerHelper.DATE_FMT);
    }

    private Map<String, Object> toAdminValidationMap(Report r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getReportId());
        m.put("judul", r.getTicketNumber());
        m.put("kategori", r.getCategory() != null ? r.getCategory().getCategoryName() : "Lainnya");
        m.put("pelapor", r.getReporter() != null ? r.getReporter().getFullName() : "-");
        m.put("kontakPelapor", r.getReporter() != null ? r.getReporter().getEmail() : "-");
        m.put("wilayah", r.getLocationHint() != null ? r.getLocationHint() : "-");
        m.put("tanggalMasuk", toDateStr(r.getSubmittedAt()));
        m.put("status", toStatusLabel(r.getStatus()));
        // Prioritas hanya tampil jika sudah diisi admin pusat (saat disposisi)
        String prio = r.getPriority();
        m.put("prioritas", (prio != null && !prio.isBlank()) ? prio : "-");
        m.put("sisaWaktuSLA", "-");
        m.put("foto", r.getPhotoBase64() != null ? r.getPhotoBase64() : dummyReportImage());
        String lat = r.getLatitude() != null ? r.getLatitude().toPlainString() : "0";
        String lng = r.getLongitude() != null ? r.getLongitude().toPlainString() : "0";
        m.put("koordinatStr", lat + "," + lng);
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("patokan", r.getLocationHint());
        // DRY: gunakan konstanta DATETIME_FMT dari ControllerHelper
        m.put("waktuKejadian", r.getSubmittedAt() != null ? r.getSubmittedAt().format(ControllerHelper.DATETIME_FMT) : "-");
        m.put("deskripsi", r.getDescription() != null ? r.getDescription() : "-");

        // Photo manipulation detection
        if (r.getPhotoTakenAt() != null && r.getSubmittedAt() != null) {
            m.put("photoTakenAt", r.getPhotoTakenAt().format(ControllerHelper.DATETIME_FMT));
            String warning = detectPhotoManipulation(r);
            if (warning != null) {
                m.put("photoManipulationWarning", warning);
            }
        } else if (r.getPhotoBase64() != null && !r.getPhotoBase64().isBlank()
                && r.getPhotoTakenAt() == null) {
            m.put("photoTakenAt", "-");
            m.put("photoManipulationWarning", "Foto tidak memiliki metadata EXIF waktu pengambilan — potensi manipulasi");
        }
        return m;
    }

    private List<Map<String, Object>> getAdminValidationList() {
        return getAdminValidationList(null);
    }

    private List<Map<String, Object>> getAdminValidationList(String regionId) {
        List<Report> real = regionId != null
            ? reportService.getReportsByStatusAndRegion(Report.ReportStatus.MENUNGGU_VERIFIKASI, regionId)
            : reportService.getReportsByStatus(Report.ReportStatus.MENUNGGU_VERIFIKASI);
        // Tambahkan juga MENUNGGU_REVISI ke antrian validasi
        List<Report> perluRevisi = regionId != null
            ? reportService.getReportsByStatusAndRegion(Report.ReportStatus.MENUNGGU_REVISI, regionId)
            : reportService.getReportsByStatus(Report.ReportStatus.MENUNGGU_REVISI);
        real = new java.util.ArrayList<>(real);
        real.addAll(perluRevisi);
        real.sort(Comparator.nullsLast(Comparator.comparing(Report::getSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder()))));
        return real.stream().map(this::toAdminValidationMap).collect(java.util.stream.Collectors.toList());
    }

    /**
     * Queue tracking list: semua status aktif.
     * Child tiket dari merge disembunyikan; parent diperkaya dengan info merge count.
     */
    private List<Map<String, Object>> getQueueList(String regionId) {
        // FIX SCN-01 (6.9): Kumpulkan laporan dari SEMUA status termasuk SELESAI dan DITOLAK agar ada riwayat
        List<Report.ReportStatus> statuses = List.of(
            Report.ReportStatus.MENUNGGU_VERIFIKASI,
            Report.ReportStatus.MENUNGGU_REVISI,
            Report.ReportStatus.DITERIMA,
            Report.ReportStatus.DALAM_PENINJAUAN,
            Report.ReportStatus.DITUGASKAN,
            Report.ReportStatus.SEDANG_BERJALAN,
            Report.ReportStatus.MENUNGGU_VALIDASI,
            Report.ReportStatus.SELESAI,
            Report.ReportStatus.SELESAI_OTOMATIS,
            Report.ReportStatus.DITOLAK
        );
        List<Report> all = new ArrayList<>();
        for (Report.ReportStatus s : statuses) {
            List<Report> chunk = (regionId != null)
                ? reportService.getReportsByStatusAndRegion(s, regionId)
                : reportService.getReportsByStatus(s);
            if (chunk != null) all.addAll(chunk);
        }

        // Hitung merge groups: parent -> jumlah child (pakai SEMUA merge aktif, bukan yg terfilter cluster)
        List<MergeRecord> allActiveMerges = mergeRecordService.getMerges().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .collect(Collectors.toList());
        Set<String> childIds = getAllActiveChildIds();
        Map<String, Integer> parentChildCount = new HashMap<>();
        for (MergeRecord mr : allActiveMerges) {
            if (mr.getParentReport() != null) {
                parentChildCount.merge(mr.getParentReport().getReportId(), 1, Integer::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Report r : all) {
            if (childIds.contains(r.getReportId())) continue; // sembunyikan child
            if (r.getStatus() == Report.ReportStatus.TERGABUNG) continue; // safety filter
            Map<String, Object> m = toAdminValidationMap(r);
            Integer childCount = parentChildCount.get(r.getReportId());
            m.put("isMergeGroup", childCount != null && childCount > 0);
            m.put("mergeCount", childCount != null ? childCount + 1 : 1);

            // Deteksi potensi duplikat (50m + kategori sama)
            m.put("hasPotentialDuplicate", hasPotentialDuplicate(r));

            result.add(m);
        }

        // FIFO: urutkan dari terlama di atas
        result.sort(Comparator.comparing(r -> {
            try { return LocalDate.parse((String) r.get("tanggalMasuk"), ControllerHelper.DATE_FMT); }
            catch (Exception e) { return LocalDate.MIN; }
        }));
        return result;
    }

    // Pagination wrapper
    private Map<String, Object> getQueueList(String regionId, int page, int size) {
        List<Map<String, Object>> all = getQueueList(regionId);
        int totalItems = all.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / size));
        int safePage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = Math.min((safePage - 1) * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<Map<String, Object>> items = (fromIndex < totalItems) ? all.subList(fromIndex, toIndex) : new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", safePage);
        result.put("totalPages", totalPages);
        result.put("totalItems", totalItems);
        return result;
    }

    private List<MergeRecord> getActiveMerges() {
        return mergeRecordService.getMerges().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                // cluster hanya tampil selama parent belum melewati tahap validasi awal
                // Begitu parent di-disposisi (DIDISPOSISI) atau lebih jauh, cluster disembunyikan
                .filter(m -> {
                    if (m.getParentReport() == null) return false;
                    Report.ReportStatus s = m.getParentReport().getStatus();
                    return s == Report.ReportStatus.MENUNGGU_VERIFIKASI
                        || s == Report.ReportStatus.DITERIMA
                        || s == Report.ReportStatus.MENUNGGU_REVISI;
                })
                .collect(Collectors.toList());
    }

    /** Ambil SEMUA child ID dari merge aktif, tanpa filter status parent — untuk queue filtering */
    private Set<String> getAllActiveChildIds() {
        return mergeRecordService.getMerges().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .filter(m -> m.getChildReport() != null && m.getChildReport().getReportId() != null)
                .map(m -> m.getChildReport().getReportId())
                .collect(Collectors.toSet());
    }

    private Set<String> getMergedChildIds(List<MergeRecord> activeMerges) {
        Set<String> ids = new HashSet<>();
        for (MergeRecord m : activeMerges) {
            if (m.getChildReport() != null && m.getChildReport().getReportId() != null) {
                ids.add(m.getChildReport().getReportId());
            }
        }
        return ids;
    }

    private List<List<MergeRecord>> buildMergeRecordClusters(List<MergeRecord> activeMerges) {
        Map<String, List<MergeRecord>> grouped = new HashMap<>();
        for (MergeRecord m : activeMerges) {
            if (m.getParentReport() == null || m.getParentReport().getReportId() == null) continue;
            String parentId = m.getParentReport().getReportId();
            grouped.computeIfAbsent(parentId, k -> new ArrayList<>()).add(m);
        }
        List<String> parentIds = new ArrayList<>(grouped.keySet());
        parentIds.sort(Comparator.naturalOrder());
        List<List<MergeRecord>> clusters = new ArrayList<>();
        for (String parentId : parentIds) {
            clusters.add(grouped.get(parentId));
        }
        return clusters;
    }

    private List<List<Map<String, Object>>> buildMergeClusters(List<MergeRecord> activeMerges) {
        List<List<MergeRecord>> recordClusters = buildMergeRecordClusters(activeMerges);
        List<List<Map<String, Object>>> clusters = new ArrayList<>();
        for (List<MergeRecord> clusterRecords : recordClusters) {
            if (clusterRecords.isEmpty()) continue;
            List<Map<String, Object>> cluster = new ArrayList<>();
            Report parent = clusterRecords.get(0).getParentReport();
            Map<String, Object> parentMap = null;
            if (parent != null) {
                parentMap = toMergeTicketMap(parent);
                cluster.add(parentMap);
            }
            for (MergeRecord mr : clusterRecords) {
                Report child = mr.getChildReport();
                if (child != null) {
                    cluster.add(toMergeTicketMap(child));
                }
            }
            // Compute actual similarity between parent and first child
            if (parentMap != null && clusterRecords.get(0).getChildReport() != null) {
                int sim = computeReportSimilarity(parent, clusterRecords.get(0).getChildReport());
                parentMap.put("similarityScore", sim);
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    private Map<String, Object> toMergeTicketMap(Report r) {
        Map<String, Object> m = toAdminValidationMap(r);
        String ticketStatus = toMergeTicketStatus(r.getStatus());
        m.put("ticketStatus", ticketStatus);
        m.put("isMergeable", !isMergeBlocked(r.getStatus()));
        m.putIfAbsent("similarityScore", 0);
        m.putIfAbsent("deskripsi", m.getOrDefault("judul", "-"));
        if (r.getPhotoBase64() == null || r.getPhotoBase64().isBlank()) {
            m.put("foto", dummyReportImage());
        }
        m.put("hasPotentialDuplicate", hasPotentialDuplicate(r));
        return m;
    }

    private String toMergeTicketStatus(ReportStatus status) {
        if (status == null) return "menunggu";
        return switch (status) {
            case DALAM_PENINJAUAN -> "peninjauan";
            case DITUGASKAN, SEDANG_BERJALAN -> "in-progress";
            default -> "menunggu";
        };
    }

    private boolean isMergeBlocked(ReportStatus status) {
        if (status == null) return false;
        return status == ReportStatus.DALAM_PENINJAUAN
                || status == ReportStatus.DITUGASKAN
                || status == ReportStatus.SEDANG_BERJALAN
                || status == ReportStatus.SELESAI
                || status == ReportStatus.SELESAI_OTOMATIS
                || status == ReportStatus.TERGABUNG;
    }

    // DRY: method ini sekarang didelegasikan ke ControllerHelper
    // sehingga tidak perlu duplikasi di AdminDinasController
    private String dummyReportImage() {
        return ControllerHelper.dummyReportImage();
    }

    // ==========================================
    // PHOTO MANIPULATION DETECTION — 
    // ==========================================

    /**
     * Cek apakah foto dicurigai dimanipulasi berdasarkan selisih photoTakenAt vs submittedAt.
     * Returns warning message jika mencurigakan, null jika aman.
     */
    private String detectPhotoManipulation(Report report) {
        if (report.getPhotoTakenAt() == null || report.getSubmittedAt() == null) return null;
        if (report.getPhotoTakenAt().isAfter(report.getSubmittedAt())) {
            return "Waktu pengambilan foto (" + report.getPhotoTakenAt().format(ControllerHelper.DATETIME_FMT)
                    + ") setelah waktu pengiriman laporan — data EXIF mencurigakan";
        }
        long hoursDiff = java.time.Duration.between(report.getPhotoTakenAt(), report.getSubmittedAt()).toHours();
        if (hoursDiff > 24) {
            return "Foto diambil " + hoursDiff + " jam sebelum laporan dikirim — potensi penggunaan foto lama";
        }
        return null;
    }

    // ==========================================
    // DUPLICATE DETECTION — 50m radius + same category (SRS V1.0)
    // ==========================================

    /**
     * Cek apakah report memiliki potensi duplikat (50m + kategori sama).
     */
    private boolean hasPotentialDuplicate(Report report) {
        if (report == null || report.getLatitude() == null || report.getLongitude() == null
                || report.getCategory() == null || report.getCategory().getCategoryId() == null) {
            return false;
        }
        double lat = report.getLatitude().doubleValue();
        double lng = report.getLongitude().doubleValue();
        double delta = 0.00045; // ~50m dalam derajat
        BigDecimal minLat = BigDecimal.valueOf(lat - delta);
        BigDecimal maxLat = BigDecimal.valueOf(lat + delta);
        BigDecimal minLng = BigDecimal.valueOf(lng - delta);
        BigDecimal maxLng = BigDecimal.valueOf(lng + delta);

        List<Report> nearby = reportRepository.findByCategoryAndCoordinateRange(
                report.getCategory().getCategoryId(), minLat, maxLat, minLng, maxLng);

        // Filter exclude diri sendiri dan pastikan dalam 50m menggunakan haversine
        for (Report other : nearby) {
            if (other.getReportId().equals(report.getReportId())) continue;
            if (other.getLatitude() == null || other.getLongitude() == null) continue;
            double dist = haversineMeters(lat, lng,
                    other.getLatitude().doubleValue(), other.getLongitude().doubleValue());
            if (dist <= 50.0) return true;
        }
        return false;
    }

    /**
     * Hitung similarity score sederhana: 100 jika dalam 50m + kategori sama, else 0.
     */
    private int computeReportSimilarity(Report ref, Report other) {
        if (ref == null || other == null) return 0;

        // 50m + kategori sama → duplikat pasti
        if (ref.getLatitude() != null && ref.getLongitude() != null
                && other.getLatitude() != null && other.getLongitude() != null
                && ref.getCategory() != null && other.getCategory() != null
                && ref.getCategory().getCategoryId() != null
                && ref.getCategory().getCategoryId().equals(other.getCategory().getCategoryId())) {
            double dist = haversineMeters(
                    ref.getLatitude().doubleValue(), ref.getLongitude().doubleValue(),
                    other.getLatitude().doubleValue(), other.getLongitude().doubleValue());
            if (dist <= 50.0) return 100;
        }

        // Di luar 50m atau beda kategori → masih bisa mirip berdasarkan teks
        int score = 0;

        // GPS proximity (0-30 pts)
        if (ref.getLatitude() != null && ref.getLongitude() != null
                && other.getLatitude() != null && other.getLongitude() != null) {
            double dist = haversineMeters(
                    ref.getLatitude().doubleValue(), ref.getLongitude().doubleValue(),
                    other.getLatitude().doubleValue(), other.getLongitude().doubleValue());
            if      (dist <=   100) score += 30;
            else if (dist <=   500) score += 20;
            else if (dist <=  1000) score += 10;
        }

        // Same category (0 or 30 pts)
        if (ref.getCategory() != null && other.getCategory() != null
                && ref.getCategory().getCategoryId() != null
                && ref.getCategory().getCategoryId().equals(other.getCategory().getCategoryId())) {
            score += 30;
        }

        // Location hint word overlap (0-20 pts)
        if (ref.getLocationHint() != null && other.getLocationHint() != null) {
            score += wordOverlapScore(ref.getLocationHint(), other.getLocationHint(), 20);
        }

        // Description word overlap (0-20 pts)
        if (ref.getDescription() != null && other.getDescription() != null) {
            score += wordOverlapScore(ref.getDescription(), other.getDescription(), 20);
        }

        return Math.min(100, score);
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private int wordOverlapScore(String s1, String s2, int maxPts) {
        java.util.Set<String> stop = new java.util.HashSet<>(java.util.Arrays.asList(
                "yang", "dan", "di", "ke", "dari", "ini", "itu", "ada",
                "tidak", "dengan", "untuk", "pada", "telah", "sudah", "juga", "atau"));
        java.util.Set<String> w1 = new java.util.HashSet<>();
        for (String w : s1.toLowerCase().split("\\W+")) {
            if (w.length() > 2 && !stop.contains(w)) w1.add(w);
        }
        java.util.Set<String> w2 = new java.util.HashSet<>();
        for (String w : s2.toLowerCase().split("\\W+")) {
            if (w.length() > 2 && !stop.contains(w)) w2.add(w);
        }
        if (w1.isEmpty() || w2.isEmpty()) return 0;
        long common = w1.stream().filter(w2::contains).count();
        return (int) Math.round((double) common / Math.max(w1.size(), w2.size()) * maxPts);
    }
}
