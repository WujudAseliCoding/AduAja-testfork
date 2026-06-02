package com.plr.aduaja.controller;

import lombok.extern.slf4j.Slf4j;
import com.plr.aduaja.model.*;
import com.plr.aduaja.model.FieldTask.TaskStatus;
import com.plr.aduaja.service.*;
import com.plr.aduaja.util.GeoUtils;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.AuditLogRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.plr.aduaja.dto.ResetPasswordDTO;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class PetugasController {

    // ============================================================
    // Konfigurasi geofencing check-in (FR-PTG-08)
    // Pusat koordinat default wilayah kerja — dapat disesuaikan per dinas.
    // Saat ini menggunakan koordinat pusat kota sebagai fallback umum.
    // ============================================================
    private static final double DINAS_CENTER_LAT   = -6.200000;   // Pusat default (Jakarta Pusat)
    private static final double DINAS_CENTER_LON   = 106.816666;
    private static final double CHECKIN_RADIUS_KM  = 50.0;        // Radius wilayah kerja check-in
    private static final double START_TASK_RADIUS_KM = 10.0;      // Radius toleransi mulai tugas

    @Autowired
    private FieldTaskService fieldTaskService;

    @Autowired
    private UserService userService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @GetMapping("/petugas/home")
    public String petugasHome() {
        return "redirect:/petugas/dashboard";
    }

    @GetMapping("/petugas/change-password")
    public String petugasChangePasswordPage(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("forceChangePasswordUserId");
        if (userId == null) {
            return "redirect:/petugas/login";
        }
        return "petugas/change-password";
    }

    @PostMapping("/petugas/change-password")
    public String petugasChangePasswordPost(@RequestParam("newPassword") String newPassword,
                                            @RequestParam("confirmPassword") String confirmPassword,
                                            HttpSession session, RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("forceChangePasswordUserId");
        if (userId == null) {
            return "redirect:/petugas/login";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Konfirmasi password tidak cocok.");
            return "redirect:/petugas/change-password";
        }

        try {
            userService.changePassword(userId, newPassword);
            session.removeAttribute("forceChangePasswordUserId");
            redirectAttributes.addFlashAttribute("success", "Password berhasil diubah. Silakan login dengan password baru.");
            return "redirect:/petugas/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengubah password: " + e.getMessage());
            return "redirect:/petugas/change-password";
        }
    }

    // ==========================================
    // GET /petugas/forgot-password
    // ==========================================
    @GetMapping("/petugas/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("step", "email");
        return "petugas/forgot-password";
    }

    // ==========================================
    // POST /petugas/forgot-password
    // ==========================================
    @PostMapping("/petugas/forgot-password")
    public String forgotPasswordRequest(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findByEmail(email.trim());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("success",
                    "Jika email terdaftar, kode OTP telah dikirim. Masukkan kode OTP di bawah.");
                return "redirect:/petugas/forgot-password/verify?email=" + email;
            }
            otpService.generateOtpForPasswordReset(email.trim());
            redirectAttributes.addFlashAttribute("success",
                "Kode OTP telah dikirim. Masukkan kode OTP untuk melanjutkan.");
            return "redirect:/petugas/forgot-password/verify?email=" + email;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengirim OTP: " + e.getMessage());
            return "redirect:/petugas/forgot-password";
        }
    }

    // ==========================================
    // GET /petugas/forgot-password/verify
    // ==========================================
    @GetMapping("/petugas/forgot-password/verify")
    public String forgotPasswordVerifyPage(
            @RequestParam("email") String email,
            Model model) {
        model.addAttribute("email", email);
        model.addAttribute("resetDTO", new ResetPasswordDTO());
        return "petugas/forgot-password-verify";
    }

    // ==========================================
    // POST /petugas/forgot-password/verify
    // ==========================================
    @PostMapping("/petugas/forgot-password/verify")
    public String forgotPasswordVerify(
            @RequestParam("email") String email,
            @RequestParam("otpCode") String otpCode,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmNewPassword") String confirmNewPassword,
            RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("error", "Password baru dan konfirmasi tidak cocok.");
            return "redirect:/petugas/forgot-password/verify?email=" + email;
        }
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password minimal 8 karakter.");
            return "redirect:/petugas/forgot-password/verify?email=" + email;
        }
        try {
            Optional<User> userOpt = userService.findByEmail(email.trim());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email tidak ditemukan.");
                return "redirect:/petugas/forgot-password";
            }
            User user = userOpt.get();
            boolean valid = otpService.verifyOtp(user.getUserId(), otpCode);
            if (!valid) {
                redirectAttributes.addFlashAttribute("error", "Kode OTP tidak valid atau sudah kadaluarsa.");
                return "redirect:/petugas/forgot-password/verify?email=" + email;
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success",
                "Password berhasil direset! Silakan login dengan password baru.");
            return "redirect:/petugas/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal reset password: " + e.getMessage());
            return "redirect:/petugas/forgot-password/verify?email=" + email;
        }
    }


    // ==========================================
    // POST /petugas/dashboard — Aksi absensi (check-in/out/break/resume)
    // ==========================================
    @PostMapping("/petugas/dashboard")
    public String petugasDashboardPost(
            @RequestParam(value = "checkIn", required = false) Boolean checkIn,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "attendanceId", required = false) String attendanceId,
            @RequestParam(value = "latitude", required = false) java.math.BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) java.math.BigDecimal longitude,
            @RequestParam(value = "deviceInfo", required = false) String deviceInfo,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        try {
            if (checkIn != null && checkIn) {
                // FIX-4: FR-PTG-08 — Validasi geofencing di backend sebelum check-in
                try {
                    double centerLat = DINAS_CENTER_LAT;
                    double centerLon = DINAS_CENTER_LON;
                    User officer = userService.findById(userId).orElse(null);
                    if (officer != null && officer.getAgency() != null) {
                        Agency agency = officer.getAgency();
                        if (agency.getLatitude() != null && agency.getLongitude() != null) {
                            centerLat = agency.getLatitude().doubleValue();
                            centerLon = agency.getLongitude().doubleValue();
                        }
                    }

                    String ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
                    String finalDeviceInfo = (deviceInfo != null ? deviceInfo : "Unknown") + " | IP: " + ip;

                    attendanceService.checkInWithGeofence(
                            userId, latitude, longitude, finalDeviceInfo,
                            CHECKIN_RADIUS_KM, centerLat, centerLon);
                } catch (IllegalStateException geoEx) {
                    log.warn("Geofencing check-in gagal untuk petugas {}: {}", userId, geoEx.getMessage());
                    redirectAttributes.addFlashAttribute("geoError", geoEx.getMessage());
                    return "redirect:/petugas/dashboard";
                }
            } else if ("checkout".equals(action) && attendanceId != null) {
                attendanceService.checkOut(attendanceId);
            } else if ("break".equals(action) && attendanceId != null) {
                attendanceService.setBreak(attendanceId);
            } else if ("resume".equals(action) && attendanceId != null) {
                attendanceService.resumeFromBreak(attendanceId);
            }
        } catch (Exception e) {
            log.error("Gagal proses absensi petugas: {}", e.getMessage(), e);
        }

        return "redirect:/petugas/dashboard";
    }

    // ==========================================
    // POST /petugas/task-action — Aksi terhadap tugas (start/complete/postpone/dll)
    // ==========================================
    @PostMapping("/petugas/task-action")
    public String petugasTaskAction(
            @RequestParam(value = "id", required = false) String id,
            @RequestParam(value = "action", required = false, defaultValue = "start") String action,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "newOfficerId", required = false) String newOfficerId,
            @RequestParam(value = "latitude", required = false) java.math.BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) java.math.BigDecimal longitude,
            @RequestParam(value = "estimatedTime", required = false) String estimatedTime,
            @RequestParam(value = "photos", required = false) MultipartFile[] photos,
            @RequestParam(value = "documents", required = false) MultipartFile[] documents,
            @RequestParam(value = "additionalNotes", required = false) String additionalNotes,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        if (id != null) {
            try {
                switch (action) {
                    case "start" -> {
                        log.info("Petugas {} memulai tugas {}", userId, id);
                        try {
                            // FIX-5: FR-PTG-18 — Validasi jarak petugas ke lokasi tugas
                            fieldTaskService.startTask(id, latitude, longitude);
                        } catch (IllegalStateException distEx) {
                            log.warn("Validasi jarak gagal untuk tugas {}: {}", id, distEx.getMessage());
                            redirectAttributes.addFlashAttribute("taskError", distEx.getMessage());
                            return "redirect:/petugas/task-detail?id=" + id;
                        }
                    }
                    case "complete" -> fieldTaskService.completeTask(id);
                    case "postpone" -> {
                        // FIX-8: FR-PTG-27 — Ajukan penundaan, TIDAK langsung TERTUNDA
                        String catReason = description != null && !description.isBlank() ? description : "Ditunda oleh petugas";
                        String notes = additionalNotes != null ? additionalNotes.trim() : "";
                        String reason = notes.isEmpty() ? catReason : catReason + " — " + notes;
                        LocalDateTime estimated = null;
                        if (estimatedTime != null && !estimatedTime.isBlank()) {
                            try { estimated = LocalDateTime.parse(estimatedTime); }
                            catch (Exception ex) { log.warn("Format estimatedTime tidak valid: {}", estimatedTime); }
                        }
                        fieldTaskService.requestPostpone(id, reason, userId, estimated);
                        // FIX SCN-10 (2.5): Gunakan 'success' bukan 'successMsg' agar toast muncul
                        redirectAttributes.addFlashAttribute("success",
                            "Pengajuan penundaan berhasil dikirim. Menunggu persetujuan admin.");
                    }
                    case "reassign" -> {
                        String targetOfficer = newOfficerId != null ? newOfficerId : "";
                        if (!targetOfficer.isBlank()) fieldTaskService.reassignTask(id, targetOfficer);
                    }
                    case "reportback" -> {
                        log.warn("Lapor balik (invalid) untuk tugas {} oleh {}: {}", id, userId, description);
                        
                        if (photos != null && photos.length > 0) {
                            log.info("Menerima {} foto pendukung lapor balik", photos.length);
                            for (MultipartFile photo : photos) {
                                if (!photo.isEmpty()) {
                                    try {
                                        String url = supabaseStorageService.upload(photo, "bukti");
                                        fieldTaskService.saveTaskEvidenceDirect(id, url, TaskEvidence.EvidenceType.LAPOR_BALIK);
                                    } catch (Exception e) {
                                        log.error("Gagal upload foto lapor balik: {}", e.getMessage());
                                    }
                                }
                            }
                        }
                        
                        if (documents != null && documents.length > 0) {
                            log.info("Menerima {} dokumen pendukung lapor balik", documents.length);
                            for (MultipartFile document : documents) {
                                if (!document.isEmpty()) {
                                    try {
                                        String url = supabaseStorageService.upload(document, "bukti");
                                        fieldTaskService.saveTaskEvidenceDirect(id, url, TaskEvidence.EvidenceType.LAPOR_BALIK);
                                    } catch (Exception e) {
                                        log.error("Gagal upload dokumen lapor balik: {}", e.getMessage());
                                    }
                                }
                            }
                        }
                        
                        fieldTaskService.requestPostpone(id,
                            "Laporan invalid: " + (description != null ? description : "Tidak ada alasan"),
                            userId, null);
                        redirectAttributes.addFlashAttribute("success",
                            "Laporan balik berhasil dikirim. Admin akan meninjau laporan ini.");
                    }
                    case "escalation" -> {
                        log.warn("Eskalasi untuk tugas {} oleh {}: {}", id, userId, description);
                        fieldTaskService.requestPostpone(id,
                            "Eskalasi: " + (description != null ? description : "Tidak ada alasan"),
                            userId, null);
                        redirectAttributes.addFlashAttribute("success",
                            "Eskalasi berhasil dikirim. Admin akan segera merespons.");
                    }
                }
            } catch (Exception e) {
                log.error("Gagal aksi tugas {} - {}: {}", id, action, e.getMessage(), e);
            }
        }
        return "redirect:/petugas/task-detail?id=" + id;
    }

    // ==========================================
    // GET /petugas/dashboard — Halaman utama petugas
    // ==========================================
    @GetMapping("/petugas/dashboard")
    public String petugasDashboard(
            Model model,
            HttpSession session,
            @RequestParam(value = "checkIn", required = false) Boolean checkIn
    ) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        // FIX-10: Nama dinas dari agency petugas (prioritas), fallback ke wilayah tugas/domisili
        userService.findById(userId).ifPresentOrElse(officer -> {
            String dinasName = "Dinas Pekerjaan Umum"; // default fallback
            if (officer.getAgency() != null) {
                dinasName = officer.getAgency().getAgencyName();
            } else {
                UserProfile profile = officer.getUserProfile();
                if (profile != null && profile.getWilayahTugas() != null) {
                    dinasName = profile.getWilayahTugas().getRegionName();
                } else if (profile != null && profile.getDomisiliRegion() != null) {
                    dinasName = profile.getDomisiliRegion().getRegionName();
                }
            }
            model.addAttribute("user", Map.of(
                "name", officer.getFullName(),
                "dinas", dinasName
            ));
        }, () -> {
            session.invalidate();
        });

        if (!model.containsAttribute("user")) return "redirect:/petugas/login";

        List<FieldTask> realTasks = fieldTaskService.getTasksByOfficer(userId);
        log.info("[DASHBOARD] userId={}, realTasks count={}", userId, realTasks.size());
        for (FieldTask t : realTasks) {
            log.info("[DASHBOARD] taskId={}, status={}, officerId={}",
                t.getTaskId(), t.getTaskStatus(),
                t.getOfficer() != null ? t.getOfficer().getUserId() : "null");
        }
        long s  = realTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.SELESAI).count();
        long ip = realTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.SEDANG_DIKERJAKAN).count();
        long n  = realTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.BARU
                || t.getTaskStatus() == TaskStatus.DITUGASKAN_ULANG).count();
        long p  = realTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.TERTUNDA).count();
        log.info("[DASHBOARD] stats: tugasBaru={}, sedangDikerjakan={}, tertunda={}, selesai={}", n, ip, p, s);
        model.addAttribute("stats", Map.of(
            "selesaiHariIni", s, "sedangDikerjakan", ip, "tugasBaru", n, "tertunda", p));

        List<Map<String, Object>> activeTasks = realTasks.stream()
            .limit(5).map(t -> toPetugasTaskMap(t, null, null)).collect(Collectors.toList());
        model.addAttribute("activeTasks", activeTasks);

        // Absensi dari DB
        Map<String, Object> attendance = new HashMap<>();
        attendance.put("attendanceId", "-");
        attendance.put("checkedIn", false);
        attendance.put("currentStatus", "Belum Check-In");
        attendance.put("checkInTime", "-");
        attendance.put("workDuration", "00:00:00");
        attendance.put("location", "-");
        attendanceService.getCurrentShift(userId).ifPresent(shift -> {
            attendance.put("attendanceId", shift.getAttendanceId());
            attendance.put("checkedIn", shift.getCheckInAt() != null
                && shift.getShiftStatus() != OfficerAttendance.ShiftStatus.SELESAI_SHIFT);
            attendance.put("currentStatus", shift.getShiftStatus() == OfficerAttendance.ShiftStatus.AKTIF ? "Siap Bertugas"
                : shift.getShiftStatus() == OfficerAttendance.ShiftStatus.ISTIRAHAT ? "Istirahat" : "Selesai Shift");
            attendance.put("checkInTime", shift.getCheckInAt() != null
                ? shift.getCheckInAt().format(ControllerHelper.TIME_FMT) : "-");
            attendance.put("rawCheckInAt", shift.getCheckInAt() != null
                ? shift.getCheckInAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000 : null);
            attendance.put("workDuration", shift.getCheckInAt() != null
                ? formatDuration(Duration.between(shift.getCheckInAt(), LocalDateTime.now())) : "00:00:00");
            attendance.put("location", shift.getCheckInLatitude() != null
                ? shift.getCheckInLatitude() + ", " + shift.getCheckInLongitude() : "-");
        });
        model.addAttribute("attendance", attendance);
        String deviceBrowser = "-", deviceOs = "-";
        Optional<OfficerAttendance> shiftOpt = attendanceService.getCurrentShift(userId);
        if (shiftOpt.isPresent() && shiftOpt.get().getDeviceInfo() != null) {
            String[] parts = shiftOpt.get().getDeviceInfo().split("/");
            deviceBrowser = parts.length > 0 ? parts[0].trim() : "-";
            deviceOs = parts.length > 1 ? parts[1].trim() : "-";
        }
        model.addAttribute("deviceInfo", Map.of("browser", deviceBrowser, "os", deviceOs));
        return "petugas/dashboard";
    }

    // ==========================================
    // GET /petugas/tasks — Daftar tugas aktif
    // ==========================================
    @GetMapping("/petugas/tasks")
    public String petugasTasks(Model model, HttpSession session) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        // FIX-9: Backend gate — petugas wajib check-in untuk akses daftar tugas
        boolean isCheckedIn = attendanceService.getCurrentShift(userId)
                .map(s -> s.getCheckInAt() != null
                       && s.getShiftStatus() != OfficerAttendance.ShiftStatus.SELESAI_SHIFT)
                .orElse(false);
        if (!isCheckedIn) {
            return "redirect:/petugas/dashboard";
        }

        List<Map<String, Object>> tasksNew = new ArrayList<>();
        List<Map<String, Object>> tasksInProgress = new ArrayList<>();
        List<Map<String, Object>> tasksPending = new ArrayList<>();

        List<FieldTask> realTasks = fieldTaskService.getTasksByOfficer(userId);
        BigDecimal userLat = null;
        BigDecimal userLng = null;
        Optional<OfficerAttendance> currentShift = attendanceService.getCurrentShift(userId);
        if (currentShift.isPresent()) {
            userLat = currentShift.get().getCheckInLatitude();
            userLng = currentShift.get().getCheckInLongitude();
        }

        for (FieldTask t : realTasks) {
            switch (t.getTaskStatus()) {
                case BARU -> tasksNew.add(toPetugasTaskMap(t, userLat, userLng));
                case DITUGASKAN_ULANG -> tasksNew.add(toPetugasTaskMap(t, userLat, userLng));
                case SEDANG_DIKERJAKAN -> tasksInProgress.add(toPetugasTaskMap(t, userLat, userLng));
                case TERTUNDA -> tasksPending.add(toPetugasTaskMap(t, userLat, userLng));
                default -> {}
            }
        }

        // FIX-8: FR-PTG-10 — Algoritma Sorting Cerdas (SLA + GPS Proximity)
        java.util.Comparator<Map<String, Object>> scoreComparator = (m1, m2) -> {
            long sla1 = ((Number) m1.getOrDefault("rawSlaRemaining", 999L)).longValue();
            double dist1 = ((Number) m1.getOrDefault("rawDistance", 999.0)).doubleValue();
            int prio1 = ((Number) m1.getOrDefault("rawPriorityScore", 2)).intValue();
            double score1 = (sla1 * 10) + (dist1 * 2) + (prio1 * 50);

            long sla2 = ((Number) m2.getOrDefault("rawSlaRemaining", 999L)).longValue();
            double dist2 = ((Number) m2.getOrDefault("rawDistance", 999.0)).doubleValue();
            int prio2 = ((Number) m2.getOrDefault("rawPriorityScore", 2)).intValue();
            double score2 = (sla2 * 10) + (dist2 * 2) + (prio2 * 50);

            return Double.compare(score1, score2);
        };

        tasksNew.sort(scoreComparator);
        tasksPending.sort(scoreComparator);

        model.addAttribute("tasksNew", tasksNew);
        model.addAttribute("tasksInProgress", tasksInProgress);
        model.addAttribute("tasksPending", tasksPending);
        return "petugas/tasks";
    }

    // ==========================================
    // GET /petugas/task-detail — Detail satu tugas
    // ==========================================
    @GetMapping("/petugas/task-detail")
    public String petugasTaskDetail(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false, defaultValue = "TGS-001") String id
    ) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        Optional<FieldTask> realTask = fieldTaskService.getTaskById(id);
        if (realTask.isPresent()) {
            FieldTask ft = realTask.get();
            Map<String, Object> task = toPetugasTaskMap(ft, null, null);
            task.put("reporterPhone", ft.getReport() != null && ft.getReport().getReporter() != null
                ? ft.getReport().getReporter().getPhoneNumber() : "-");

            // pendingReason & pendingSince dari postponement terbaru
            fieldTaskService.getLatestPostponement(id).ifPresent(lp -> {
                task.put("pendingReason", lp.getReason());
                task.put("pendingSince", lp.getRequestedAt() != null
                    ? lp.getRequestedAt().format(ControllerHelper.DATETIME_FMT) : "-");
                task.put("postponeStatus", lp.getApprovalStatus() != null ? lp.getApprovalStatus().name() : "MENUNGGU");
            });

            // Riwayat status tugas dari FieldTaskStatusRevision
            List<FieldTaskStatusRevision> revisions = fieldTaskService.getTaskRevisions(id);
            task.put("taskRevisions", revisions);

            // FIX-1: Gunakan koordinat LAPORAN (lokasi kerusakan) untuk navigasi
            Map<String, Object> locationMap = new HashMap<>();
            String addr = ft.getReport() != null ? (ft.getReport().getLocationHint() != null
                ? ft.getReport().getLocationHint() : "-") : "-";
            locationMap.put("address", addr);
            if (ft.getReport() != null && ft.getReport().getLatitude() != null) {
                locationMap.put("latitude", ft.getReport().getLatitude().toPlainString());
                locationMap.put("longitude", ft.getReport().getLongitude().toPlainString());
            } else {
                // Fallback ke koordinat officer jika laporan tidak punya koordinat
                locationMap.put("latitude", ft.getOfficerLatitude() != null
                    ? ft.getOfficerLatitude().toPlainString() : "-6.2000");
                locationMap.put("longitude", ft.getOfficerLongitude() != null
                    ? ft.getOfficerLongitude().toPlainString() : "106.8167");
            }
            task.put("location", locationMap);

            List<Map<String, Object>> statusHistory = new ArrayList<>();
            statusHistory.add(Map.of("status", "Tugas Dibuat", "time",
                ft.getCreatedAt() != null ? ft.getCreatedAt().format(ControllerHelper.DATETIME_FMT) : "-",
                "note", "Tugas diterima dari laporan warga"));
            if (ft.getStartedAt() != null) {
                statusHistory.add(Map.of("status", "Mulai Dikerjakan", "time",
                    ft.getStartedAt().format(ControllerHelper.DATETIME_FMT),
                    "note", "Petugas memulai pengerjaan"));
            }
            if (ft.getCompletedAt() != null) {
                statusHistory.add(Map.of("status", "Selesai", "time",
                    ft.getCompletedAt().format(ControllerHelper.DATETIME_FMT),
                    "note", "Tugas telah selesai dikerjakan"));
            }
            task.put("statusHistory", statusHistory);

            model.addAttribute("task", task);
            model.addAttribute("user", userService.findById(userId)
                .map(u -> Map.of("name", u.getFullName())).orElse(Map.of("name", "Petugas")));

            Map<String, Object> attendance = new HashMap<>();
            attendance.put("checkedIn", false);
            attendanceService.getCurrentShift(userId).ifPresent(shift -> {
                attendance.put("checkedIn", shift.getCheckInAt() != null
                    && shift.getShiftStatus() != OfficerAttendance.ShiftStatus.SELESAI_SHIFT);
            });
            model.addAttribute("attendance", attendance);

            return "petugas/task-detail";
        }

        return "redirect:/petugas/tasks";
    }

    // ==========================================
    // GET /petugas/task-execution — Halaman upload foto bukti
    // ==========================================
    @GetMapping("/petugas/task-execution")
    public String petugasTaskExecution(
            Model model,
            HttpSession session,
            @RequestParam(value = "id", required = false, defaultValue = "TGS-001") String id,
            @RequestParam(value = "step", required = false, defaultValue = "before") String step
    ) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        // Cek evidence: jika sudah ada SEBELUM, step=after
        List<TaskEvidence> existingBefore = new ArrayList<>();
        try {
            existingBefore = fieldTaskService.getEvidencesByTaskAndType(id, TaskEvidence.EvidenceType.SEBELUM);
        } catch (Exception e) { /* ignore */ }
        String effectiveStep = (!existingBefore.isEmpty() || "after".equals(step)) ? "after" : "before";

        Optional<FieldTask> realTask = fieldTaskService.getTaskById(id);
        if (realTask.isPresent()) {
            FieldTask ft = realTask.get();
            Map<String, Object> task = toPetugasTaskMap(ft, null, null);

            // FIX-1: Gunakan koordinat laporan untuk konsistensi navigasi
            Map<String, Object> locationMap = new HashMap<>();
            String addr = ft.getReport() != null ? (ft.getReport().getLocationHint() != null
                ? ft.getReport().getLocationHint() : "-") : "-";
            locationMap.put("address", addr);
            if (ft.getReport() != null && ft.getReport().getLatitude() != null) {
                locationMap.put("latitude", ft.getReport().getLatitude().toPlainString());
                locationMap.put("longitude", ft.getReport().getLongitude().toPlainString());
            } else {
                locationMap.put("latitude", ft.getOfficerLatitude() != null
                    ? ft.getOfficerLatitude().toPlainString() : "-6.2000");
                locationMap.put("longitude", ft.getOfficerLongitude() != null
                    ? ft.getOfficerLongitude().toPlainString() : "106.8167");
            }
            task.put("location", locationMap);

            // workDuration untuk task-execution
            if (ft.getStartedAt() != null) {
                LocalDateTime end = ft.getCompletedAt() != null ? ft.getCompletedAt() : LocalDateTime.now();
                task.put("workDuration", formatDuration(Duration.between(ft.getStartedAt(), end)));
            } else {
                task.put("workDuration", "00:00:00");
            }
            task.put("distanceToTask", "-");
            model.addAttribute("task", task);
            model.addAttribute("materials", new ArrayList<>());
            model.addAttribute("currentStep", effectiveStep);
            return "petugas/task-execution";
        }

        return "redirect:/petugas/tasks";
    }

    // ==========================================
    // POST /petugas/task-execution — Simpan foto bukti & selesaikan tugas
    // ==========================================
    @PostMapping("/petugas/task-execution")
    public String petugasTaskExecutionPost(
            @RequestParam(value = "id", required = false, defaultValue = "TGS-001") String id,
            @RequestParam(value = "action", required = false, defaultValue = "save") String action,
            @RequestParam(value = "photoBeforeData", required = false) String photoBeforeData,
            @RequestParam(value = "photoAfterData", required = false) String photoAfterData,
            HttpSession session
    ) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        try {
            if ("save".equals(action) && photoBeforeData != null && !photoBeforeData.isBlank()) {
                // FIX-6: saveTaskEvidence sudah include watermarking di service layer
                fieldTaskService.saveTaskEvidence(id, photoBeforeData, TaskEvidence.EvidenceType.SEBELUM);
                return "redirect:/petugas/task-execution?id=" + id + "&step=after";
            } else if ("complete".equals(action)) {
                try {
                    if (photoAfterData != null && !photoAfterData.isBlank()) {
                        // FIX-6: Watermark diterapkan di service layer
                        fieldTaskService.saveTaskEvidence(id, photoAfterData, TaskEvidence.EvidenceType.SESUDAH);
                    }
                } catch (Exception ev) {
                    log.error("Gagal simpan evidence {}, membatalkan completeTask: {}", id, ev.getMessage());
                    return "redirect:/petugas/task-execution?id=" + id + "&step=after&error=Gagal+menyimpan+foto";
                }
                fieldTaskService.completeTask(id);
                return "redirect:/petugas/dashboard";
            }
        } catch (Exception e) {
            log.error("Gagal proses task execution {}: {}", id, e.getMessage(), e);
        }
        return "redirect:/petugas/task-execution?id=" + id;
    }

    // ==========================================
    // GET /petugas/history — Riwayat tugas selesai
    // ==========================================
    @GetMapping("/petugas/history")
    public String petugasHistory(Model model, HttpSession session) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        List<FieldTask> allTasks = fieldTaskService.getTasksByOfficer(userId);
        List<FieldTask> completed = allTasks.stream()
            .filter(t -> t.getTaskStatus() == TaskStatus.SELESAI).collect(Collectors.toList());

        long totalH = completed.stream()
            .filter(t -> t.getStartedAt() != null && t.getCompletedAt() != null)
            .mapToLong(t -> Duration.between(t.getStartedAt(), t.getCompletedAt()).toHours())
            .sum();
        long avgMins = completed.isEmpty() ? 0 :
            completed.stream()
                .filter(t -> t.getStartedAt() != null && t.getCompletedAt() != null)
                .mapToLong(t -> Duration.between(t.getStartedAt(), t.getCompletedAt()).toMinutes())
                .sum() / completed.size();

        model.addAttribute("stats", Map.of(
            "totalTasks", allTasks.size(),
            "avgDuration", (avgMins / 60) + "j " + (avgMins % 60) + "m",
            "totalHours", totalH
        ));

        List<Map<String, Object>> taskList = completed.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getTaskId());
            m.put("title", t.getReport() != null ? "Tugas #" + t.getTaskId().substring(0, 8) : "-");
            m.put("category", t.getReport() != null && t.getReport().getCategory() != null
                ? t.getReport().getCategory().getCategoryName() : "Lainnya");
            m.put("location", t.getReport() != null && t.getReport().getLocationHint() != null
                ? t.getReport().getLocationHint() : "-");
            m.put("status", "approved");
            m.put("duration", t.getStartedAt() != null && t.getCompletedAt() != null
                ? formatDuration(Duration.between(t.getStartedAt(), t.getCompletedAt())) : "-");
            m.put("completedAt", t.getCompletedAt() != null
                ? t.getCompletedAt().format(ControllerHelper.DATE_FMT) : "-");
            m.put("rawCompletedAt", t.getCompletedAt() != null
                ? t.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000 : 0L);
            // FIX: Count evidence dari DB
            List<TaskEvidence> beforeEvs = fieldTaskService.getEvidencesByTaskAndType(
                t.getTaskId(), TaskEvidence.EvidenceType.SEBELUM);
            List<TaskEvidence> afterEvs  = fieldTaskService.getEvidencesByTaskAndType(
                t.getTaskId(), TaskEvidence.EvidenceType.SESUDAH);
            m.put("photoBefore", beforeEvs.size());
            m.put("photoAfter",  afterEvs.size());
            m.put("materialUsed", 0);
            return m;
        }).collect(Collectors.toList());

        model.addAttribute("tasks", taskList);
        return "petugas/history";
    }

    // ==========================================
    // GET /petugas/history-detail — Detail riwayat tugas
    // ==========================================
    @GetMapping("/petugas/history-detail")
    public String petugasHistoryDetail(Model model, HttpSession session, @RequestParam("id") String id) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        Optional<FieldTask> taskOpt = fieldTaskService.getTaskById(id);
        if (taskOpt.isPresent() && taskOpt.get().getOfficer() != null
            && taskOpt.get().getOfficer().getUserId().equals(userId)) {

            FieldTask t = taskOpt.get();
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getTaskId());
            m.put("title", t.getReport() != null ? t.getReport().getDescription() : "Tugas #" + t.getTaskId().substring(0, 8));
            m.put("category", t.getReport() != null && t.getReport().getCategory() != null
                ? t.getReport().getCategory().getCategoryName() : "Lainnya");
            m.put("location", t.getReport() != null && t.getReport().getLocationHint() != null
                ? t.getReport().getLocationHint() : "-");
            m.put("status", "Selesai");
            m.put("duration", t.getStartedAt() != null && t.getCompletedAt() != null
                ? formatDuration(Duration.between(t.getStartedAt(), t.getCompletedAt())) : "-");
            m.put("startedAt", t.getStartedAt() != null ? t.getStartedAt().format(ControllerHelper.DATETIME_FMT) : "-");
            m.put("completedAt", t.getCompletedAt() != null ? t.getCompletedAt().format(ControllerHelper.DATETIME_FMT) : "-");

            m.put("description", t.getReport() != null && t.getReport().getDescription() != null
                ? t.getReport().getDescription() : "-");
            m.put("reporterName", t.getReport() != null && t.getReport().getReporter() != null
                ? t.getReport().getReporter().getFullName() : "-");
            m.put("reportDate", t.getReport() != null && t.getReport().getSubmittedAt() != null
                ? t.getReport().getSubmittedAt().format(ControllerHelper.DATETIME_FMT) : "-");

            if (t.getReport() != null) {
                m.put("latitude", t.getReport().getLatitude());
                m.put("longitude", t.getReport().getLongitude());
            }

            List<TaskEvidence> beforeEvs = fieldTaskService.getEvidencesByTaskAndType(t.getTaskId(), TaskEvidence.EvidenceType.SEBELUM);
            List<TaskEvidence> afterEvs  = fieldTaskService.getEvidencesByTaskAndType(t.getTaskId(), TaskEvidence.EvidenceType.SESUDAH);

            m.put("photoBeforeList", beforeEvs);
            m.put("photoAfterList", afterEvs);

            model.addAttribute("task", m);
            return "petugas/history-detail";
        }
        return "redirect:/petugas/history";
    }

    // ==========================================
    // GET /petugas/reports — Statistik performa petugas
    // ==========================================
    @GetMapping("/petugas/reports")
    public String petugasReports(
            Model model,
            HttpSession session,
            @RequestParam(value = "period", required = false, defaultValue = "week") String period
    ) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        Map<String, Object> user = new HashMap<>();
        user.put("name", "Petugas");
        userService.findById(userId).ifPresent(u -> user.put("name", u.getFullName()));
        model.addAttribute("user", user);
        model.addAttribute("selectedPeriod", period);

        List<FieldTask> allTasks = fieldTaskService.getTasksByOfficer(userId);
        List<FieldTask> completed = allTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.SELESAI).collect(Collectors.toList());

        int total = allTasks.size();
        int completedCount = completed.size();
        int pendingCount = allTasks.size() - completedCount;
        long totalMinutes = completed.stream()
            .filter(t -> t.getStartedAt() != null && t.getCompletedAt() != null)
            .mapToLong(t -> Duration.between(t.getStartedAt(), t.getCompletedAt()).toMinutes())
            .sum();
        int hours = total > 0 ? (int) (totalMinutes / 60) : 0;
        long avgMins = completedCount > 0 ? totalMinutes / completedCount : 0;
        String avgDur = avgMins > 0 ? (avgMins / 60) + "j " + (avgMins % 60) + "m" : "-";
        String rate = total > 0 ? (int) Math.round((double) completedCount / total * 100) + "%" : "0%";

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTasks",     total);
        stats.put("completedTasks", completedCount);
        stats.put("pendingTasks",   pendingCount);
        stats.put("totalHours",     hours);
        stats.put("avgDuration",    avgDur);
        stats.put("completionRate", rate);

        Map<String, Long> catCount = new java.util.TreeMap<>();
        for (FieldTask t : allTasks) {
            String cat = t.getReport() != null && t.getReport().getCategory() != null
                ? t.getReport().getCategory().getCategoryName() : "Lainnya";
            catCount.merge(cat, 1L, Long::sum);
        }
        List<Map<String, Object>> categories = new ArrayList<>();
        String[] catColors = {"bg-blue-500", "bg-yellow-500", "bg-green-500", "bg-purple-500", "bg-red-500", "bg-gray-400"};
        int ci = 0;
        for (Map.Entry<String, Long> e : catCount.entrySet()) {
            int pct = total > 0 ? (int) Math.round(e.getValue() * 100.0 / total) : 0;
            categories.add(Map.of("name", e.getKey(), "count", e.getValue().intValue(),
                "percentage", pct, "colorClass", catColors[ci++ % catColors.length]));
        }
        stats.put("categories", categories);

        List<Map<String, Object>> progress = new ArrayList<>();
        String progressTitle;
        LocalDate now = LocalDate.now();
        if (period.equals("week")) {
            progressTitle = "Tugas per Hari (7 Hari Terakhir)";
            String[] dayNames = {"Min","Sen","Sel","Rab","Kam","Jum","Sab"};
            for (int i = 6; i >= 0; i--) {
                LocalDate d = now.minusDays(i);
                String dayLabel = dayNames[d.getDayOfWeek().getValue() % 7];
                int dayTasks = (int) completed.stream()
                    .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().toLocalDate().equals(d))
                    .count();
                progress.add(Map.of("label", dayLabel, "completed", dayTasks, "percent", Math.min(dayTasks * 25, 100)));
            }
        } else if (period.equals("month")) {
            progressTitle = "Tugas per Minggu (30 Hari Terakhir)";
            for (int w = 4; w >= 1; w--) {
                LocalDate end = now.minusDays((w - 1) * 7L);
                LocalDate start = end.minusDays(6);
                int weekTasks = (int) completed.stream()
                    .filter(t -> t.getCompletedAt() != null
                        && !t.getCompletedAt().toLocalDate().isBefore(start)
                        && !t.getCompletedAt().toLocalDate().isAfter(end))
                    .count();
                progress.add(Map.of("label", "Mg " + (5 - w), "completed", weekTasks, "percent", Math.min(weekTasks * 25, 100)));
            }
        } else {
            progressTitle = "Tugas per Bulan (1 Tahun)";
            String[] monthLabels = {"Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agt","Sep","Okt","Nov","Des"};
            for (int m = 0; m < 12; m++) {
                int monthValue = m + 1;
                int monthTasks = (int) completed.stream()
                    .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().getMonthValue() == monthValue)
                    .count();
                progress.add(Map.of("label", monthLabels[m], "completed", monthTasks, "percent", Math.min(monthTasks * 25, 100)));
            }
        }
        stats.put("progress", progress);
        stats.put("progressTitle", progressTitle);

        model.addAttribute("stats", stats);
        model.addAttribute("materialStats", new ArrayList<>());
        return "petugas/reports";
    }

    // ==========================================
    // GET /petugas/attendance-history — Riwayat absensi
    // ==========================================
    @GetMapping("/petugas/attendance-history")
    public String petugasAttendanceHistory(Model model, HttpSession session) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";
        {
            List<OfficerAttendance> realRecords = attendanceService.getAttendanceByOfficer(userId);
            if (!realRecords.isEmpty()) {
                long daysPresent = realRecords.size();
                long totalMinutes = realRecords.stream()
                    .filter(r -> r.getCheckInAt() != null && r.getCheckOutAt() != null)
                    .mapToLong(r -> Duration.between(r.getCheckInAt(), r.getCheckOutAt()).toMinutes())
                    .sum();
                long lateCount = 0;
                List<Map<String, Object>> recordList = realRecords.stream().map(r -> {
                    Map<String, Object> di = new HashMap<>();
                    String userAgent = r.getDeviceInfo() != null ? r.getDeviceInfo() : "Unknown";
                    String ip = "-";
                    if (userAgent.contains("| IP: ")) {
                        String[] parts = userAgent.split("\\| IP: ");
                        userAgent = parts[0].trim();
                        if (parts.length > 1) ip = parts[1].trim();
                    }
                    di.put("browser", userAgent);
                    
                    String os = "Unknown";
                    if (userAgent.contains("Windows")) os = "Windows";
                    else if (userAgent.contains("Mac")) os = "MacOS";
                    else if (userAgent.contains("Linux")) os = "Linux";
                    else if (userAgent.contains("Android")) os = "Android";
                    else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) os = "iOS";
                    
                    di.put("os", os);
                    di.put("ip", ip);
                    String dateFmt = r.getCheckInAt() != null ? r.getCheckInAt().format(ControllerHelper.DATE_FMT) : "-";
                    String ciTime = r.getCheckInAt() != null ? r.getCheckInAt().format(ControllerHelper.TIME_FMT) : "-";
                    String coTime = r.getCheckOutAt() != null ? r.getCheckOutAt().format(ControllerHelper.TIME_FMT) : null;
                    String dur = r.getCheckInAt() != null && r.getCheckOutAt() != null
                        ? formatDuration(Duration.between(r.getCheckInAt(), r.getCheckOutAt())) : "-";
                    String status = r.getShiftStatus() == OfficerAttendance.ShiftStatus.SELESAI_SHIFT ? "completed" : "ongoing";
                    return buildAttendanceRecord(dateFmt, ciTime, coTime, dur, status,
                        Map.of("address", r.getCheckInLatitude() != null
                            ? r.getCheckInLatitude().toPlainString() + ", " + r.getCheckInLongitude().toPlainString() : "Kantor Dinas"),
                        r.getCheckOutAt() != null ? Map.of("address", "Check-out lokasi") : null,
                        di);
                }).collect(Collectors.toList());
                model.addAttribute("user", Map.of("name", "Petugas"));
                model.addAttribute("summary", Map.of("daysPresent", (int) daysPresent, "totalHours", totalMinutes / 60, "lateCount", (int) lateCount));
                model.addAttribute("attendanceRecords", recordList);
                return "petugas/attendance-history";
            }
        }

        // Jika tidak ada riwayat di DB — tampilkan list kosong
        model.addAttribute("user", Map.of("name", "-"));
        model.addAttribute("summary", Map.of("daysPresent", 0, "totalHours", 0L, "lateCount", 0));
        model.addAttribute("attendanceRecords", new ArrayList<>());
        return "petugas/attendance-history";
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long mins = d.toMinutes() % 60;
        long secs = d.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    /**
     * Konversi FieldTask ke Map untuk Thymeleaf template.
     * FIX-7: pendingReason diisi dari postponement terbaru agar tampil di task list.
     */
    private Map<String, Object> toPetugasTaskMap(FieldTask task, BigDecimal userLat, BigDecimal userLng) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", task.getTaskId());
        m.put("title", task.getReport() != null ? task.getReport().getDescription()
            : "Tugas #" + task.getTaskId().substring(0, 8));
        m.put("category", task.getReport() != null && task.getReport().getCategory() != null
            ? task.getReport().getCategory().getCategoryName() : "Lainnya");
        String status = switch (task.getTaskStatus()) {
            case BARU -> "new";
            case SEDANG_DIKERJAKAN -> "in_progress";
            case TERTUNDA -> "pending";
            case SELESAI -> "completed";
            case DITUGASKAN_ULANG -> "new";
        };
        m.put("status", status);
        m.put("priority", "medium");
        m.put("location", task.getReport() != null ? task.getReport().getLocationHint() : "-");
        m.put("description", task.getReport() != null ? task.getReport().getDescription() : "-");
        m.put("reporterName", task.getReport() != null && task.getReport().getReporter() != null
            ? task.getReport().getReporter().getFullName() : "-");
        m.put("reporterPhone", task.getReport() != null && task.getReport().getReporter() != null
            && task.getReport().getReporter().getPhoneNumber() != null
            ? task.getReport().getReporter().getPhoneNumber() : "-");
        m.put("reportDate", task.getReport() != null && task.getReport().getSubmittedAt() != null
            ? task.getReport().getSubmittedAt().format(ControllerHelper.DATE_FMT) : "-");
        m.put("rawReportDate", task.getReport() != null && task.getReport().getSubmittedAt() != null
            ? task.getReport().getSubmittedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : 0L);
        // FR-PTG-17: flag koreksi koordinat agar UI modal bisa tampilkan status 1x
        m.put("coordinateCorrected", task.getReport() != null && task.getReport().isCoordinateCorrected());
        // Laporan Warga Evidence
        m.put("photoBase64", task.getReport() != null ? task.getReport().getPhotoBase64() : null);

        // Calculate distance
        if (userLat != null && userLng != null && task.getReport() != null &&
            task.getReport().getLatitude() != null && task.getReport().getLongitude() != null) {
            double distKm = com.plr.aduaja.util.GeoUtils.haversineKm(
                userLat, userLng,
                task.getReport().getLatitude(), task.getReport().getLongitude());
            m.put("distanceToTask", String.format("%.2f km", distKm));
            m.put("rawDistance", distKm);
        } else {
            m.put("rawDistance", 999.0);
        }

        // SLA data dari SlaRecord
        m.put("rawSlaRemaining", 999L);
        if (task.getSlaRecord() != null) {
            SlaRecord sla = task.getSlaRecord();
            m.put("slaDeadline", sla.getSlaDeadlineAt() != null
                ? sla.getSlaDeadlineAt().format(ControllerHelper.DATETIME_FMT) : "-");
            boolean isOverdue = sla.getSlaDeadlineAt() != null
                && sla.getCurrentStatus() != SlaRecord.SlaStatus.SELESAI
                && LocalDateTime.now().isAfter(sla.getSlaDeadlineAt());

            long remainingHours = sla.getSlaDeadlineAt() != null
                ? Duration.between(LocalDateTime.now(), sla.getSlaDeadlineAt()).toHours() : 999L;
            m.put("rawSlaRemaining", remainingHours);

            if (sla.getCurrentStatus() == SlaRecord.SlaStatus.SELESAI) {
                m.put("slaStatusText", "Selesai"); m.put("slaStatusClass", "text-green-600");
            } else if (sla.getCurrentStatus() == SlaRecord.SlaStatus.TERLAMBAT || isOverdue) {
                m.put("slaStatusText", "Terlambat"); m.put("slaStatusClass", "text-red-600 font-bold");
                m.put("rawSlaRemaining", -999L);
            } else if (sla.getCurrentStatus() == SlaRecord.SlaStatus.TERTUNDA) {
                m.put("slaStatusText", "Tertunda"); m.put("slaStatusClass", "text-yellow-600");
            } else {
                m.put("slaStatusText", remainingHours + " jam tersisa");
                m.put("slaStatusClass", remainingHours < 10 ? "text-orange-600 font-bold" : "text-blue-600");
            }
        } else {
            m.put("slaDeadline", "-"); m.put("slaStatusText", "-"); m.put("slaStatusClass", "text-gray-600");
            m.put("rawSlaRemaining", 999L);
        }

        m.put("rawPriorityScore", 2); // Default Medium
        if (m.get("priority") != null) {
            String p = m.get("priority").toString();
            if (p.equals("critical")) m.put("rawPriorityScore", 0);
            else if (p.equals("high")) m.put("rawPriorityScore", 1);
            else if (p.equals("low")) m.put("rawPriorityScore", 3);
        }
        if (!m.containsKey("distanceToTask")) {
            m.put("distanceToTask", "-");
        }
        if (task.getStartedAt() != null) m.put("startedAt", task.getStartedAt().format(ControllerHelper.DATETIME_FMT));
        m.put("officerLatitude", task.getOfficerLatitude());
        m.put("officerLongitude", task.getOfficerLongitude());

        // FIX-7: pendingReason diisi dari postponement terbaru agar tampil di daftar tugas tertunda
        if (task.getTaskStatus() == TaskStatus.TERTUNDA) {
            fieldTaskService.getLatestPostponement(task.getTaskId()).ifPresent(lp -> {
                m.put("pendingReason", lp.getReason());
                m.put("pendingSince", lp.getRequestedAt() != null
                    ? lp.getRequestedAt().format(ControllerHelper.DATETIME_FMT) : "-");
            });
        }
        return m;
    }

    // ==========================================
    // POST /petugas/coordinate-correction — FR-PTG-17
    // ==========================================
    @PostMapping("/petugas/coordinate-correction")
    public String petugasCoordinateCorrection(
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestParam("taskId") String taskId,
            @RequestParam("correctedLat") String correctedLatStr,
            @RequestParam("correctedLng") String correctedLngStr
    ) {
        if (session.getAttribute("forceChangePasswordUserId") != null) return "redirect:/petugas/change-password";
        String userId = ControllerHelper.requireRole(session, "PETUGAS");
        if (userId == null) return "redirect:/petugas/login";

        try {
            FieldTask task = fieldTaskService.getTasksByOfficer(userId).stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst().orElse(null);

            if (task == null || task.getReport() == null) {
                redirectAttributes.addFlashAttribute("taskError", "Tugas tidak ditemukan atau tidak sah.");
                return "redirect:/petugas/task-detail?id=" + taskId;
            }

            Report report = task.getReport();

            // Validasi: hanya boleh koreksi 1 kali
            if (report.isCoordinateCorrected()) {
                redirectAttributes.addFlashAttribute("taskError",
                    "Koreksi koordinat hanya dapat dilakukan 1 kali per laporan.");
                return "redirect:/petugas/task-detail?id=" + taskId;
            }

            // Validasi format koordinat
            java.math.BigDecimal newLat;
            java.math.BigDecimal newLng;
            try {
                newLat = new java.math.BigDecimal(correctedLatStr.trim());
                newLng = new java.math.BigDecimal(correctedLngStr.trim());
                // Validasi rentang koordinat (lat: -90~90, lng: -180~180)
                if (newLat.compareTo(java.math.BigDecimal.valueOf(-90)) < 0
                        || newLat.compareTo(java.math.BigDecimal.valueOf(90)) > 0
                        || newLng.compareTo(java.math.BigDecimal.valueOf(-180)) < 0
                        || newLng.compareTo(java.math.BigDecimal.valueOf(180)) > 0) {
                    throw new NumberFormatException("Koordinat di luar rentang valid");
                }
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("taskError",
                    "Format koordinat tidak valid. Pastikan nilai latitude dan longitude benar.");
                return "redirect:/petugas/task-detail?id=" + taskId;
            }

            // Simpan nilai lama untuk audit log
            String oldCoords = report.getLatitude() + ", " + report.getLongitude();
            String newCoords = newLat + ", " + newLng;

            // Update koordinat dan set flag
            report.setLatitude(newLat);
            report.setLongitude(newLng);
            report.setCoordinateCorrected(true);
            reportRepository.save(report);

            // Tulis ke AuditLog menggunakan factory method (setter AuditLog bersifat package-private)
            userService.findById(userId).ifPresent(officer -> {
                AuditLog auditLog = AuditLog.create(
                    officer, report,
                    "REPORT", report.getReportId(),
                    "COORDINATE_CORRECTION",
                    oldCoords, newCoords
                );
                auditLogRepository.save(auditLog);
            });

            log.info("[FR-PTG-17] Petugas {} koreksi koordinat laporan {} dari {} ke {}",
                userId, report.getReportId(), oldCoords, newCoords);

            redirectAttributes.addFlashAttribute("successMsg",
                "Koreksi koordinat berhasil disimpan. Lokasi laporan telah diperbarui.");

        } catch (Exception e) {
            log.error("Gagal koreksi koordinat tugas {}: {}", taskId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("taskError", "Terjadi kesalahan saat menyimpan koreksi koordinat.");
        }

        return "redirect:/petugas/task-detail?id=" + taskId;
    }

    private Map<String, Object> buildAttendanceRecord(
            String dateFormatted, String checkInTime, String checkOutTime,
            String duration, String status,
            Map<String, Object> checkInLoc, Map<String, Object> checkOutLoc,
            Map<String, Object> deviceInfo
    ) {
        Map<String, Object> r = new HashMap<>();
        r.put("dateFormatted",    dateFormatted);
        r.put("checkInTime",      checkInTime);
        r.put("checkOutTime",     checkOutTime);
        r.put("duration",         duration);
        r.put("status",           status);
        r.put("checkInLocation",  checkInLoc);
        r.put("checkOutLocation", checkOutLoc);
        r.put("deviceInfo",       deviceInfo);
        return r;
    }
}
