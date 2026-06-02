package com.plr.aduaja.controller;

import com.plr.aduaja.model.User;
import com.plr.aduaja.dto.LoginDTO;
import com.plr.aduaja.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

// ============================================================
// ABSTRACTION (Abstraksi): AdminAuthController hanya tahu Interface Service
// Controller TIDAK mengakses Repository atau implementasi langsung
// ============================================================
@Slf4j
@Controller
public class AdminAuthController {

    // ABSTRACTION: hanya inject Interface
    @Autowired
    private AuthService authService;  // ← Abstraction: hanya tahu interface

    // ==========================================
    // GET /admin/login — Halaman login admin
    // ==========================================
    @GetMapping("/admin/login")
    public String loginPage(Model model,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("loginDTO", new LoginDTO());
        if (logout != null) {
            model.addAttribute("info", "Anda berhasil logout.");
        }
        return "admin/login";
    }

    // ==========================================
    // POST /admin/login — Proses login admin
    // ==========================================
    @PostMapping("/admin/login")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        HttpSession session,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {

        // ENKAPSULASI: buat LoginDTO, bukan kirim data mentah
        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setPassword(password);

        String ipAddress = getClientIp(request);

        // ABSTRACTION: Controller tidak tahu detail proses autentikasi
        Optional<User> userOpt = authService.login(dto, ipAddress);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email atau password salah.");
            return "redirect:/admin/login";
        }

        User user = userOpt.get();

        // Validasi role — hanya admin yang bisa login di sini
        if (user.getRole() != User.Role.ADMIN_PUSAT && user.getRole() != User.Role.ADMIN_DINAS) {
            redirectAttributes.addFlashAttribute("error", "Akun ini bukan akun admin.");
            return "redirect:/admin/login";
        }

        // Simpan session
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userRole", user.getRole().toString());

        if (user.getRole() == User.Role.ADMIN_DINAS && user.getAgency() != null) {
            session.setAttribute(ControllerHelper.SESSION_AGENCY_ID, user.getAgency().getAgencyId());
            session.setAttribute(ControllerHelper.SESSION_AGENCY_NAME, user.getAgency().getAgencyName());
            if (user.getAgency().getRegion() != null) {
                session.setAttribute(ControllerHelper.SESSION_REGION_ID, user.getAgency().getRegion().getRegionId());
                session.setAttribute(ControllerHelper.SESSION_REGION_NAME, user.getAgency().getRegion().getRegionName());
            }
        }

        if (user.getRole() == User.Role.ADMIN_PUSAT && user.getRegion() != null) {
            session.setAttribute(ControllerHelper.SESSION_REGION_ID, user.getRegion().getRegionId());
            session.setAttribute(ControllerHelper.SESSION_REGION_NAME, user.getRegion().getRegionName());
        }

        // Redirect ke dashboard yang sesuai role
        if (user.getRole() == User.Role.ADMIN_DINAS) {
            return "redirect:/admin/dinas/dashboard";
        }
        return "redirect:/admin/dashboard";
    }

    // ==========================================
    // POST /admin/logout — Logout admin
    // ==========================================
    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login?logout=true";
    }

    // ==========================================
    // GET /petugas/login — Halaman login petugas
    // ==========================================
    @GetMapping("/petugas/login")
    public String petugasLoginPage(Model model) {
        model.addAttribute("loginDTO", new LoginDTO());
        return "petugas/login";
    }

    // ==========================================
    // POST /petugas/login — Proses login petugas
    // ==========================================
    @PostMapping("/petugas/login")
    public String petugasLogin(@RequestParam("email") String email,
                               @RequestParam("password") String password,
                               HttpSession session,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {

        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setPassword(password);

        String ipAddress = getClientIp(request);
        Optional<User> userOpt = authService.login(dto, ipAddress);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email atau password salah.");
            return "redirect:/petugas/login";
        }

        User user = userOpt.get();

        // Validasi role — hanya petugas yang bisa login di sini
        if (user.getRole() != User.Role.PETUGAS) {
            redirectAttributes.addFlashAttribute("error", "Akun ini bukan akun petugas.");
            return "redirect:/petugas/login";
        }

        // Jika status PENDING (login pertama), paksa ganti password
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            session.setAttribute("forceChangePasswordUserId", user.getUserId());
            return "redirect:/petugas/change-password";
        }

        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userRole", "PETUGAS");

        return "redirect:/petugas/dashboard";
    }

    // ==========================================
    // POST /petugas/logout — Logout petugas
    // ==========================================
    @PostMapping("/petugas/logout")
    public String petugasLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/petugas/login";
    }

    // ENKAPSULASI: method private — tidak bisa diakses dari luar
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
