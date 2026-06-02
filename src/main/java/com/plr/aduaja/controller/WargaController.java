package com.plr.aduaja.controller;

import lombok.extern.slf4j.Slf4j;
import com.plr.aduaja.dto.CreateReportDTO;
import com.plr.aduaja.dto.DisputeDTO;
import com.plr.aduaja.model.ConfirmationRequest;
import com.plr.aduaja.model.FieldTask;
import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.Report.ReportStatus;
import com.plr.aduaja.model.ReportCategory;
import com.plr.aduaja.model.ReportRevision;
import com.plr.aduaja.model.SlaRecord;
import com.plr.aduaja.model.Region;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.RegionRepository;
import com.plr.aduaja.repository.ReportCategoryRepository;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.service.ConfirmationService;
import com.plr.aduaja.service.DisputeService;
import com.plr.aduaja.service.NotificationService;
import com.plr.aduaja.service.ReportService;
import com.plr.aduaja.service.SlaRecordService;
import com.plr.aduaja.service.SlaMonitoringService;
import com.plr.aduaja.service.FieldTaskService;
import com.plr.aduaja.service.SupabaseStorageService;
import com.plr.aduaja.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Controller
public class WargaController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReportCategoryRepository reportCategoryRepository;

    @Autowired
    private ConfirmationService confirmationService;

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private SlaRecordService slaRecordService;

    @Autowired
    private SlaMonitoringService slaMonitoringService;

    @Autowired
    private FieldTaskService fieldTaskService;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    // ABSTRAKSI: Controller tidak inject Repository langsung

    @ModelAttribute("devMode")
    public boolean isDevMode() {
        return devMode;
    }

    @GetMapping("/warga/module")
    public String wargaModule() {
        return "warga/module";
    }

    @GetMapping("/warga/dashboard")
    public String wargaDashboard(Model model, HttpSession session) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/warga/login";
        }

        User user = userOpt.get();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", user.getFullName());
        userMap.put("email", user.getEmail());
        userMap.put("id", user.getUserId());
        userMap.put("profilePhotoUrl", user.getUserProfile() != null ? user.getUserProfile().getProfilePhotoUrl() : null);
        model.addAttribute("user", userMap);
        model.addAttribute("categories", reportCategoryRepository.findByIsActiveTrue());
        model.addAttribute("userProfile", user.getUserProfile());
        model.addAttribute("regions", regionRepository.findAll());

        List<Report> dbReports = reportService.getReportsByWarga(userId);
        long total = dbReports.size();
        long diproses = dbReports.stream().filter(r -> r.getStatus() == ReportStatus.DITUGASKAN || r.getStatus() == ReportStatus.SEDANG_BERJALAN).count();
        long selesai = dbReports.stream().filter(r -> r.getStatus() == ReportStatus.SELESAI).count();
        long ditolak = dbReports.stream().filter(r -> r.getStatus() == ReportStatus.DITOLAK).count();
        long menunggu = dbReports.stream().filter(r -> r.getStatus() == ReportStatus.MENUNGGU_VERIFIKASI).count();

        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(Map.of(    "icon","file-spreadsheet",   "color","bg-blue-100 text-blue-600",   "count",total,"label","Total Laporan"));
        stats.add(Map.of("icon","clock",        "color","bg-yellow-100 text-yellow-600","count",menunggu,"label","Menunggu"));
        stats.add(Map.of("icon","wrench",       "color","bg-orange-100 text-orange-600","count",diproses,"label","Diproses"));
        stats.add(Map.of("icon","check-circle", "color","bg-green-100 text-green-600", "count",selesai,"label","Selesai"));
        stats.add(Map.of("icon","x-circle",     "color","bg-red-100 text-red-600",     "count",ditolak,"label","Ditolak"));
        model.addAttribute("stats", stats);

        List<Map<String, Object>> recentReports = new ArrayList<>();
        for (Report r : dbReports.stream().limit(5).toList()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getReportId());
            map.put("title", r.getTicketNumber());
            map.put("category", r.getCategory() != null ? r.getCategory().getCategoryName() : "Lainnya");
            map.put("status", toWargaStatusLabel(r.getStatus()));
            String colorClass = switch (r.getStatus()) {
                case MENUNGGU_VERIFIKASI -> "bg-gray-100 text-gray-700";
                case DITERIMA, DITUGASKAN, SEDANG_BERJALAN -> "bg-yellow-100 text-yellow-700";
                case TERTUNDA -> "bg-yellow-100 text-yellow-700";
                case TERLAMBAT -> "bg-red-100 text-red-700";
                case SELESAI, SELESAI_OTOMATIS -> "bg-green-100 text-green-700";
                case DITOLAK -> "bg-red-100 text-red-700";
                default -> "bg-gray-100 text-gray-700";
            };
            map.put("statusColor", colorClass);
            map.put("location", r.getLocationHint());
            map.put("date", r.getSubmittedAt() != null ? r.getSubmittedAt().toLocalDate() : java.time.LocalDate.now());
            recentReports.add(map);
        }
        model.addAttribute("recentReports", recentReports);
        model.addAttribute("unreadCount", notificationService.countUnreadByUser(userId));
        return "warga/dashboard";
    }

    @GetMapping("/warga/create-report")
    public String wargaCreateReport(Model model, HttpSession session) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";
        model.addAttribute("createReportDTO", new CreateReportDTO());
        model.addAttribute("categories", reportCategoryRepository.findByIsActiveTrue());

        List<Region> regions = regionRepository.findAll();
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Region r : regions) {
            if (!first) json.append(",");
            json.append("{\"id\":\"").append(r.getRegionId().replace("\"", "\\\""))
                .append("\",\"name\":\"").append(r.getRegionName().replace("\"", "\\\""))
                .append("\"}");
            first = false;
        }
        json.append("]");
        model.addAttribute("regionListJson", json.toString());
        return "warga/create-report";
    }

    @PostMapping("/warga/create-report")
    public String wargaCreateReportPost(
            @ModelAttribute CreateReportDTO dto,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";

        try {
            // Upload foto ke Supabase jika ada
            if (dto.getPhotoBase64() != null && !dto.getPhotoBase64().isBlank()
                    && !dto.getPhotoBase64().startsWith("http")) {
                String url = supabaseStorageService.uploadBase64(dto.getPhotoBase64(), "laporan");
                if (url != null) dto.setPhotoBase64(url);
            }
            Report report = reportService.createReport(dto, userId);
            // ABSTRAKSI: userService.findByRole() gantikan userRepository.findByRole()
            List<User> admins = userService.findByRole(User.Role.ADMIN_PUSAT);
            for (User admin : admins) {
                notificationService.createNotification(
                    admin.getUserId(),
                    "Laporan Baru",
                    "Laporan baru nomor " + report.getTicketNumber() + " telah dibuat dan menunggu validasi.",
                    "REPORT",
                    report.getReportId()
                );
            }
            redirectAttributes.addFlashAttribute("success", "Laporan berhasil dikirim!");
            return "redirect:/warga/report-detail?id=" + report.getReportId();
        } catch (Exception e) {
            log.error("Gagal buat laporan oleh user {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal mengirim laporan: " + e.getMessage());
            return "redirect:/warga/create-report";
        }
    }

    @GetMapping("/warga/report-history")
    public String wargaReportHistory(
            Model model,
            HttpSession session,
            @RequestParam(value = "status", required = false, defaultValue = "Semua") String filterStatus,
            @RequestParam(value = "q", required = false, defaultValue = "") String searchQuery,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";

        List<Report> dbReports = reportService.getReportsByWarga(userId);
        List<Map<String, Object>> allReports = new ArrayList<>();
        // DRY: gunakan konstanta DATE_FMT dari ControllerHelper
        for (Report r : dbReports) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getReportId());
            map.put("title", r.getTicketNumber());
            map.put("category", r.getCategory() != null ? r.getCategory().getCategoryName() : "Lainnya");
            map.put("status", toWargaStatusLabel(r.getStatus()));
            map.put("location", r.getLocationHint());
            map.put("date", r.getSubmittedAt().toLocalDate());
            map.put("submittedAt", r.getSubmittedAt() != null ? r.getSubmittedAt().format(ControllerHelper.DATETIME_FMT) : "-");
            // updatedAt untuk metadata kronologis
            map.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().format(ControllerHelper.DATETIME_FMT) : "-");
            map.put("description", r.getDescription());
            map.put("landmark", r.getLocationHint());
            map.put("rejectionReason", "");


            String icon = "file";
            String iconColor = "text-gray-600";
            if (r.getCategory() != null && r.getCategory().getCategoryName().contains("Sampah")) { icon = "trash-2"; iconColor = "text-emerald-600"; }
            else if (r.getCategory() != null && r.getCategory().getCategoryName().contains("Taman")) { icon = "tree-pine"; iconColor = "text-green-600"; }
            else if (r.getCategory() != null && r.getCategory().getCategoryName().contains("Penerangan")) { icon = "zap"; iconColor = "text-yellow-600"; }
            else if (r.getCategory() != null && r.getCategory().getCategoryName().contains("Jalan")) { icon = "alert-triangle"; iconColor = "text-orange-600"; }
            map.put("icon", icon);
            map.put("iconColor", iconColor);

            String colorClass = switch (r.getStatus()) {
                case MENUNGGU_VERIFIKASI -> "bg-gray-100 text-gray-700";
                case MENUNGGU_REVISI -> "bg-amber-100 text-amber-700";
                case DITERIMA -> "bg-blue-100 text-blue-700";
                case DITUGASKAN, SEDANG_BERJALAN -> "bg-yellow-100 text-yellow-700";
                case TERTUNDA -> "bg-yellow-100 text-yellow-700";
                case TERLAMBAT -> "bg-red-100 text-red-700";
                case SELESAI, SELESAI_OTOMATIS -> "bg-green-100 text-green-700";
                case DITOLAK -> "bg-red-100 text-red-700";
                case SENGKETA -> "bg-orange-100 text-orange-700";
                default -> "bg-gray-100 text-gray-700";
            };
            map.put("statusColor", colorClass);
            allReports.add(map);
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> r : allReports) {
            boolean matchStatus = filterStatus.equals("Semua") || r.get("status").equals(filterStatus);
            boolean matchQuery = searchQuery.isBlank()
                    || r.get("title").toString().toLowerCase().contains(searchQuery.toLowerCase())
                    || r.get("id").toString().toLowerCase().contains(searchQuery.toLowerCase())
                    || r.get("category").toString().toLowerCase().contains(searchQuery.toLowerCase());
            if (matchStatus && matchQuery) filtered.add(r);
        }

        List<Map<String, Object>> statusOptions = new ArrayList<>();
        Map<String, Object> allOpt = new LinkedHashMap<>();
        allOpt.put("name", "Semua");
        allOpt.put("count", allReports.size());
        statusOptions.add(allOpt);

        String[] allLabels = {
            "Menunggu Verifikasi","Menunggu Revisi","Ditolak","Diterima","Tergabung",
            "Dalam Peninjauan","Ditugaskan","Sedang Berjalan","Tertunda","Terlambat",
            "Menunggu Konfirmasi Warga","Disengketakan","Dalam Evaluasi Sengketa","Selesai Otomatis","Selesai"
        };
        for (String label : allLabels) {
            int cnt = 0;
            for (Map<String, Object> r : allReports) {
                if (label.equals(r.get("status"))) cnt++;
            }
            Map<String, Object> opt = new LinkedHashMap<>();
            opt.put("name", label);
            opt.put("count", cnt);
            statusOptions.add(opt);
        }

        int totalCount = filtered.size();
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages < 1) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int startIndex = (page - 1) * size + 1;
        int endIndex = Math.min(page * size, totalCount);
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<Map<String, Object>> paged = totalCount > 0 && fromIndex < totalCount
                ? filtered.subList(fromIndex, toIndex)
                : new ArrayList<>();

        int maxVisiblePages = 7;
        int startPage = Math.max(1, page - 3);
        int endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
        if (endPage - startPage < maxVisiblePages - 1) {
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }
        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = startPage; i <= endPage; i++) {
            pageNumbers.add(i);
        }

        model.addAttribute("reports",      paged);
        model.addAttribute("totalCount",   totalCount);
        model.addAttribute("allCount",     allReports.size());
        model.addAttribute("filterStatus", filterStatus);
        model.addAttribute("searchQuery",  searchQuery);
        model.addAttribute("statusOptions",statusOptions);
        model.addAttribute("page",         page);
        model.addAttribute("totalPages",   totalPages);
        model.addAttribute("startIndex",   startIndex);
        model.addAttribute("endIndex",     endIndex);
        model.addAttribute("pageNumbers",  pageNumbers);
        return "warga/report-history";
    }

    @GetMapping("/warga/report-detail")
    public String wargaReportDetail(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "revPage", required = false, defaultValue = "1") int revPage,
            @RequestParam(value = "revSize", required = false, defaultValue = "5") int revSize
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";
        if (id == null || id.isBlank()) return "redirect:/warga/report-history";

        Report report = reportService.findById(id).orElse(null);
        if (report == null) return "redirect:/warga/report-history";
        if (revSize < 1) revSize = 5;

        // DRY: gunakan konstanta DATETIME_FMT dari ControllerHelper
        Map<String, Object> reportMap = new HashMap<>();
        reportMap.put("id", report.getReportId());
        reportMap.put("reportId", report.getReportId());
        reportMap.put("ticketNumber", report.getTicketNumber());
        reportMap.put("title", report.getTicketNumber());
        reportMap.put("description", report.getDescription());
        reportMap.put("locationHint", report.getLocationHint());
        reportMap.put("landmark", report.getLocationHint());
        reportMap.put("location", report.getLocationHint());
        reportMap.put("latitude", report.getLatitude());
        reportMap.put("longitude", report.getLongitude());
        reportMap.put("photoUrl", report.getPhotoBase64());
        reportMap.put("photoBase64", report.getPhotoBase64());
        reportMap.put("adminNotes", report.getAdminNotes());
        reportMap.put("regionName", report.getRegion() != null ? report.getRegion().getRegionName() : "-");
        reportMap.put("rejectionReason", report.getRejectionReason());
        reportMap.put("status", toWargaStatusLabel(report.getStatus()));
        reportMap.put("category", report.getCategory() != null ? report.getCategory().getCategoryName() : "Lainnya");
        reportMap.put("submittedAt", report.getSubmittedAt() != null ? report.getSubmittedAt().format(ControllerHelper.DATETIME_FMT) : "-");
        reportMap.put("createdDate", report.getSubmittedAt() != null ? report.getSubmittedAt().format(ControllerHelper.DATETIME_FMT) : "-");
        reportMap.put("date", report.getSubmittedAt() != null ? report.getSubmittedAt().toLocalDate() : java.time.LocalDate.now());
        // Ambil SLA deadline dari DB
        String slaDeadlineStr = null;
        String slaDeadlineIso = null;
        try {
            Optional<SlaRecord> slaOpt = slaRecordService.findByReportId(report.getReportId());
            if (slaOpt.isPresent()) {
                SlaRecord sla = slaOpt.get();
                if (sla.getSlaDeadlineAt() != null) {
                    slaDeadlineStr = sla.getSlaDeadlineAt().format(ControllerHelper.DATETIME_FMT);
                    slaDeadlineIso = sla.getSlaDeadlineAt().toString(); // ISO-8601 untuk JS
                }
            }
        } catch (Exception ex) {
            log.warn("Gagal ambil SLA untuk report {}: {}", id, ex.getMessage());
        }
        reportMap.put("slaDeadline", slaDeadlineStr != null ? slaDeadlineStr : "-");
        reportMap.put("slaDeadlineIso", slaDeadlineIso); // null jika belum ada SLA

        Map<String, Object> slaInfo = null;
        try {
            slaInfo = slaMonitoringService.getReportSlaStatus(report.getReportId());
        } catch (Exception ex) {
            log.warn("Gagal ambil SLA status untuk report {}: {}", id, ex.getMessage());
        }
        model.addAttribute("slaStatus", slaInfo != null ? slaInfo.get("status") : null);
        model.addAttribute("slaDeadline", slaInfo != null ? slaInfo.get("deadline") : null);
        model.addAttribute("slaPausedMinutes", slaInfo != null ? slaInfo.get("pausedMinutes") : null);

        String taskStatus = null;
        try {
            List<FieldTask> tasks = fieldTaskService.getTasksByReport(report.getReportId());
            boolean anyTertunda = tasks.stream()
                    .anyMatch(t -> t.getTaskStatus() == FieldTask.TaskStatus.TERTUNDA);
            if (anyTertunda) {
                taskStatus = "TERTUNDA";
            } else if (tasks.stream().anyMatch(t -> t.getTaskStatus() == FieldTask.TaskStatus.SEDANG_DIKERJAKAN)) {
                taskStatus = "SEDANG_DIKERJAKAN";
            }
        } catch (Exception ex) {
            log.warn("Gagal ambil status tugas untuk report {}: {}", id, ex.getMessage());
        }
        model.addAttribute("taskStatus", taskStatus);

        List<Map<String, Object>> revisionMaps = new ArrayList<>();
        if (report.getRevisions() != null) {
            for (ReportRevision rev : report.getRevisions()) {
                Map<String, Object> revMap = new HashMap<>();
                revMap.put("oldStatus", toWargaStatusLabel(rev.getOldStatus()));
                revMap.put("newStatus", toWargaStatusLabel(rev.getNewStatus()));
                revMap.put("notes", rev.getNotes());
                revMap.put("changedAt", rev.getChangedAt());
                revisionMaps.add(revMap);
            }
        }
        // Urutkan dari terlama ke terbaru
        revisionMaps.sort(Comparator.comparing(m -> (LocalDateTime) m.get("changedAt")));
        List<?> allRevisions = new ArrayList<>(revisionMaps);
        int revisionTotalCount = allRevisions.size();
        int revisionTotalPages = (int) Math.ceil((double) revisionTotalCount / revSize);
        if (revisionTotalPages < 1) revisionTotalPages = 1;
        if (revPage < 1) revPage = 1;
        if (revPage > revisionTotalPages) revPage = revisionTotalPages;
        int revisionFromIndex = (revPage - 1) * revSize;
        int revisionToIndex = Math.min(revisionFromIndex + revSize, revisionTotalCount);
        List<?> pagedRevisions = revisionTotalCount > 0 && revisionFromIndex < revisionTotalCount
                ? allRevisions.subList(revisionFromIndex, revisionToIndex)
                : new ArrayList<>();

        int maxVisiblePages = 7;
        int revisionStartPage = Math.max(1, revPage - 3);
        int revisionEndPage = Math.min(revisionTotalPages, revisionStartPage + maxVisiblePages - 1);
        if (revisionEndPage - revisionStartPage < maxVisiblePages - 1) {
            revisionStartPage = Math.max(1, revisionEndPage - maxVisiblePages + 1);
        }
        List<Integer> revisionPageNumbers = new ArrayList<>();
        for (int i = revisionStartPage; i <= revisionEndPage; i++) {
            revisionPageNumbers.add(i);
        }

        reportMap.put("revisions", pagedRevisions);
        String cc = switch (report.getStatus()) {
            case MENUNGGU_VERIFIKASI -> "bg-gray-100 text-gray-700";
            case MENUNGGU_REVISI -> "bg-amber-100 text-amber-700";
            case DITERIMA, DITUGASKAN, SEDANG_BERJALAN -> "bg-yellow-100 text-yellow-700";
            case TERTUNDA -> "bg-yellow-100 text-yellow-700";
            case TERLAMBAT -> "bg-red-100 text-red-700";
            case SELESAI, SELESAI_OTOMATIS -> "bg-green-100 text-green-700";
            case DITOLAK -> "bg-red-100 text-red-700";
            case SENGKETA -> "bg-orange-100 text-orange-700";
            case TERGABUNG -> "bg-purple-100 text-purple-700";
            case DALAM_EVALUASI_SENGKETA -> "bg-orange-100 text-orange-700";
            default -> "bg-gray-100 text-gray-700";
        };
        reportMap.put("statusColor", cc);

        // Cek apakah konfirmasi sudah dikunci (one-time logic)
        boolean confirmationIsLocked = false;
        boolean hasPendingConfirmation = false;
        String confirmationDeadlineIso = null;
        try {
            Optional<ConfirmationRequest> confOpt = confirmationService.getByReportId(report.getReportId());
            if (confOpt.isPresent()) {
                confirmationIsLocked = Boolean.TRUE.equals(confOpt.get().getIsLocked());
                hasPendingConfirmation = !confirmationIsLocked;
                if (confOpt.get().getDeadlineAt() != null) {
                    confirmationDeadlineIso = confOpt.get().getDeadlineAt().toString();
                }
            }
        } catch (Exception ex) {
            log.warn("Gagal ambil confirmation request untuk report {}: {}", id, ex.getMessage());
        }
        model.addAttribute("confirmationIsLocked", confirmationIsLocked);
        model.addAttribute("hasPendingConfirmation", hasPendingConfirmation);
        model.addAttribute("confirmationDeadlineIso", confirmationDeadlineIso);

        // Cek apakah sengketa sudah pernah diajukan (maks 1x)
        boolean existingDispute = false;
        try {
            existingDispute = disputeService.getDisputes(report.getReportId()).stream()
                .findAny().isPresent();
        } catch (Exception ex) {
            log.warn("Gagal cek sengketa untuk report {}: {}", id, ex.getMessage());
        }
        model.addAttribute("existingDispute", existingDispute);

        // Status final = read-only
        boolean isFinalStatus = report.getStatus() == ReportStatus.SELESAI
                || report.getStatus() == ReportStatus.SELESAI_OTOMATIS
                || report.getStatus() == ReportStatus.DITOLAK;
        model.addAttribute("isFinalStatus", isFinalStatus);

        model.addAttribute("report", reportMap);
        model.addAttribute("categories", reportCategoryRepository.findByIsActiveTrue());
        model.addAttribute("revisionPage", revPage);
        model.addAttribute("revisionSize", revSize);
        model.addAttribute("revisionTotalCount", revisionTotalCount);
        model.addAttribute("revisionTotalPages", revisionTotalPages);
        model.addAttribute("revisionStartIndex", revisionTotalCount == 0 ? 0 : revisionFromIndex + 1);
        model.addAttribute("revisionEndIndex", revisionToIndex);
        model.addAttribute("revisionPageNumbers", revisionPageNumbers);
        return "warga/report-detail";
    }

    @GetMapping("/warga/notifications")
    public String wargaNotifications(
            Model model,
            HttpSession session,
            @RequestParam(value = "filter", required = false, defaultValue = "semua") String filter,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";

        List<com.plr.aduaja.model.Notification> allNotifs = filter.equals("belum-dibaca")
                ? notificationService.getUnreadNotificationsByUser(userId)
                : notificationService.getNotificationsByUser(userId);
        long unreadCount = notificationService.countUnreadByUser(userId);

        int totalCount = allNotifs.size();
        int totalPages = (int) Math.ceil((double) totalCount / size);
        if (totalPages < 1) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int startIndex = (page - 1) * size + 1;
        int endIndex = Math.min(page * size, totalCount);
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<com.plr.aduaja.model.Notification> notifs = totalCount > 0 && fromIndex < totalCount
                ? allNotifs.subList(fromIndex, toIndex)
                : new ArrayList<>();

        int maxVisiblePages = 7;
        int startPage = Math.max(1, page - 3);
        int endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
        if (endPage - startPage < maxVisiblePages - 1) {
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }
        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = startPage; i <= endPage; i++) {
            pageNumbers.add(i);
        }

        model.addAttribute("notifications", notifs);
        model.addAttribute("totalCount",  totalCount);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("filter",      filter);
        model.addAttribute("page",         page);
        model.addAttribute("totalPages",   totalPages);
        model.addAttribute("startIndex",   startIndex);
        model.addAttribute("endIndex",     endIndex);
        model.addAttribute("pageNumbers",  pageNumbers);
        return "warga/notifications";
    }

    @PostMapping("/warga/notifications/mark-read")
    public String wargaMarkAllRead(HttpSession session) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";
        notificationService.markAllAsReadByUser(userId);
        return "redirect:/warga/notifications";
    }

    // ==========================================
    // POST /warga/confirm-report — Konfirmasi laporan selesai
    // ==========================================
    @PostMapping("/warga/confirm-report")
    public String confirmReport(
            @RequestParam("id") String reportId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";
        try {
            // Validasi bahwa warga yang konfirmasi adalah pembuat laporan
            Report report = reportService.findById(reportId).orElse(null);
            if (report == null) {
                redirectAttributes.addFlashAttribute("error", "Laporan tidak ditemukan.");
                return "redirect:/warga/report-history";
            }
            if (report.getReporter() == null || !report.getReporter().getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Anda tidak berwenang mengkonfirmasi laporan ini.");
                return "redirect:/warga/report-detail?id=" + reportId;
            }
            // Cek apakah konfirmasi sudah dikunci (sudah pernah merespons)
            // Jika sudah pernah sengketa, bypass locked check — konfirmasi tetap boleh
            boolean hasExistingDispute = disputeService.getDisputes(reportId).stream().findAny().isPresent();
            java.util.Optional<ConfirmationRequest> confOpt = confirmationService.getByReportId(reportId);
            if (!hasExistingDispute && confOpt.isPresent() && Boolean.TRUE.equals(confOpt.get().getIsLocked())) {
                redirectAttributes.addFlashAttribute("error", "Konfirmasi laporan ini sudah pernah dilakukan sebelumnya.");
                return "redirect:/warga/report-detail?id=" + reportId;
            }
            confirmationService.respond(reportId, ConfirmationRequest.ResponseType.TERIMA);
            redirectAttributes.addFlashAttribute("success", "Terima kasih! Laporan telah dikonfirmasi selesai.");
        } catch (Exception e) {
            log.error("Gagal konfirmasi laporan {}: {}", reportId, e.getMessage(), e);
            // Fallback: langsung update status ke SELESAI jika belum ada confirmation request
            try {
                reportService.updateStatus(reportId, ReportStatus.SELESAI, "Dikonfirmasi oleh warga", userId);
                redirectAttributes.addFlashAttribute("success", "Laporan telah dikonfirmasi selesai.");
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("error", "Gagal konfirmasi: " + ex.getMessage());
            }
        }
        return "redirect:/warga/report-detail?id=" + reportId;
    }

    // ==========================================
    // POST /warga/dispute-report — Ajukan sengketa
    // ==========================================
    @PostMapping("/warga/dispute-report")
    public String disputeReport(
            @RequestParam("id") String reportId,
            @RequestParam("reason") String reason,
            @RequestParam(value = "evidenceBase64", required = false, defaultValue = "") String evidenceBase64,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";
        // SCN-08: Blokir pengajuan sengketa jika laporan sudah final (SELESAI atau SELESAI_OTOMATIS)
        try {
            Report reportCheck = reportService.findById(reportId).orElse(null);
            if (reportCheck != null &&
                    (reportCheck.getStatus() == ReportStatus.SELESAI_OTOMATIS
                    || reportCheck.getStatus() == ReportStatus.SELESAI)) {
                redirectAttributes.addFlashAttribute("error",
                        "Laporan sudah selesai dan tidak dapat disengketakan lagi.");
                return "redirect:/warga/report-detail?id=" + reportId;
            }
        } catch (Exception ex) {
            log.warn("Gagal cek status laporan saat dispute: {}", ex.getMessage());
        }
        // Server-side validation
        if (reason == null || reason.trim().length() < 20) {
            redirectAttributes.addFlashAttribute("error", "Alasan sengketa minimal 20 karakter.");
            return "redirect:/warga/report-detail?id=" + reportId;
        }
        if (evidenceBase64 == null || evidenceBase64.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Foto bukti sengketa wajib dilampirkan.");
            return "redirect:/warga/report-detail?id=" + reportId;
        }
        try {
            DisputeDTO dto = new DisputeDTO();
            dto.setReportId(reportId);
            dto.setReason(reason);
            // Upload bukti sengketa ke Supabase
            String evidenceUrl = evidenceBase64.isBlank() ? null :
                (evidenceBase64.startsWith("http") ? evidenceBase64 :
                 supabaseStorageService.uploadBase64(evidenceBase64, "sengketa"));
            dto.setEvidencePhotoUrl(evidenceUrl);
            disputeService.createDispute(dto, userId);
            redirectAttributes.addFlashAttribute("success", "Sengketa berhasil diajukan. Admin akan meninjau laporan Anda.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Validation errors — tampilkan langsung pesan dari service
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Gagal ajukan sengketa {}: {}", reportId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal mengajukan sengketa: " + e.getMessage());
        }
        return "redirect:/warga/report-detail?id=" + reportId;
    }

    // ==========================================
    // POST /warga/revise-report — Kirim revisi laporan
    // ==========================================
    @PostMapping("/warga/revise-report")
    public String reviseReport(
            @RequestParam("id") String reportId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "landmark", required = false) String landmark,
            @RequestParam(value = "latitude", required = false) String latitude,
            @RequestParam(value = "longitude", required = false) String longitude,
            @RequestParam(value = "photoData", required = false) String photoData,
            @RequestParam(value = "category", required = false) String category,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";
        try {
            Report report = reportService.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));
            // Validasi: hanya bisa revisi jika status MENUNGGU_REVISI
            if (report.getStatus() != ReportStatus.MENUNGGU_REVISI) {
                redirectAttributes.addFlashAttribute("error", "Revisi hanya dapat dilakukan saat laporan berstatus 'Perlu Revisi'.");
                return "redirect:/warga/report-detail?id=" + reportId;
            }
            // FIX SCN-04: Validasi minimal ada deskripsi revisi
            if (description == null || description.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Deskripsi laporan tidak boleh kosong.");
                return "redirect:/warga/report-detail?id=" + reportId;
            }
            boolean anyUpdate = false;
            // Update field yang dikirim
            if (description != null && !description.isBlank()) { report.setDescription(description); anyUpdate = true; }
            if (landmark != null && !landmark.isBlank()) { report.setLocationHint(landmark); anyUpdate = true; }
            if (latitude != null && !latitude.isBlank()) {
                try { report.setLatitude(new java.math.BigDecimal(latitude)); anyUpdate = true; } catch (Exception ignored) {}
            }
            if (longitude != null && !longitude.isBlank()) {
                try { report.setLongitude(new java.math.BigDecimal(longitude)); anyUpdate = true; } catch (Exception ignored) {}
            }
            if (photoData != null && !photoData.isBlank()) {
                String photoUrl = photoData.startsWith("http") ? photoData :
                    supabaseStorageService.uploadBase64(photoData, "laporan");
                if (photoUrl != null) { report.setPhotoBase64(photoUrl); anyUpdate = true; }
            }
            // Update kategori jika diisi
            if (category != null && !category.isBlank()) {
                reportCategoryRepository.findByCategoryName(category).ifPresent(report::setCategory);
            }
            // FIX KRITIS SCN-03: Simpan perubahan field ke DB dulu sebelum update status
            // Sebelumnya data tidak tersimpan karena tidak ada save() di sini
            if (anyUpdate) {
                reportRepository.save(report);
            }
            // Ubah status kembali ke menunggu validasi setelah revisi (edit dikunci)
            reportService.updateStatus(reportId, ReportStatus.MENUNGGU_VERIFIKASI, "Revisi dikirim oleh warga", userId);
            redirectAttributes.addFlashAttribute("success", "Revisi laporan berhasil dikirim. Admin akan meninjau kembali.");
        } catch (Exception e) {
            log.error("Gagal revisi laporan {}: {}", reportId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal mengirim revisi: " + e.getMessage());
        }
        return "redirect:/warga/report-detail?id=" + reportId;
    }

    // ==========================================
    // POST /warga/withdraw-report — Batalkan laporan
    // ==========================================
    @PostMapping("/warga/withdraw-report")
    public String withdrawReport(
            @RequestParam("id") String reportId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";
        try {
            Report report = reportService.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));
            // Pembatalan hanya boleh saat MENUNGGU_VERIFIKASI
            if (report.getStatus() != ReportStatus.MENUNGGU_VERIFIKASI) {
                redirectAttributes.addFlashAttribute("error", "Laporan hanya dapat dibatalkan saat masih dalam antrian verifikasi.");
                return "redirect:/warga/report-detail?id=" + reportId;
            }
            //  style: Pastikan hanya pembuat yang bisa membatalkan
            if (report.getReporter() == null || !report.getReporter().getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Anda tidak berwenang membatalkan laporan ini.");
                return "redirect:/warga/report-detail?id=" + reportId;
            }
            reportService.updateStatus(reportId, ReportStatus.DITOLAK, "Dibatalkan oleh pelapor", userId);
            redirectAttributes.addFlashAttribute("success", "Laporan berhasil dibatalkan.");
            return "redirect:/warga/report-history";
        } catch (Exception e) {
            log.error("Gagal membatalkan laporan {}: {}", reportId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal membatalkan laporan: " + e.getMessage());
            return "redirect:/warga/report-detail?id=" + reportId;
        }
    }

    // ==========================================
    // POST /warga/change-password — Ganti password
    // ==========================================
    @PostMapping("/warga/change-password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmNewPassword") String confirmNewPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";
        try {
            if (!newPassword.equals(confirmNewPassword)) {
                redirectAttributes.addFlashAttribute("error", "Password baru dan konfirmasi tidak cocok.");
                return "redirect:/warga/profile";
            }
            if (newPassword.length() < 8) {
                redirectAttributes.addFlashAttribute("error", "Password baru minimal 8 karakter.");
                return "redirect:/warga/profile";
            }
            User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("error", "Password saat ini salah.");
                return "redirect:/warga/profile";
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success", "Password berhasil diubah.");
        } catch (Exception e) {
            log.error("Gagal ganti password user {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal mengubah password: " + e.getMessage());
        }
        return "redirect:/warga/profile";
    }

    // ==========================================
    // GET /warga/logout — Redirect GET logout ke POST
    // ==========================================
    @GetMapping("/warga/logout")
    public String logoutGet(HttpSession session) {
        session.invalidate();
        return "redirect:/warga/login";
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private String toWargaStatusLabel(String statusName) {
        if (statusName == null || statusName.isBlank()) return "Menunggu";
        try {
            return toWargaStatusLabel(Report.ReportStatus.valueOf(statusName));
        } catch (IllegalArgumentException e) {
            return statusName;
        }
    }

    // ===========================
    // DEV MODE: Generate test reports otomatis
    // Hanya aktif saat app.dev-mode=true
    // ===========================
    private static final String PLACEHOLDER_PHOTO =
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    private static final String[][] LOKASI_TES = {
        {"Medan",  "Kota Medan",       "Kecamatan Medan Baru",       "3.5952", "98.6722"},
        {"Pekanbaru", "Kota Pekanbaru","Kecamatan Tampan",           "0.5071", "101.4478"},
        {"Tanjungpinang", "Kota Tanjungpinang", "Kecamatan Bukit Bestari", "0.9167", "104.4500"},
    };

    private static final String[] DESKRIPSI = {
        "Jalan berlubang besar di depan pasar tradisional, sudah 2 minggu tidak diperbaiki",
        "Lampu penerangan jalan umum mati total di sepanjang jalan utama, sangat gelap saat malam",
        "Taman kota tidak terawat, rumput liar setinggi lutut dan bangku taman rusak",
        "Tumpukan sampah di TPS sudah menggunung selama 3 hari tidak diangkut",
        "Saluran air tersumbat sampah, air meluap ke jalan saat hujan",
        "Trotoar rusak dan membahayakan pejalan kaki di depan sekolah dasar",
        "Pohon tumbang menutup akses jalan setelah hujan angin semalam",
        "Banjir setinggi 30 cm di pemukiman warga karena drainase buruk",
        "Jembatan gantung rusak, warga tidak bisa menyeberang sungai",
        "Gedung serbaguna bocor parah saat hujan, atap perlu diperbaiki",
        "Marka jalan pudar tidak terlihat di perempatan utama, rawan kecelakaan",
        "Fasilitas olahraga di lapangan desa rusak, ring basket patah",
        "Sumur bor warga kering, pasokan air bersih terhenti",
        "Pipa PDAM bocor menggenangi jalan selama seminggu",
        "Halte bus rusak dan tidak memiliki atap pelindung",
        "Tiang listrik miring di pinggir jalan, rawan roboh",
        "Gotong royong membersihkan kali butuh peralatan tambahan",
        "Tempat ibadah butuh renovasi atap dan penerangan",
        "Pos kamling tidak layak pakai, atap bocor dan dinding retak",
        "Jalan lingkungan belum diaspal, becek saat hujan dan berdebu saat kemarau",
    };

    @PostMapping("/warga/generate-test-reports")
    public String generateTestReports(
            @RequestParam(value = "count", defaultValue = "5") int count,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "locationMode", defaultValue = "RANDOM") String locationMode,
            @RequestParam(value = "customLat", required = false) String customLat,
            @RequestParam(value = "customLng", required = false) String customLng,
            @RequestParam(value = "regionId", required = false) String regionId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!devMode) {
            redirectAttributes.addFlashAttribute("error", "Fitur ini hanya tersedia dalam mode developer.");
            return "redirect:/warga/dashboard";
        }

        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) return "redirect:/warga/login";

        List<ReportCategory> categories = reportCategoryRepository.findByIsActiveTrue();
        if (categories.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tidak ada kategori laporan. Jalankan DataSeeder terlebih dahulu.");
            return "redirect:/warga/dashboard";
        }

        List<Region> regions = regionRepository.findAll();
        if (regions.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tidak ada region. Jalankan DataSeeder terlebih dahulu.");
            return "redirect:/warga/dashboard";
        }

        // Siapkan koordinat & region untuk lokasi tetap
        String fixedLat = null, fixedLng = null, fixedKota = null, fixedKecamatan = null;
        if ("MY_LOCATION".equals(locationMode)) {
            User user = userService.findById(userId).orElse(null);
            if (user != null && user.getUserProfile() != null
                    && user.getUserProfile().getDomisiliLatitude() != null
                    && user.getUserProfile().getDomisiliLongitude() != null) {
                fixedLat = user.getUserProfile().getDomisiliLatitude().toPlainString();
                fixedLng = user.getUserProfile().getDomisiliLongitude().toPlainString();
                Region domRegion = user.getUserProfile().getDomisiliRegion();
                if (domRegion != null) {
                    fixedKota = domRegion.getParentRegion() != null
                        ? domRegion.getParentRegion().getRegionName() : domRegion.getRegionName();
                    fixedKecamatan = domRegion.getRegionName();
                } else {
                    fixedKota = "Lokasi Saya";
                    fixedKecamatan = "Lokasi Saya";
                }
            } else {
                redirectAttributes.addFlashAttribute("warning",
                    "Profil belum memiliki koordinat domisili. Gunakan mode lokasi lain.");
                return "redirect:/warga/dashboard";
            }
        } else if (List.of("MEDAN", "PEKANBARU", "TANJUNGPINANG").contains(locationMode)) {
            for (String[] lok : LOKASI_TES) {
                if (lok[0].toUpperCase().equals(locationMode)) {
                    fixedLat = lok[3]; fixedLng = lok[4]; fixedKota = lok[0]; fixedKecamatan = lok[2];
                    break;
                }
            }
        } else if ("CUSTOM".equals(locationMode)) {
            if (customLat == null || customLng == null || customLat.isBlank() || customLng.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Isi koordinat latitude dan longitude untuk lokasi kustom.");
                return "redirect:/warga/dashboard";
            }
            fixedLat = customLat.trim();
            fixedLng = customLng.trim();
            fixedKota = "Kustom";
            fixedKecamatan = "Kustom";
        }

        // Tentukan regionId: prioritas dari form, fallback dari nama kecamatan/kota
        String effectiveRegionId = regionId;
        if (effectiveRegionId == null || effectiveRegionId.isBlank()) {
            if (fixedKecamatan != null) {
                Region r = findRegion(regions, fixedKecamatan);
                if (r != null) effectiveRegionId = r.getRegionId();
            }
            if (effectiveRegionId == null && fixedKota != null) {
                Region r = findRegion(regions, fixedKota);
                if (r != null) effectiveRegionId = r.getRegionId();
            }
        }

        int generated = 0;
        int max = Math.min(count, 20);
        Random rand = new Random();

        for (int i = 0; i < max; i++) {
            try {
                String lat, lng, kota, kecamatan;
                if (fixedLat != null) {
                    lat = fixedLat; lng = fixedLng; kota = fixedKota; kecamatan = fixedKecamatan;
                } else {
                    String[] lokasi = LOKASI_TES[rand.nextInt(LOKASI_TES.length)];
                    lat = lokasi[3]; lng = lokasi[4]; kota = lokasi[0]; kecamatan = lokasi[2];
                }

                String desc = DESKRIPSI[rand.nextInt(DESKRIPSI.length)];

                String selectedCategoryId = categoryId;
                if (selectedCategoryId == null || selectedCategoryId.isBlank()) {
                    selectedCategoryId = categories.get(rand.nextInt(categories.size())).getCategoryId();
                }

                // Untuk mode RANDOM (tanpa lokasi tetap), cari region per laporan
                String finalRegionId = effectiveRegionId;
                if (finalRegionId == null || finalRegionId.isBlank()) {
                    Region r = findRegion(regions, kecamatan);
                    if (r == null) r = findRegion(regions, kota);
                    if (r != null) finalRegionId = r.getRegionId();
                }

                CreateReportDTO dto = new CreateReportDTO();
                dto.setDescription(desc);
                dto.setLocationHint((kota != null ? kota + ", " : "") + (kecamatan != null ? kecamatan : ""));
                dto.setLatitude(new BigDecimal(lat));
                dto.setLongitude(new BigDecimal(lng));
                dto.setPhotoBase64(PLACEHOLDER_PHOTO);
                dto.setCategoryId(selectedCategoryId);
                dto.setRegionId(finalRegionId);
                dto.setPhotoTakenAt(LocalDateTime.now().minusDays(rand.nextInt(7))
                    .minusHours(rand.nextInt(24)).toString());

                reportService.createReport(dto, userId);
                generated++;
            } catch (Exception e) {
                log.warn("Gagal generate report ke-{}: {}", i + 1, e.getMessage());
            }
        }

        redirectAttributes.addFlashAttribute("success",
            "Berhasil membuat " + generated + " laporan uji coba.");
        return "redirect:/warga/dashboard";
    }

    private Region findRegion(List<Region> regions, String name) {
        return regions.stream()
            .filter(r -> r.getRegionName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    private String toWargaStatusLabel(Report.ReportStatus status) {
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
}
