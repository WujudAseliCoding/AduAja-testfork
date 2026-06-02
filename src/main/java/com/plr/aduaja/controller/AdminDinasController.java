package com.plr.aduaja.controller;

import lombok.extern.slf4j.Slf4j;
import com.plr.aduaja.dto.CreatePetugasDTO;
import com.plr.aduaja.model.*;
import com.plr.aduaja.repository.RegionRepository;
import com.plr.aduaja.repository.SlaRecordRepository;
import com.plr.aduaja.repository.TaskEvidenceRepository;
import com.plr.aduaja.repository.TaskPostponementRepository;
import com.plr.aduaja.repository.UserProfileRepository;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class AdminDinasController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FieldTaskService fieldTaskService;

    @Autowired
    private DispositionService dispositionService;

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private SlaRecordService slaRecordService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TaskEvidenceRepository taskEvidenceRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private SlaRecordRepository slaRecordRepository;

    @Autowired
    private TaskPostponementRepository taskPostponementRepository;

    @Autowired
    private ConfirmationService confirmationService;

    @GetMapping("/admin/dinas/dashboard")
    public String adminDinasDashboard(Model model, HttpSession session) {
        // SESSION CHECK — semua halaman admin harus login
        String sessionUserId = ControllerHelper.requireAgencySession(session);
        if (sessionUserId == null) return "redirect:/admin/login";

        String agencyId = ControllerHelper.getSessionAgencyId(session);
        String agencyName = ControllerHelper.getSessionAgencyName(session);

        model.addAttribute("dinasName", agencyName != null ? agencyName : "Dinas Pekerjaan Umum");

        List<Disposition> allDisp;
        if (agencyId != null) {
            allDisp = dispositionService.getDispositionsByAgency(agencyId);
        } else {
            allDisp = dispositionService.getAllDispositions();
        }

        long diterima = allDisp.size();
        long diproses = allDisp.stream()
                .filter(d -> d.getReport() != null)
                .flatMap(d -> fieldTaskService.getTasksByReport(d.getReport().getReportId()).stream())
                .filter(t -> t.getTaskStatus() == FieldTask.TaskStatus.SEDANG_DIKERJAKAN)
                .count();
        long selesai = allDisp.stream()
                .filter(d -> d.getReport() != null)
                .flatMap(d -> fieldTaskService.getTasksByReport(d.getReport().getReportId()).stream())
                .filter(t -> t.getTaskStatus() == FieldTask.TaskStatus.SELESAI)
                .count();
        long baru = allDisp.stream()
                .filter(d -> d.getReport() != null)
                .flatMap(d -> fieldTaskService.getTasksByReport(d.getReport().getReportId()).stream())
                .filter(t -> t.getTaskStatus() == FieldTask.TaskStatus.BARU)
                .count();

        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(Map.of("title", "Laporan Diterima", "value", diterima, "icon", "inbox", "bgColor", "bg-blue-100", "color", "text-blue-600"));
        stats.add(Map.of("title", "Tugas Baru", "value", baru, "icon", "inbox", "bgColor", "bg-indigo-100", "color", "text-indigo-600"));
        stats.add(Map.of("title", "Dalam Penanganan", "value", diproses, "icon", "wrench", "bgColor", "bg-yellow-100", "color", "text-yellow-600"));
        stats.add(Map.of("title", "Selesai", "value", selesai, "icon", "check-circle", "bgColor", "bg-green-100", "color", "text-green-600"));
        model.addAttribute("stats", stats);

        List<Map<String, Object>> pendingAssignments = new ArrayList<>();
        for (Disposition d : allDisp) {
            if (d.getReport() != null) {
                List<FieldTask> existingTasks = fieldTaskService.getTasksByReport(d.getReport().getReportId());
                if (existingTasks.isEmpty()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", d.getReport().getReportId());
                    m.put("judul", d.getReport().getTicketNumber() != null ? d.getReport().getTicketNumber() : "Laporan");
                    m.put("kategori", d.getReport().getCategory() != null ? d.getReport().getCategory().getCategoryName() : "Lainnya");
                    m.put("prioritas", d.getPriority() != null ? d.getPriority() : "Sedang");
                    // SCN-09: Ambil SLA status real dari SlaRecord
                    String slaStatus = "-";
                    try {
                        SlaRecord slaRec = slaRecordRepository.findByReportReportId(d.getReport().getReportId()).orElse(null);
                        if (slaRec != null && slaRec.getCurrentStatus() != null) {
                            slaStatus = slaRec.getCurrentStatus() == SlaRecord.SlaStatus.TERLAMBAT ? "Terlambat" : slaRec.getCurrentStatus().name();
                        }
                    } catch (Exception ignored) {}
                    m.put("slaStatus", slaStatus);
                    pendingAssignments.add(m);
                }
            }
        }
        model.addAttribute("pendingAssignments", pendingAssignments);

        List<Map<String, Object>> petugasList = buildPetugasList(agencyId);
        model.addAttribute("availablePetugas", petugasList.isEmpty() ? new ArrayList<>() : petugasList);

        // Badge: jumlah penundaan menunggu persetujuan (untuk notifikasi di nav)
        long pendingPostponementCount = taskPostponementRepository
                .findByApprovalStatus(TaskPostponement.ApprovalStatus.MENUNGGU).size();
        model.addAttribute("pendingPostponementCount", pendingPostponementCount);

        return "admin/dinas/dinas-dashboard";
    }

    @GetMapping("/admin/dinas/dinas-dashboard")
    public String adminDinasDashboardAlias(Model model, HttpSession session) {
        return adminDinasDashboard(model, session);
    }

    private String computeOfficerStatus(String officerId) {
        Optional<OfficerAttendance> shift = attendanceService.getCurrentShift(officerId);
        if (shift.isEmpty()) return "Selesai Shift";
        OfficerAttendance.ShiftStatus shiftStatus = shift.get().getShiftStatus();
        if (shiftStatus == OfficerAttendance.ShiftStatus.SELESAI_SHIFT) return "Selesai Shift";
        if (shiftStatus == OfficerAttendance.ShiftStatus.ISTIRAHAT) return "Istirahat";
        List<FieldTask> activeTasks = fieldTaskService.getTasksByOfficerAndStatus(officerId, FieldTask.TaskStatus.SEDANG_DIKERJAKAN);
        if (!activeTasks.isEmpty()) return "Sedang Bertugas";
        return "Siap Bertugas";
    }

    private List<Map<String, Object>> buildPetugasList(String agencyId) {
        List<User> realPetugas;
        if (agencyId != null) {
            realPetugas = userRepository.findByRoleAndAgencyAgencyId(User.Role.PETUGAS, agencyId);
        } else {
            realPetugas = userService.findByRole(User.Role.PETUGAS);
        }
        return realPetugas.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getUserId());
            m.put("nama", p.getFullName());
            UserProfile profile = userProfileRepository.findByUserUserId(p.getUserId()).orElse(null);
            m.put("nip", profile != null && profile.getNip() != null ? profile.getNip() : "-");
            m.put("statusKetersediaan", computeOfficerStatus(p.getUserId()));
            m.put("wilayahTugas", profile != null && profile.getWilayahTugas() != null
                    ? profile.getWilayahTugas().getRegionName() : "-");
            m.put("tugasAktif", (int) fieldTaskService.getTasksByOfficerAndStatus(p.getUserId(), FieldTask.TaskStatus.SEDANG_DIKERJAKAN).size());
            m.put("kontak", p.getEmail());
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/admin/dinas/queue")
    public String adminDinasQueue(
            Model model,
            HttpSession session,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page
    ) {
        // SESSION CHECK
        if (ControllerHelper.requireAgencySession(session) == null) return "redirect:/admin/login";

        String agencyId = ControllerHelper.getSessionAgencyId(session);
        String agencyName = ControllerHelper.getSessionAgencyName(session);

        model.addAttribute("dinasName", agencyName != null ? agencyName : "Dinas Pekerjaan Umum");
        List<Map<String, Object>> laporanDinas = new ArrayList<>();
        List<Disposition> realDispositions;
        if (agencyId != null) {
            realDispositions = dispositionService.getDispositionsByAgency(agencyId);
        } else {
            realDispositions = dispositionService.getAllDispositions();
        }
        if (!realDispositions.isEmpty()) {
            // DRY: gunakan konstanta DATE_FMT dari ControllerHelper
            for (Disposition d : realDispositions) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", d.getReport() != null ? d.getReport().getReportId() : "-");
                m.put("judul", d.getReport() != null ? (d.getReport().getTicketNumber() != null ? d.getReport().getTicketNumber() : "Laporan") : "Disposisi");
                m.put("kategori", d.getReport() != null && d.getReport().getCategory() != null ? d.getReport().getCategory().getCategoryName() : "Lainnya");
                m.put("pelapor", d.getReport() != null && d.getReport().getReporter() != null ? d.getReport().getReporter().getFullName() : "-");
                m.put("wilayah", d.getReport() != null && d.getReport().getLocationHint() != null ? d.getReport().getLocationHint() : "-");
                m.put("tanggalDisposisi", d.getDispatchedAt() != null ? d.getDispatchedAt().format(ControllerHelper.DATE_FMT) : "-");
                m.put("prioritas", d.getPriority() != null ? d.getPriority() : "Sedang");
                m.put("deadline", d.getDeadline() != null ? d.getDeadline().format(ControllerHelper.DATETIME_FMT) : "-");
                m.put("sisaWaktu", d.getDeadline() != null ? hitungSisaWaktu(d.getDeadline()) : "-");
                // SCN-09: Cek SLA record untuk set status yang benar
                boolean slaLate = false;
                try {
                    if (d.getReport() != null) {
                        SlaRecord slaRec = slaRecordRepository.findByReportReportId(d.getReport().getReportId()).orElse(null);
                        if (slaRec != null && slaRec.getCurrentStatus() == SlaRecord.SlaStatus.TERLAMBAT) {
                            slaLate = true;
                        }
                    }
                } catch (Exception ignored) {}
                m.put("slaLate", slaLate);
                m.put("status", slaLate ? "Terlambat SLA" : "Belum Ditindaklanjuti");
                laporanDinas.add(m);
            }
        }

        // SCN-09 FIX: count laporan dengan SLA terlambat (berdasarkan flag slaLate, bukan string status hardcoded)
        long terlambatCount = laporanDinas.stream().filter(r -> Boolean.TRUE.equals(r.get("slaLate"))).count();
        int pageSize = 10;
        int totalCount = laporanDinas.size();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        int startIndex = (page - 1) * pageSize + 1;
        int endIndex = Math.min(page * pageSize, totalCount);

        model.addAttribute("laporanDinas", laporanDinas);
        model.addAttribute("terlambatCount", (int) terlambatCount);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("startIndex", startIndex);
        model.addAttribute("endIndex", endIndex);
        return "admin/dinas/dinas-queue";
    }

    @GetMapping("/admin/dinas/dinas-queue")
    public String adminDinasQueueAlias(Model model, HttpSession session,
                                       @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        return adminDinasQueue(model, session, page);
    }

    @GetMapping("/admin/dinas/penugasan")
    public String adminDinasPenugasan(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        // SESSION CHECK
        if (ControllerHelper.requireAgencySession(session) == null) return "redirect:/admin/login";

        String agencyId = ControllerHelper.getSessionAgencyId(session);
        String agencyName = ControllerHelper.getSessionAgencyName(session);
        model.addAttribute("dinasName", agencyName != null ? agencyName : "Dinas Pekerjaan Umum");

        List<Map<String, Object>> incomingReports = new ArrayList<>();
        List<Disposition> allDisp;
        if (agencyId != null) {
            allDisp = dispositionService.getDispositionsByAgency(agencyId);
        } else {
            allDisp = dispositionService.getAllDispositions();
        }
        // DRY: gunakan konstanta DATE_FMT dari ControllerHelper
        for (Disposition d : allDisp) {
            if (d.getReport() != null) {
                List<FieldTask> existing = fieldTaskService.getTasksByReport(d.getReport().getReportId());
                if (existing.isEmpty()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", d.getReport().getReportId());
                    m.put("judul", d.getReport().getTicketNumber() != null ? d.getReport().getTicketNumber() : "Laporan");
                    m.put("kategori", d.getReport().getCategory() != null ? d.getReport().getCategory().getCategoryName() : "Lainnya");
                    m.put("prioritas", d.getPriority() != null ? d.getPriority() : "Sedang");
                    m.put("tanggalDisposisi", d.getDispatchedAt() != null ? d.getDispatchedAt().format(ControllerHelper.DATE_FMT) : "-");
                    m.put("wilayah", d.getReport().getLocationHint() != null ? d.getReport().getLocationHint() : "-");
                    m.put("deadline", d.getDeadline() != null ? d.getDeadline().format(ControllerHelper.DATETIME_FMT) : "-");
                    m.put("instruksiAdmin", d.getInstructions() != null ? d.getInstructions()
                            : (d.getNotes() != null ? d.getNotes() : "-"));
                    m.put("foto", d.getReport().getPhotoBase64() != null ? d.getReport().getPhotoBase64() : dummyReportImage());
                    incomingReports.add(m);
                }
            }
        }

        List<Map<String, Object>> petugasList = buildPetugasList(agencyId);
        model.addAttribute("incomingReports", incomingReports);
        model.addAttribute("petugasList", petugasList);

        Map<String, Object> selected = null;
        if (id != null && !id.trim().isEmpty()) {
            String targetId = id.trim();
            for (Map<String, Object> r : incomingReports) {
                Object rid = r.get("id");
                if (rid != null && targetId.equals(String.valueOf(rid))) {
                    selected = r;
                    break;
                }
            }
            if (selected == null && !incomingReports.isEmpty()) selected = incomingReports.get(0);
        } else if (!incomingReports.isEmpty()) {
            selected = incomingReports.get(0);
        }
        model.addAttribute("selectedReport", selected);
        return "admin/dinas/penugasan-petugas";
    }

    @PostMapping("/admin/dinas/penugasan")
    public String adminDinasPenugasanPost(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "petugasId", required = false) String petugasId,
            @RequestParam(value = "catatan", required = false) String catatan,
            HttpSession session
    ) {
        try {
            String adminDinasId = ControllerHelper.requireAgencySession(session);
            if (adminDinasId == null) return "redirect:/admin/login";
            // Ambil userId admin dinas dari session (bukan hardcoded)
            String userId = (String) session.getAttribute("userId");
            if (userId == null) return "redirect:/admin/login";
            fieldTaskService.createTask(id, petugasId, userId);
        } catch (Exception e) {
            log.error("Gagal penugasan petugas: {}", e.getMessage(), e);
        }
        return "redirect:/admin/dinas/penugasan" + (id != null ? "?id=" + id : "");
    }

    @GetMapping("/admin/dinas/penugasan-petugas")
    public String adminDinasPenugasanAlias(Model model, HttpSession session,
                                           @RequestParam(value = "id", required = false) String id) {
        return adminDinasPenugasan(model, session, id);
    }

    @PostMapping("/admin/dinas/penugasan-petugas")
    public String adminDinasPenugasanAliasPost(
            @RequestParam(value = "id", required = false) String id
    ) {
        return "redirect:/admin/dinas/penugasan" + (id != null ? "?id=" + id : "");
    }

    @GetMapping("/admin/dinas/progress")
    public String adminDinasProgress(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        // SESSION CHECK
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

        List<Map<String, Object>> ticketsInProgress = new ArrayList<>();

        // SCN-09/10 FIX: Tampilkan SEDANG_DIKERJAKAN *dan* TERTUNDA agar admin bisa manage SLA
        List<FieldTask> realTasks = new ArrayList<>();
        realTasks.addAll(fieldTaskService.getTasksByStatus(FieldTask.TaskStatus.SEDANG_DIKERJAKAN));
        realTasks.addAll(fieldTaskService.getTasksByStatus(FieldTask.TaskStatus.TERTUNDA));

        for (FieldTask t : realTasks) {
            try {
                Map<String, Object> m = new HashMap<>();
                m.put("id", t.getTaskId());
                m.put("judul", t.getReport() != null ? (t.getReport().getTicketNumber() != null ? t.getReport().getTicketNumber() : "Laporan") : "Tugas");
                m.put("kategori", t.getReport() != null && t.getReport().getCategory() != null ? t.getReport().getCategory().getCategoryName() : "Lainnya");
                String prioritas = "Sedang";
                if (t.getReport() != null) {
                    Optional<Disposition> disp = dispositionService.getDispositionByReportId(t.getReport().getReportId());
                    if (disp.isPresent() && disp.get().getPriority() != null) {
                        prioritas = disp.get().getPriority();
                    }
                }
                m.put("prioritas", prioritas);
                m.put("pelapor", t.getReport() != null && t.getReport().getReporter() != null ? t.getReport().getReporter().getFullName() : "-");
                m.put("officerName", t.getOfficer() != null ? t.getOfficer().getFullName() : "-");
                String deadlineStr = "-";
                String slaStatus = "-";
                String slaId = null;
                if (t.getSlaRecord() != null) {
                    slaId = t.getSlaRecord().getSlaId();
                    if (t.getSlaRecord().getSlaDeadlineAt() != null) {
                        deadlineStr = t.getSlaRecord().getSlaDeadlineAt().format(ControllerHelper.DATETIME_FMT);
                    }
                    slaStatus = t.getSlaRecord().getCurrentStatus() != null
                            ? t.getSlaRecord().getCurrentStatus().name() : "-";
                }
                m.put("deadline", deadlineStr);
                m.put("slaStatus", slaStatus);
                m.put("slaId", slaId != null ? slaId : "");
                m.put("foto", dummyReportImage());
                // statusTask: status tugas sebenarnya (SEDANG_DIKERJAKAN atau TERTUNDA)
                m.put("statusTask", t.getTaskStatus() != null ? t.getTaskStatus().name() : "BARU");
                m.put("statusLabel", t.getTaskStatus() == FieldTask.TaskStatus.TERTUNDA ? "Tertunda" : "Dalam Penanganan");
                // postponementReason: ambil alasan dari postponement terakhir (MENUNGGU / DISETUJUI)
                String postponementReason = null;
                List<TaskPostponement> postponements = taskPostponementRepository.findByTaskTaskIdOrderByRequestedAtDesc(t.getTaskId());
                if (!postponements.isEmpty()) {
                    TaskPostponement latest = postponements.get(0);
                    postponementReason = latest.getReason();
                }
                m.put("postponementReason", postponementReason);
                List<Map<String, Object>> ph = new ArrayList<>();
                if (t.getStartedAt() != null) {
                    ph.add(Map.of("tanggal", t.getStartedAt().format(ControllerHelper.DATE_FMT), "petugas",
                        t.getOfficer() != null ? t.getOfficer().getFullName() : "-",
                        "keterangan", "Pengerjaan dimulai", "estimasi", "-"));
                }
                m.put("progressHistory", ph);
                ticketsInProgress.add(m);
            } catch (Exception ex) {
                log.warn("Gagal mapping task {} ke map: {}", t.getTaskId(), ex.getMessage());
            }
        }
        model.addAttribute("ticketsInProgress", ticketsInProgress);


        // SCN-09 FIX: Hapus auto-select fallback — hanya pilih jika ada parameter id yang valid
        Map<String, Object> selected = null;
        if (id != null && !id.trim().isEmpty()) {
            String targetId = id.trim();
            for (Map<String, Object> t : ticketsInProgress) {
                Object tid = t.get("id");
                if (tid != null && targetId.equals(String.valueOf(tid))) {
                    selected = t;
                    break;
                }
            }
        }
        model.addAttribute("selectedTicket", selected);

        // SCN-10: Tambah daftar penundaan yang menunggu persetujuan ke model
        List<TaskPostponement> pendingPostponements = taskPostponementRepository
                .findByApprovalStatus(TaskPostponement.ApprovalStatus.MENUNGGU);
        model.addAttribute("pendingPostponements", pendingPostponements);

        return "admin/dinas/progress-update";
    }

    @PostMapping("/admin/dinas/progress")
    public String adminDinasProgressPost(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "keterangan", required = false) String keterangan,
            @RequestParam(value = "estimasi", required = false) String estimasi
    ) {
        try {
            fieldTaskService.startTask(id, null, null);
        } catch (Exception e) {
            log.error("Gagal update progress: {}", e.getMessage(), e);
        }
        return "redirect:/admin/dinas/progress" + (id != null ? "?id=" + id : "");
    }

    @GetMapping("/admin/dinas/progress-update")
    public String adminDinasProgressAlias(Model model, HttpSession session,
                                          @RequestParam(value = "id", required = false) String id) {
        return adminDinasProgress(model, session, id);
    }

    @PostMapping("/admin/dinas/progress-update")
    public String adminDinasProgressAliasPost(
            @RequestParam(value = "id", required = false) String id
    ) {
        return "redirect:/admin/dinas/progress" + (id != null ? "?id=" + id : "");
    }

    @GetMapping("/admin/dinas/close")
    public String adminDinasClose(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        // SESSION CHECK
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

        List<Map<String, Object>> ticketsReady = new ArrayList<>();
        List<FieldTask> realTasks = fieldTaskService.getTasksByStatus(FieldTask.TaskStatus.SELESAI);
        if (!realTasks.isEmpty()) {
            // DRY: gunakan konstanta DATE_FMT dari ControllerHelper
            for (FieldTask t : realTasks) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", t.getTaskId());
                m.put("judul", t.getReport() != null ? (t.getReport().getTicketNumber() != null ? t.getReport().getTicketNumber() : "Laporan") : "Tugas");
                m.put("kategori", t.getReport() != null && t.getReport().getCategory() != null ? t.getReport().getCategory().getCategoryName() : "Lainnya");
                String prio = "Sedang";
                try {
                    Optional<Disposition> disp = dispositionService.getDispositionByReportId(t.getReport().getReportId());
                    if (disp.isPresent() && disp.get().getPriority() != null) prio = disp.get().getPriority();
                } catch (Exception e) { /* ignore */ }
                m.put("prioritas", prio);
                m.put("pelapor", t.getReport() != null && t.getReport().getReporter() != null ? t.getReport().getReporter().getFullName() : "-");
                m.put("wilayah", t.getReport() != null && t.getReport().getLocationHint() != null ? t.getReport().getLocationHint() : "-");
                m.put("foto", dummyReportImage());
                List<Map<String, Object>> ph = new ArrayList<>();
                if (t.getStartedAt() != null) ph.add(Map.of("tanggal", t.getStartedAt().format(ControllerHelper.DATE_FMT), "keterangan", "Pengerjaan dimulai"));
                if (t.getCompletedAt() != null) ph.add(Map.of("tanggal", t.getCompletedAt().format(ControllerHelper.DATE_FMT), "keterangan", "Pengerjaan selesai"));
                m.put("progressHistory", ph);
                ticketsReady.add(m);
            }
        }
        model.addAttribute("ticketsReadyToClose", ticketsReady);

        Map<String, Object> selected = null;
        if (id != null) {
            selected = ticketsReady.stream()
                    .filter(t -> t.get("id").equals(id))
                    .findFirst().orElse(ticketsReady.isEmpty() ? null : ticketsReady.get(0));
        }
        model.addAttribute("selectedTicket", selected);
        return "admin/dinas/close-ticket";
    }

    @PostMapping("/admin/dinas/close")
    public String adminDinasClosePost(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "keterangan", required = false) String keterangan
    ) {
        try {
            fieldTaskService.closeTaskByAdmin(id);
        } catch (Exception e) {
            log.error("Gagal close tiket {}: {}", id, e.getMessage(), e);
        }
        return "redirect:/admin/dinas/close" + (id != null ? "?id=" + id : "");
    }

    @GetMapping("/admin/dinas/close-ticket")
    public String adminDinasCloseAlias(Model model, HttpSession session,
                                       @RequestParam(value = "id", required = false) String id) {
        return adminDinasClose(model, session, id);
    }

    @PostMapping("/admin/dinas/close-ticket")
    public String adminDinasCloseAliasPost(
            @RequestParam(value = "id", required = false) String id
    ) {
        return "redirect:/admin/dinas/close" + (id != null ? "?id=" + id : "");
    }

    @GetMapping("/admin/dinas/sengketa")
    public String adminDinasSengketa(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false) String id
    ) {
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";
        String agencyId = ControllerHelper.getSessionAgencyId(session);

        List<DisputeRecord> realDisputes = disputeService.getPendingDisputes();
        if (agencyId != null) {
            realDisputes = realDisputes.stream()
                .filter(d -> d.getReport() != null
                    && dispositionService.getDispositionByReportId(d.getReport().getReportId())
                        .map(disp -> disp.getTargetAgency() != null
                            && agencyId.equals(disp.getTargetAgency().getAgencyId()))
                        .orElse(false))
                .collect(Collectors.toList());
        }

        List<Map<String, Object>> disputes = realDisputes.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            Report r = d.getReport();
            m.put("id", d.getDisputeId());
            m.put("ticketId", r != null ? r.getReportId() : "-");
            m.put("judul", d.getReasonText() != null ? d.getReasonText() : "Sengketa #" + d.getDisputeId().substring(0, 8));
            m.put("statusSengketa", d.getResolution() == null ? "Menunggu Tinjauan" : "Selesai");
            m.put("prioritas", "Sedang");
            m.put("tanggalSengketa", d.getFiledAt() != null ? d.getFiledAt().format(ControllerHelper.DATE_FMT) : "-");
            m.put("pelapor", r != null && r.getReporter() != null ? r.getReporter().getFullName() : "-");
            m.put("tanggalLaporan", r != null && r.getSubmittedAt() != null ? r.getSubmittedAt().format(ControllerHelper.DATE_FMT) : "-");
            m.put("tanggalSelesai", d.getResolvedAt() != null ? d.getResolvedAt().format(ControllerHelper.DATE_FMT) : "-");
            m.put("statusSebelum", r != null && r.getStatus() != null ? r.getStatus().name() : "-");
            m.put("alasanSengketa", d.getReasonText() != null ? d.getReasonText() : "-");
            // Foto bukti sengketa dari warga
            String evidence = d.getEvidencePhotoUrl();
            m.put("fotoBuktiSengketa", (evidence != null && !evidence.isBlank()) ? evidence : dummyReportImage());
            // Foto bukti perbaikan dari petugas (ambil dari TaskEvidence SESUDAH)
            String petugasPhoto = dummyReportImage();
            String petugasId = "-";
            String petugasNama = "-";
            if (r != null) {
                List<TaskEvidence> evs = taskEvidenceRepository.findByTaskReportReportId(r.getReportId());
                for (TaskEvidence te : evs) {
                    if (te.getEvidenceType() == TaskEvidence.EvidenceType.SESUDAH && te.getPhotoUrl() != null) {
                        petugasPhoto = te.getPhotoUrl();
                    }
                }
                // Cari petugas terakhir dari field tasks
                if (r.getFieldTasks() != null && !r.getFieldTasks().isEmpty()) {
                    List<FieldTask> tasks = r.getFieldTasks().stream()
                        .sorted(Comparator.comparing(FieldTask::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .collect(Collectors.toList());
                    FieldTask lastTask = tasks.get(0);
                    if (lastTask.getOfficer() != null) {
                        petugasId = lastTask.getOfficer().getUserId();
                        petugasNama = lastTask.getOfficer().getFullName();
                    }
                }
            }
            m.put("fotoBuktiPerbaikan", petugasPhoto);
            m.put("keteranganDinas", d.getResolutionNotes() != null ? d.getResolutionNotes() : "-");
            m.put("dinas", "Dinas Terkait");
            m.put("petugasId", petugasId);
            m.put("petugasNama", petugasNama);
            return m;
        }).collect(Collectors.toList());
        model.addAttribute("disputes", disputes);

        Map<String, Object> selected = null;
        List<Map<String, Object>> availablePetugas = new ArrayList<>();
        if (id != null && !id.trim().isEmpty()) {
            String targetId = id.trim();
            for (Map<String, Object> d : disputes) {
                Object did = d.get("id");
                if (did != null && targetId.equals(String.valueOf(did))) {
                    selected = d;
                    break;
                }
            }
        }

        // Report region untuk validasi wilayah petugas
        String reportRegionId = null;
        String reportWilayahName = null;
        if (selected != null) {
            String originalPetugasId = (String) selected.get("petugasId");
            List<User> agencyPetugas;
            if (agencyId != null) {
                agencyPetugas = userRepository.findByRoleAndAgencyAgencyId(User.Role.PETUGAS, agencyId);
            } else {
                agencyPetugas = userService.findByRole(User.Role.PETUGAS);
            }

            // Dapatkan region laporan untuk validasi wilayah
            String selDisputeId = (String) selected.get("id");
            if (selDisputeId != null) {
                Optional<DisputeRecord> optDisp = disputeService.getDisputeById(selDisputeId);
                if (optDisp.isPresent() && optDisp.get().getReport() != null) {
                    Region rr = optDisp.get().getReport().getRegion();
                    if (rr != null) {
                        reportRegionId = rr.getRegionId();
                        reportWilayahName = rr.getRegionName();
                    }
                }
            }
            model.addAttribute("reportRegionId", reportRegionId);
            model.addAttribute("reportWilayahName", reportWilayahName);

            for (User p : agencyPetugas) {
                Map<String, Object> pm = new HashMap<>();
                pm.put("id", p.getUserId());
                pm.put("nama", p.getFullName());

                // Status ketersediaan dari shift terkini
                String statusKetersediaan = "Siap Bertugas";
                Optional<OfficerAttendance> curShift = attendanceService.getCurrentShift(p.getUserId());
                if (curShift.isPresent()) {
                    OfficerAttendance.ShiftStatus ss = curShift.get().getShiftStatus();
                    if (ss == OfficerAttendance.ShiftStatus.AKTIF) {
                        List<FieldTask> activeTasks = fieldTaskService.getTasksByOfficerAndStatus(
                                p.getUserId(), FieldTask.TaskStatus.SEDANG_DIKERJAKAN);
                        statusKetersediaan = activeTasks.isEmpty() ? "Siap Bertugas" : "Sedang Bertugas";
                    } else if (ss == OfficerAttendance.ShiftStatus.ISTIRAHAT) {
                        statusKetersediaan = "Istirahat";
                    } else if (ss == OfficerAttendance.ShiftStatus.SELESAI_SHIFT) {
                        statusKetersediaan = "Selesai Shift";
                    }
                }
                pm.put("statusKetersediaan", statusKetersediaan);

                // Wilayah tugas petugas
                String petugasWilayahId = p.getRegion() != null ? p.getRegion().getRegionId() : null;
                String petugasWilayahName = p.getRegion() != null ? p.getRegion().getRegionName() : "Tidak ditentukan";
                pm.put("wilayahId", petugasWilayahId != null ? petugasWilayahId : "");
                pm.put("wilayahNama", petugasWilayahName);
                pm.put("wilayahSesuai", reportRegionId != null && reportRegionId.equals(petugasWilayahId));

                // Tandai apakah ini petugas asli yang sebelumnya mengerjakan
                boolean isOriginal = p.getUserId().equals(originalPetugasId);
                pm.put("isOriginalPetugas", isOriginal);
                if (isOriginal) {
                    pm.put("statusKetersediaan", "Pilihan Terakhir");
                }

                availablePetugas.add(pm);
            }
            model.addAttribute("availablePetugasForReassignment", availablePetugas);
            model.addAttribute("originalPetugasId", originalPetugasId);
            model.addAttribute("originalPetugasNama", selected.get("petugasNama"));
        }

        model.addAttribute("selectedDispute", selected);
        model.addAttribute("dinasName", ControllerHelper.getSessionAgencyName(session));
        return "admin/dinas/sengketa-dinas";
    }

    @PostMapping("/admin/dinas/sengketa")
    public String adminDinasSengketaPost(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "keputusan", required = false) String keputusan,
            @RequestParam(value = "catatan", required = false) String catatan,
            @RequestParam(value = "petugasId", required = false) String petugasId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String adminId = ControllerHelper.requireAnyAdminSession(session);
        if (adminId == null) return "redirect:/admin/login";

        log.info("[SENGKETA POST] id={}, keputusan={}, petugasId={}, catatan panjang={}",
            id, keputusan, petugasId, catatan != null ? catatan.length() : 0);

        // Catatan wajib diisi sebelum keputusan
        if (catatan == null || catatan.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Catatan resolusi wajib diisi.");
            return "redirect:/admin/dinas/sengketa" + (id != null ? "?id=" + id : "");
        }

        try {
            if ("diterima".equals(keputusan)) {
                if (petugasId == null || petugasId.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Pilih petugas pengganti untuk perbaikan ulang.");
                    return "redirect:/admin/dinas/sengketa" + (id != null ? "?id=" + id : "");
                }
                if (adminId != null) {
                    disputeService.resolveDispute(id, DisputeRecord.ResolutionType.TUGASKAN_KEMBALI, adminId, catatan);
                }
                try {
                    log.info("[DEBUG SENGKETA] Mulai reassign — disputeId={}, petugasId={}", id, petugasId);
                    DisputeRecord dispute = disputeService.getDisputeById(id).orElse(null);
                    if (dispute != null && dispute.getReport() != null) {
                        String reportId = dispute.getReport().getReportId();
                        log.info("[DEBUG SENGKETA] reportId={}", reportId);
                        List<FieldTask> relatedTasks = fieldTaskService.getTasksByReport(reportId);
                        log.info("[DEBUG SENGKETA] relatedTasks count={}", relatedTasks.size());
                        for (FieldTask task : relatedTasks) {
                            log.info("[DEBUG SENGKETA] Akan reassign taskId={}, current officerId={}, status={}",
                                task.getTaskId(),
                                task.getOfficer() != null ? task.getOfficer().getUserId() : "null",
                                task.getTaskStatus());
                            FieldTask updated = fieldTaskService.reassignTask(task.getTaskId(), petugasId);
                            log.info("[DEBUG SENGKETA] Selesai reassign — taskId={}, new officerId={}, new status={}",
                                updated.getTaskId(),
                                updated.getOfficer() != null ? updated.getOfficer().getUserId() : "null",
                                updated.getTaskStatus());
                            // Kirim notifikasi ke petugas yang ditugaskan ulang
                            User officer = updated.getOfficer();
                            if (officer != null) {
                                notificationService.createNotification(
                                    officer.getUserId(),
                                    "Tugas Ditugaskan Kembali",
                                    "Laporan " + (updated.getReport() != null ? updated.getReport().getTicketNumber() : "") + " telah ditugaskan kembali kepada Anda untuk perbaikan ulang.",
                                    "TASK", updated.getTaskId()
                                );
                            }
                        }
                    } else {
                        log.warn("[DEBUG SENGKETA] dispute or report is null — dispute={}", dispute);
                    }
                } catch (Exception e) {
                    log.error("[DEBUG SENGKETA] Gagal reassign petugas saat sengketa: {}", e.getMessage(), e);
                }
                redirectAttributes.addFlashAttribute("success", "Sengketa diterima. Laporan ditugaskan kembali ke petugas baru.");
                return "redirect:/admin/dinas/sengketa?reassigned=true";
            } else {
                // ditolak — Tutup Laporan
                if (adminId != null) {
                    disputeService.resolveDispute(id, DisputeRecord.ResolutionType.TUTUP_LAPORAN, adminId, catatan);
                }
                redirectAttributes.addFlashAttribute("success", "Sengketa ditolak. Laporan ditutup.");
            }
        } catch (Exception e) {
            log.error("Gagal proses sengketa {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal memproses sengketa: " + e.getMessage());
        }

        return "redirect:/admin/dinas/sengketa" + (id != null ? "?id=" + id : "");
    }

    // ==========================================
    // ADMIN DINAS — KELOLA PETUGAS
    // ==========================================

    @GetMapping("/admin/dinas/petugas")
    public String adminDinasPetugas(Model model, HttpSession session) {
        if (ControllerHelper.requireAgencySession(session) == null) return "redirect:/admin/login";

        String agencyId = ControllerHelper.getSessionAgencyId(session);
        String agencyName = ControllerHelper.getSessionAgencyName(session);
        model.addAttribute("dinasName", agencyName != null ? agencyName : "Dinas Pekerjaan Umum");

        List<Map<String, Object>> petugasList = buildPetugasList(agencyId);
        model.addAttribute("petugasList", petugasList);
        model.addAttribute("createPetugasDTO", new CreatePetugasDTO());
        List<Region> regions = regionRepository.findAll();
        model.addAttribute("regions", regions);
        // Kirim region milik agency admin ke template (untuk form create — tidak perlu pilih manual)
        if (agencyId != null) {
            agencyService.getAgencyById(agencyId).ifPresent(agency -> {
                if (agency.getRegion() != null) {
                    model.addAttribute("adminAgencyRegionId", agency.getRegion().getRegionId());
                    model.addAttribute("adminAgencyRegionName", agency.getRegion().getRegionName());
                }
            });
        }
        return "admin/dinas/petugas";
    }

    @PostMapping("/admin/dinas/petugas/create")
    public String adminDinasCreatePetugas(
            @ModelAttribute CreatePetugasDTO dto,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password minimal 6 karakter");
            return "redirect:/admin/dinas/petugas";
        }

        String agencyId = ControllerHelper.getSessionAgencyId(session);
        if (agencyId != null) {
            dto.setAgencyId(agencyId);
            // Auto-set wilayah tugas dari region agency admin — tidak perlu dipilih manual
            if (dto.getWilayahTugasRegionId() == null || dto.getWilayahTugasRegionId().isBlank()) {
                agencyService.getAgencyById(agencyId).ifPresent(agency -> {
                    if (agency.getRegion() != null) {
                        dto.setWilayahTugasRegionId(agency.getRegion().getRegionId());
                    }
                });
            }
        }

        try {
            userService.createPetugas(dto);
            redirectAttributes.addFlashAttribute("success", "Petugas " + dto.getFullName() + " berhasil dibuat");
        } catch (Exception e) {
            log.error("Gagal buat petugas: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal membuat petugas: " + e.getMessage());
        }
        return "redirect:/admin/dinas/petugas";
    }

    @PostMapping("/admin/dinas/petugas/update-profile")
    public String adminDinasUpdatePetugasProfile(
            @RequestParam("petugasId") String petugasId,
            @RequestParam(value = "nip", required = false) String nip,
            @RequestParam(value = "wilayahTugasRegionId", required = false) String wilayahTugasRegionId,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        if (ControllerHelper.requireAnyAdminSession(session) == null) return "redirect:/admin/login";

        try {
            UserProfile profile = userProfileRepository.findByUserUserId(petugasId).orElse(null);
            if (profile == null) {
                User petugas = userService.findById(petugasId)
                        .orElseThrow(() -> new RuntimeException("Petugas tidak ditemukan"));
                profile = new UserProfile();
                profile.setUser(petugas);
            }
            if (nip != null && !nip.isBlank()) {
                profile.setNip(nip);
            }
            if (wilayahTugasRegionId != null && !wilayahTugasRegionId.isBlank()) {
                Region wilayah = regionRepository.findById(wilayahTugasRegionId)
                        .orElseThrow(() -> new RuntimeException("Wilayah tidak ditemukan"));
                profile.setWilayahTugas(wilayah);
            }
            userProfileRepository.save(profile);
            redirectAttributes.addFlashAttribute("success", "Profil petugas berhasil diperbarui");
        } catch (Exception e) {
            log.error("Gagal update profil petugas: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal update profil: " + e.getMessage());
        }
        return "redirect:/admin/dinas/petugas";
    }

    // ==========================================
    // SCN-08: Trigger timeout secara manual (untuk kebutuhan testing)
    // ==========================================

    @GetMapping("/admin/dinas/trigger-timeout")
    public String adminDinasTriggerTimeout(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String adminId = ControllerHelper.requireAnyAdminSession(session);
        if (adminId == null) return "redirect:/admin/login";
        try {
            confirmationService.processTimeouts();
            redirectAttributes.addFlashAttribute("success", "Proses timeout konfirmasi berhasil dijalankan.");
        } catch (Exception e) {
            log.error("Gagal trigger timeout: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal trigger timeout: " + e.getMessage());
        }
        return "redirect:/admin/dinas/dashboard";
    }

    // ==========================================
    // SCN-10: Approve / Reject penundaan tugas
    // ==========================================

    @PostMapping("/admin/dinas/approve-postponement")
    public String adminDinasApprovePostponement(
            @RequestParam("postponementId") String postponementId,
            @RequestParam(value = "action", defaultValue = "approve") String action,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String adminId = ControllerHelper.requireAnyAdminSession(session);
        if (adminId == null) return "redirect:/admin/login";

        try {
            TaskPostponement postponement = taskPostponementRepository.findById(postponementId)
                    .orElseThrow(() -> new RuntimeException("Permintaan penundaan tidak ditemukan"));
            User adminUser = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            if ("approve".equals(action)) {
                // 1. Setujui penundaan
                postponement.setApprovalStatus(TaskPostponement.ApprovalStatus.DISETUJUI);
                postponement.setApprovedBy(adminUser);
                taskPostponementRepository.save(postponement);

                // 2. Ubah status tugas menjadi TERTUNDA (tanpa duplikasi TaskPostponement)
                FieldTask task = postponement.getTask();
                fieldTaskService.setTaskAsTertunda(task.getTaskId());

                // 3. Pause SLA jika ada
                FieldTask freshTask = fieldTaskService.getTaskById(task.getTaskId()).orElse(task);
                if (freshTask.getSlaRecord() != null) {
                    try {
                        slaRecordService.pauseSla(
                                freshTask.getSlaRecord().getSlaId(),
                                "Penundaan disetujui: " + postponement.getReason(),
                                adminId);
                    } catch (Exception slaEx) {
                        log.warn("Gagal pause SLA saat approve postponement: {}", slaEx.getMessage());
                    }
                }
                // 4. Update report status ke TERTUNDA
                Report report = task.getReport();
                if (report != null) {
                    reportService.updateStatus(report.getReportId(), Report.ReportStatus.TERTUNDA,
                        "Penundaan disetujui: " + postponement.getReason(), adminId);
                }
                redirectAttributes.addFlashAttribute("success", "Penundaan disetujui. SLA dijeda.");
            } else {
                // Tolak — status tugas tidak berubah
                postponement.setApprovalStatus(TaskPostponement.ApprovalStatus.DITOLAK);
                postponement.setApprovedBy(adminUser);
                taskPostponementRepository.save(postponement);
                redirectAttributes.addFlashAttribute("success", "Penundaan ditolak. Tugas tetap berjalan.");
            }
        } catch (Exception e) {
            log.error("Gagal proses penundaan {}: {}", postponementId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal memproses penundaan: " + e.getMessage());
        }
        return "redirect:/admin/dinas/progress";
    }

    // ==========================================
    // ADMIN DINAS — PAUSE / RESUME SLA (FR-JDA)
    // ==========================================

    @PostMapping("/admin/dinas/pause-sla")
    public String adminDinasPauseSla(
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String adminId = ControllerHelper.requireAnyAdminSession(session);
        if (adminId == null) return "redirect:/admin/login";

        try {
            FieldTask task = fieldTaskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));
            if (task.getSlaRecord() != null) {
                slaRecordService.pauseSla(task.getSlaRecord().getSlaId(), reason, adminId);
            }
            fieldTaskService.setTaskAsTertunda(taskId);
            // Update report status ke TERTUNDA
            Report report = task.getReport();
            if (report != null) {
                reportService.updateStatus(report.getReportId(), Report.ReportStatus.TERTUNDA,
                    "SLA dijeda: " + (reason != null ? reason : "Jeda oleh admin dinas"), adminId);
            }
            redirectAttributes.addFlashAttribute("success", "SLA berhasil dijeda");
        } catch (Exception e) {
            log.error("Gagal pause SLA: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal pause SLA: " + e.getMessage());
        }
        return "redirect:/admin/dinas/progress" + (taskId != null ? "?id=" + taskId : "");
    }

    @PostMapping("/admin/dinas/resume-sla")
    public String adminDinasResumeSla(
            @RequestParam(value = "taskId", required = false) String taskId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String adminId = ControllerHelper.requireAnyAdminSession(session);
        if (adminId == null) return "redirect:/admin/login";

        try {
            FieldTask task = fieldTaskService.getTaskById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task tidak ditemukan"));
            if (task.getSlaRecord() != null) {
                slaRecordService.resumeSla(task.getSlaRecord().getSlaId());
            }
            fieldTaskService.resumeTask(taskId);
            // Update report status kembali ke SEDANG_BERJALAN
            Report report = task.getReport();
            if (report != null) {
                reportService.updateStatus(report.getReportId(), Report.ReportStatus.SEDANG_BERJALAN,
                    "SLA dilanjutkan, tugas kembali dikerjakan", adminId);
            }
            redirectAttributes.addFlashAttribute("success", "SLA berhasil dilanjutkan");
        } catch (Exception e) {
            log.error("Gagal resume SLA: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal resume SLA: " + e.getMessage());
        }
        return "redirect:/admin/dinas/progress" + (taskId != null ? "?id=" + taskId : "");
    }

    private String hitungSisaWaktu(java.time.LocalDateTime deadline) {
        if (deadline == null) return "-";
        java.time.Duration dur = java.time.Duration.between(java.time.LocalDateTime.now(), deadline);
        if (dur.isNegative()) return "Terlambat";
        long days = dur.toDays();
        long hours = dur.toHours() % 24;
        if (days > 0) return days + " hari " + hours + " jam";
        return hours + " jam";
    }

    // DRY: didelegasikan ke ControllerHelper — tidak ada duplikasi dengan AdminPusatController
    private String dummyReportImage() {
        return ControllerHelper.dummyReportImage();
    }
}
