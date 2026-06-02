package com.plr.aduaja.service;

import com.plr.aduaja.model.User;
import com.plr.aduaja.model.LoginAttempt;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.repository.LoginAttemptRepository;
import com.plr.aduaja.dto.LoginDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

// ============================================================
// POLYMORPHISM (Run-time Polymorphism): AuthServiceImpl
// Mengimplementasikan AuthService interface → @Override setiap method
//
// ABSTRACTION: Controller tidak perlu tahu detail:
// - Bagaimana password diverifikasi
// - Bagaimana login attempt dicatat
// - Mekanisme lockout setelah 5 kali gagal
// ============================================================
@Service
@Transactional
public class AuthServiceImpl implements AuthService {  // ← POLYMORPHISM

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<User> login(LoginDTO dto, String ipAddress) {
        // Delegasi ke loginByEmail (ABSTRACTION: satu entry point)
        return loginByEmail(dto.getEmail(), dto.getPassword(), ipAddress);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public Optional<User> loginByEmail(String email, String password, String ipAddress) {
        // Cek apakah akun terkunci (MAX_ATTEMPTS kali gagal)
        if (isAccountLocked(email)) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Catat percobaan gagal meski user tidak ada
            recordLoginAttempt(email, false, ipAddress);
            return Optional.empty();
        }

        User user = userOpt.get();

        // Verifikasi password (ENKAPSULASI: logic di dalam service)
        if (!verifyPassword(password, user.getPasswordHash())) {
            recordLoginAttempt(email, false, ipAddress);
            return Optional.empty();
        }

        // Cek status akun
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            // Petugas baru berstatus PENDING diizinkan lewat agar bisa diarahkan ke halaman ganti password oleh Controller.
            // Selain Petugas (seperti Warga), PENDING berarti belum verifikasi OTP, sehingga tidak boleh login.
            if (user.getRole() != User.Role.PETUGAS) {
                return Optional.empty();
            }
        }
        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            return Optional.empty();  // Akun ditangguhkan
        }

        // Login berhasil
        recordLoginAttempt(email, true, ipAddress);
        return Optional.of(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public Optional<User> loginByPhone(String phone, String password, String ipAddress) {
        if (isAccountLocked(phone)) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByPhoneNumber(phone);
        if (userOpt.isEmpty()) {
            recordLoginAttempt(phone, false, ipAddress);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!verifyPassword(password, user.getPasswordHash())) {
            recordLoginAttempt(phone, false, ipAddress);
            return Optional.empty();
        }

        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            return Optional.empty();
        }

        recordLoginAttempt(phone, true, ipAddress);
        return Optional.of(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        // ENKAPSULASI: detail BCrypt tersembunyi dari caller
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean isAccountLocked(String email) {
        // Hitung percobaan gagal dalam LOCKOUT_MINUTES terakhir
        LocalDateTime lockoutStart = LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES);
        long failedAttempts = loginAttemptRepository
            .countByEmailAndIsSuccessAndAttemptedAtAfter(email, false, lockoutStart);
        return failedAttempts >= MAX_ATTEMPTS;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public void recordLoginAttempt(String email, boolean success, String ipAddress) {
        // ENKAPSULASI: detail penyimpanan tersembunyi dari Controller
        LoginAttempt attempt = new LoginAttempt();
        attempt.setEmail(email);
        attempt.setIsSuccess(success);
        attempt.setIpAddress(ipAddress);
        attempt.setAttemptedAt(LocalDateTime.now());
        loginAttemptRepository.save(attempt);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public void logout(String userId) {
        // Session management dilakukan oleh Controller via HttpSession.invalidate()
        // Di sini bisa tambah logika invalidate ActiveSession jika diperlukan
    }
}
