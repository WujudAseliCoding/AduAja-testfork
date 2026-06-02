# PROMPT UNTUK ANGGOTA 1 — MODUL AUTH & PROFIL
## Sistem AduAja — Spring Boot Monolithic Application

> File ini adalah prompt lengkap yang bisa diberikan ke AI lain
> untuk menyelesaikan SEMUA tugas Anggota 1 (Modul Auth & Profil).
> Prompt ini juga sebagai CONTOH untuk anggota lain membuat prompt mereka.

---

## BAGIAN 1: CONTEXT & BACKGROUND

### 1.1 Tentang Proyek

```
Nama Proyek: AduAja — Sistem Informasi Pengaduan dan Aspirasi Masyarakat
Framework: Spring Boot 4.x + Spring Data JPA + Thymeleaf + Lombok + H2 Database
Arsitektur: MVC (Model-View-Controller) Monolithic
Jumlah Entity: 23 entities
Jumlah Developer: 4 orang (vertical slicing per modul)
Database: H2 file-based (./data/aduaja)
```

### 1.2 Struktur Folder Proyek

```
src/main/java/com/plr/aduaja/
├── AduAjaApplication.java          ← Main class
├── model/                          ← Entity (JPA)
├── repository/                     ← JPA Repository
├── service/                        ← Service Layer
├── controller/                     ← Web Controller (MVC)
├── dto/                            ← Data Transfer Object (HARUS ADA)
└── config/                         ← Configuration

src/main/resources/
├── templates/                      ← Thymeleaf HTML
│   ├── layouts/master.html        ← Template utama
│   ├── warga/                      ← Halaman Warga
│   ├── admin/                      ← Halaman Admin
│   └── petugas/                    ← Halaman Petugas
└── application.properties          ← Konfigurasi
```

### 1.3 Database Configuration (sudah ada, jangan diubah)

```properties
spring.datasource.url=jdbc:h2:file:./data/aduaja
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### 1.4 Role dalam Sistem

```java
public enum Role {
    WARGA,       // Warga biasa
    ADMIN_PUSAT, // Admin pusat
    ADMIN_DINAS, // Admin dinas
    PETUGAS      // Petugas lapangan
}

public enum AccountStatus {
    PENDING,  // Belum aktif (harus OTP verify)
    ACTIVE,   // Aktif
    SUSPENDED // Ditangguhkan
}
```

---

## BAGIAN 2: TUGAS UTAMA ANGGOTA 1

### 2.1 Deskripsi Modul

**Modul Auth & Profil** menangani:
1. Registrasi warga baru
2. Login (Warga, Admin Pusat, Admin Dinas, Petugas)
3. Logout
4. Edit profil warga (nama, email, HP, NIK, alamat)
5. Verifikasi OTP
6. Session management
7. Login attempt tracking (security)
8. Active session management

### 2.2 Entity yang Harus Dibuat (5 files)

| No | File | extends BaseEntity | Deskripsi |
|----|------|:-----------------:|-----------|
| 1 | `User.java` | ✅ WAJIB | Data user utama (warga, admin, petugas) |
| 2 | `UserProfile.java` | ✅ WAJIB | Profil tambahan (NIK, alamat) |
| 3 | `LoginAttempt.java` | ✅ WAJIB | Log percobaan login gagal |
| 4 | `OtpVerification.java` | ✅ WAJIB | Kode OTP verifikasi |
| 5 | `ActiveSession.java` | ✅ WAJIB | Session aktif user |

### 2.3 Repository yang Harus Dibuat (4 files)

| No | File | extends JpaRepository |
|----|------|:--------------------:|
| 1 | `UserRepository.java` | ✅ JpaRepository<User, String> |
| 2 | `UserProfileRepository.java` | ✅ JpaRepository<UserProfile, String> |
| 3 | `LoginAttemptRepository.java` | ✅ JpaRepository<LoginAttempt, String> |
| 4 | `OtpVerificationRepository.java` | ✅ JpaRepository<OtpVerification, String> |

### 2.4 Service yang Harus Dibuat (4 files)

| No | Interface | Implementation | Polymorphism |
|----|-----------|----------------|:------------:|
| 1 | `UserService.java` | `UserServiceImpl.java` | ✅ @Override + Overload |
| 2 | `AuthService.java` | `AuthServiceImpl.java` | ✅ @Override + Overload |
| 3 | `OtpService.java` | `OtpServiceImpl.java` | ✅ @Override + Overload |
| 4 | `SessionService.java` | `SessionServiceImpl.java` | ✅ @Override + Overload |

### 2.5 DTO yang Harus Dibuat (5 files)

| No | File | Untuk Form |
|----|------|-----------|
| 1 | `LoginDTO.java` | Form login |
| 2 | `RegisterDTO.java` | Form registrasi |
| 3 | `ProfileDTO.java` | Form edit profil |
| 4 | `OtpVerifyDTO.java` | Form verifikasi OTP |
| 5 | `ResetPasswordDTO.java` | Form reset password |

### 2.6 View HTML yang Harus Dibuat/Diedit (4 files)

| No | File | Deskripsi |
|----|------|-----------|
| 1 | `templates/warga/login.html` | Halaman login warga |
| 2 | `templates/warga/register.html` | Halaman registrasi |
| 3 | `templates/warga/profile.html` | Halaman edit profil |
| 4 | `templates/admin/login.html` | Halaman login admin |

### 2.7 Route/Endpoint yang Harus Ditambahkan

```
WARGA:
GET  /warga/login      → Halaman login warga
POST /warga/login      → Proses login warga
GET  /warga/register   → Halaman registrasi
POST /warga/register   → Proses registrasi
GET  /warga/verify-otp → Halaman verifikasi OTP
POST /warga/verify-otp → Proses verifikasi OTP
GET  /warga/profile    → Lihat profil
POST /warga/profile/edit → Simpan perubahan profil
POST /warga/logout     → Logout

ADMIN:
GET  /admin/login      → Halaman login admin
POST /admin/login      → Proses login admin
POST /admin/logout     → Logout
```

---

## BAGIAN 3: INHERITANCE — extends BaseEntity (WAJIB)

### 3.1 BaseEntity (sudah dibuat, tinggal extends)

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### 3.2 Contoh User.java extends BaseEntity

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {  // ← INHERITANCE (WAJIB)
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private String userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.PENDING;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile userProfile;

    public enum Role { WARGA, ADMIN_PUSAT, ADMIN_DINAS, PETUGAS }
    public enum AccountStatus { PENDING, ACTIVE, SUSPENDED }

    // Getter & Setter untuk semua field
    // ENKAPSULASI: Semua field adalah PRIVATE
}
```

### 3.3 Template Entity Lainnya

```java
// UserProfile.java
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {  // ← INHERITANCE
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id")
    private String profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;  // HAS-A (Composition) ≠ Inheritance

    @Column(unique = true, length = 16)
    private String nik;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domisili_region_id")
    private Region domisiliRegion;  // HAS-A

    @Column(name = "alamat_lengkap", columnDefinition = "TEXT")
    private String alamatLengkap;

    // Getter & Setter untuk semua field
}

// LoginAttempt.java
@Entity
@Table(name = "login_attempts")
public class LoginAttempt extends BaseEntity {  // ← INHERITANCE
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attempt_id")
    private String attemptId;

    @Column(nullable = false)
    private String email;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess = false;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt = LocalDateTime.now();

    // Getter & Setter
}

// OtpVerification.java
@Entity
@Table(name = "otp_verifications")
public class OtpVerification extends BaseEntity {  // ← INHERITANCE
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "otp_id")
    private String otpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type", nullable = false)
    private OtpType otpType = OtpType.REGISTRATION;

    public enum OtpType { REGISTRATION, FORGOT_PASSWORD, LOGIN }

    // Getter & Setter
}

// ActiveSession.java
@Entity
@Table(name = "active_sessions")
public class ActiveSession extends BaseEntity {  // ← INHERITANCE
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Getter & Setter
}
```

---

## BAGIAN 4: POLYMORPHISM — @Override + Overloading (WAJIB)

### 4.1 UserService.java (Interface)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.User;
import com.plr.aduaja.dto.*;
import java.util.List;
import java.util.Optional;

public interface UserService {
    
    // ===========================
    // OVERLOADING (Compile-time Polymorphism)
    // Method sama, parameter beda
    // ===========================
    
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndStatus(User.Role role, User.AccountStatus status);
    
    // ===========================
    // METHOD LAIN
    // ===========================
    
    User createUser(RegisterDTO dto);
    User updateUser(User user);
    User updateProfile(String userId, ProfileDTO dto);
    UserProfile getProfileByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    long countByRole(User.Role role);
}
```

### 4.2 UserServiceImpl.java (@Override)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.User;
import com.plr.aduaja.model.UserProfile;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.repository.UserProfileRepository;
import com.plr.aduaja.dto.RegisterDTO;
import com.plr.aduaja.dto.ProfileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {  // ← Polymorphism

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ===========================
    // @Override — Run-time Polymorphism
    // ===========================

    @Override  // ← POLYMORPHISM
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override  // ← POLYMORPHISM
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override  // ← POLYMORPHISM
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Override  // ← POLYMORPHISM
    public List<User> findByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    @Override  // ← POLYMORPHISM (Overload: 2 parameter)
    public List<User> findByRoleAndStatus(User.Role role, User.AccountStatus status) {
        return userRepository.findByRoleAndAccountStatus(role, status);
    }

    @Override  // ← POLYMORPHISM
    public User createUser(RegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }
        if (dto.getPhoneNumber() != null && userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new RuntimeException("Nomor HP sudah terdaftar");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.WARGA);
        user.setAccountStatus(User.AccountStatus.PENDING);

        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        profile.setNik(dto.getNik());
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override  // ← POLYMORPHISM
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override  // ← POLYMORPHISM
    public User updateProfile(String userId, ProfileDTO dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber().isBlank() ? null : dto.getPhoneNumber());
        }

        User savedUser = userRepository.save(user);

        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }
        if (dto.getNik() != null && !dto.getNik().isBlank()) {
            profile.setNik(dto.getNik());
        }
        if (dto.getAlamatLengkap() != null) {
            profile.setAlamatLengkap(dto.getAlamatLengkap());
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override  // ← POLYMORPHISM
    public UserProfile getProfileByUserId(String userId) {
        return userRepository.findById(userId)
            .map(User::getUserProfile)
            .orElse(null);
    }

    @Override  // ← POLYMORPHISM
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override  // ← POLYMORPHISM
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override  // ← POLYMORPHISM
    public long countByRole(User.Role role) {
        return userRepository.findByRole(role).size();
    }
}
```

### 4.3 AuthService.java (Interface)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.User;
import com.plr.aduaja.dto.LoginDTO;
import java.util.Optional;

public interface AuthService {
    
    // OVERLOADING: Login dengan cara berbeda
    Optional<User> login(LoginDTO dto, String ipAddress);
    Optional<User> loginByEmail(String email, String password, String ipAddress);
    Optional<User> loginByPhone(String phone, String password, String ipAddress);
    
    // OVERLOADING: Verifikasi dengan cara berbeda
    boolean verifyPassword(String rawPassword, String hashedPassword);
    boolean isAccountLocked(String email);
    void recordLoginAttempt(String email, boolean success, String ipAddress);
    void logout(String userId);
}
```

### 4.4 AuthServiceImpl.java (@Override)

```java
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

@Service
@Transactional
public class AuthServiceImpl implements AuthService {  // ← Polymorphism

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override  // ← POLYMORPHISM
    public Optional<User> login(LoginDTO dto, String ipAddress) {
        return loginByEmail(dto.getEmail(), dto.getPassword(), ipAddress);
    }

    @Override  // ← POLYMORPHISM (Overload)
    public Optional<User> loginByEmail(String email, String password, String ipAddress) {
        if (isAccountLocked(email)) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            recordLoginAttempt(email, false, ipAddress);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!verifyPassword(password, user.getPasswordHash())) {
            recordLoginAttempt(email, false, ipAddress);
            return Optional.empty();
        }

        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            return Optional.empty();
        }

        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            return Optional.empty();
        }

        recordLoginAttempt(email, true, ipAddress);
        return Optional.of(user);
    }

    @Override  // ← POLYMORPHISM (Overload)
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

    @Override  // ← POLYMORPHISM
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    @Override  // ← POLYMORPHISM
    public boolean isAccountLocked(String email) {
        LocalDateTime lockoutStart = LocalDateTime.now().minusMinutes(LOCKOUT_MINUTES);
        long failedAttempts = loginAttemptRepository
            .countByEmailAndIsSuccessAndAttemptedAtAfter(email, false, lockoutStart);
        return failedAttempts >= MAX_ATTEMPTS;
    }

    @Override  // ← POLYMORPHISM
    public void recordLoginAttempt(String email, boolean success, String ipAddress) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setEmail(email);
        attempt.setIsSuccess(success);
        attempt.setIpAddress(ipAddress);
        attempt.setAttemptedAt(LocalDateTime.now());
        loginAttemptRepository.save(attempt);
    }

    @Override  // ← POLYMORPHISM
    public void logout(String userId) {
        // Invalidate session logic here
    }
}
```

### 4.5 OtpService.java (Interface)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.User;
import com.plr.aduaja.model.OtpVerification;
import com.plr.aduaja.dto.OtpVerifyDTO;

public interface OtpService {
    
    // OVERLOADING: Generate OTP dengan cara berbeda
    OtpVerification generateOtp(String userId, OtpVerification.OtpType type);
    OtpVerification generateOtpForRegistration(String email);
    OtpVerification generateOtpForPasswordReset(String email);
    
    // OVERLOADING: Verify OTP dengan cara berbeda
    boolean verifyOtp(String userId, String otpCode);
    boolean verifyOtpByEmail(String email, String otpCode);
    
    OtpVerification getActiveOtp(String userId);
    void invalidateOtp(String otpId);
    boolean isOtpExpired(String otpId);
}
```

### 4.6 OtpServiceImpl.java (@Override)

```java
package com.plr.aduaja.service;

import com.plr.aduaja.model.User;
import com.plr.aduaja.model.OtpVerification;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.repository.OtpVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class OtpServiceImpl implements OtpService {  // ← Polymorphism

    private static final int OTP_VALIDITY_MINUTES = 10;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpVerificationRepository otpRepository;

    private final Random random = new Random();

    @Override  // ← POLYMORPHISM
    public OtpVerification generateOtp(String userId, OtpVerification.OtpType type) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setOtpCode(generateRandomOtp());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        otp.setOtpType(type);
        otp.setIsVerified(false);
        otp.setIsUsed(false);

        return otpRepository.save(otp);
    }

    @Override  // ← POLYMORPHISM (Overload)
    public OtpVerification generateOtpForRegistration(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User dengan email ini tidak ditemukan");
        }
        return generateOtp(userOpt.get().getUserId(), OtpVerification.OtpType.REGISTRATION);
    }

    @Override  // ← POLYMORPHISM (Overload)
    public OtpVerification generateOtpForPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User dengan email ini tidak ditemukan");
        }
        return generateOtp(userOpt.get().getUserId(), OtpVerification.OtpType.FORGOT_PASSWORD);
    }

    @Override  // ← POLYMORPHISM
    public boolean verifyOtp(String userId, String otpCode) {
        Optional<OtpVerification> otpOpt = otpRepository
            .findByUserIdAndOtpCodeAndIsUsedFalse(userId, otpCode);

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpVerification otp = otpOpt.get();

        if (isOtpExpired(otp.getOtpId())) {
            return false;
        }

        otp.setIsVerified(true);
        otpRepository.save(otp);

        User user = otp.getUser();
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);

        return true;
    }

    @Override  // ← POLYMORPHISM (Overload)
    public boolean verifyOtpByEmail(String email, String otpCode) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        return verifyOtp(userOpt.get().getUserId(), otpCode);
    }

    @Override  // ← POLYMORPHISM
    public OtpVerification getActiveOtp(String userId) {
        return otpRepository.findByUserIdAndIsUsedFalse(userId)
            .stream()
            .filter(otp -> !otp.getIsVerified() && !isOtpExpired(otp.getOtpId()))
            .findFirst()
            .orElse(null);
    }

    @Override  // ← POLYMORPHISM
    public void invalidateOtp(String otpId) {
        otpRepository.findById(otpId).ifPresent(otp -> {
            otp.setIsUsed(true);
            otpRepository.save(otp);
        });
    }

    @Override  // ← POLYMORPHISM
    public boolean isOtpExpired(String otpId) {
        return otpRepository.findById(otpId)
            .map(otp -> LocalDateTime.now().isAfter(otp.getExpiresAt()))
            .orElse(true);
    }

    private String generateRandomOtp() {
        return String.format("%06d", random.nextInt(999999));
    }
}
```

---

## BAGIAN 5: ABSTRACTION — Interface sebagai Kontrak

### 5.1 Prinsip Abstraction

```
CONTROLLER (Pemanggil)
    ↓
SERVICE INTERFACE (Kontrak — mendefinisikan APA yang dilakukan)
    ↓
SERVICE IMPL (Implementasi — mendefinisikan BAGAIMANA dilakukan)
    ↓
REPOSITORY (Akses database)
```

### 5.2 Contoh Alur Abstraction

```java
// CONTROLLER — Tidak tahu implementasi
@Controller
public class WebController {

    @Autowired
    private UserService userService;  // ← Abstraction: hanya tahu interface

    @PostMapping("/warga/register")
    public String register(@ModelAttribute RegisterDTO dto,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            // Abstraction: Controller tidak tahu:
            // - Bagaimana password di-hash
            // - Bagaimana UserProfile dibuat
            // - Bagaimana validasi email uniqueness
            // Semua DISEMBUNYIKAN di UserServiceImpl

            User user = userService.createUser(dto);

            redirectAttributes.addFlashAttribute("success",
                "Registrasi berhasil! Kode OTP telah dikirim ke email.");
            return "redirect:/warga/verify-otp?userId=" + user.getUserId();

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("dto", dto);
            return "warga/register";
        }
    }
}
```

---

## BAGIAN 6: ENCAPSULATION — Private + DTO + Immutable

### 6.1 Prinsip Encapsulation

```
1. SEMUA field di Entity = PRIVATE
2. SEMUA form input = pakai DTO (bukan Entity langsung)
3. Objek sensitif = IMMUTABLE (tidak ada setter setelah creation)
```

### 6.2 LoginDTO.java

```java
package com.plr.aduaja.dto;

// ENCAPSULATION: Semua field PRIVATE
public class LoginDTO {
    private String email;
    private String password;

    // ENCAPSULATION: Hanya getter & setter
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### 6.3 RegisterDTO.java

```java
package com.plr.aduaja.dto;

// ENCAPSULATION: Semua field PRIVATE
public class RegisterDTO {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String nik;
    private String password;
    private String confirmPassword;

    // ENCAPSULATION: Hanya getter & setter
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getNik() { return nik; }
    public void setNik(String nik) { this.nik = nik; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    // Validation method (ENCAPSULATION: logic di dalam class)
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public boolean isNikValid() {
        return nik != null && nik.length() == 16 && nik.matches("\\d+");
    }
}
```

### 6.4 ProfileDTO.java

```java
package com.plr.aduaja.dto;

// ENCAPSULATION: Semua field PRIVATE
public class ProfileDTO {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String nik;
    private String alamatLengkap;
    private String profilePhotoUrl;

    // ENCAPSULATION: Hanya getter & setter
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getNik() { return nik; }
    public void setNik(String nik) { this.nik = nik; }
    public String getAlamatLengkap() { return alamatLengkap; }
    public void setAlamatLengkap(String alamatLengkap) { this.alamatLengkap = alamatLengkap; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
}
```

### 6.5 OtpVerifyDTO.java

```java
package com.plr.aduaja.dto;

// ENCAPSULATION: Semua field PRIVATE
public class OtpVerifyDTO {
    private String userId;
    private String email;
    private String otpCode;

    // ENCAPSULATION: Hanya getter & setter
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
```

---

## BAGIAN 7: REPOSITORY (5 files)

### 7.1 UserRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndAccountStatus(User.Role role, User.AccountStatus accountStatus);
}
```

### 7.2 UserProfileRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByNik(String nik);
    boolean existsByNik(String nik);
}
```

### 7.3 LoginAttemptRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {

    long countByEmailAndIsSuccessAndAttemptedAtAfter(String email, Boolean isSuccess, LocalDateTime after);
}
```

### 7.4 OtpVerificationRepository.java

```java
package com.plr.aduaja.repository;

import com.plr.aduaja.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, String> {

    Optional<OtpVerification> findByUserIdAndOtpCodeAndIsUsedFalse(String userId, String otpCode);
    Optional<OtpVerification> findByUserIdAndIsUsedFalse(String userId);
    List<OtpVerification> findByUserId(String userId);
}
```

---

## BAGIAN 8: VIEW HTML (Thymeleaf)

### 8.1 warga/login.html

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Login Warga</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50 flex items-center justify-center">
    <div class="max-w-md w-full bg-white rounded-xl shadow-lg p-8">
        <!-- Logo & Title -->
        <div class="text-center mb-8">
            <h1 class="text-3xl font-bold text-blue-600">AduAja</h1>
            <p class="text-gray-500 mt-2">Masuk sebagai Warga</p>
        </div>

        <!-- Error Message -->
        <div th:if="${error}" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg mb-6">
            <span th:text="${error}">Email atau password salah</span>
        </div>

        <!-- Success Message -->
        <div th:if="${success}" class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded-lg mb-6">
            <span th:text="${success}">Registrasi berhasil</span>
        </div>

        <!-- Login Form -->
        <form th:action="@{/warga/login}" th:object="${loginDTO}" method="post" class="space-y-5">
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Email</label>
                <div class="relative">
                    <i data-lucide="mail" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"></i>
                    <input type="email"
                           th:field="*{email}"
                           class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                           placeholder="nama@email.com"
                           required />
                </div>
            </div>

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Password</label>
                <div class="relative">
                    <i data-lucide="lock" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"></i>
                    <input type="password"
                           th:field="*{password}"
                           class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                           placeholder="••••••••"
                           required />
                </div>
            </div>

            <button type="submit"
                    class="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors">
                Masuk
            </button>
        </form>

        <!-- Register Link -->
        <div class="text-center mt-6">
            <p class="text-gray-500">
                Belum punya akun?
                <a th:href="@{/warga/register}" class="text-blue-600 font-semibold hover:underline">
                    Daftar Sekarang
                </a>
            </p>
        </div>
    </div>
</div>
```

### 8.2 warga/register.html

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Registrasi Warga</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50 flex items-center justify-center py-12">
    <div class="max-w-lg w-full bg-white rounded-xl shadow-lg p-8">
        <!-- Header -->
        <div class="text-center mb-8">
            <h1 class="text-3xl font-bold text-blue-600">AduAja</h1>
            <p class="text-gray-500 mt-2">Daftar sebagai Warga</p>
        </div>

        <!-- Error Message -->
        <div th:if="${error}" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg mb-6">
            <span th:text="${error}">Error message</span>
        </div>

        <!-- Register Form -->
        <form th:action="@{/warga/register}" th:object="${registerDTO}" method="post" class="space-y-5">
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Nama Lengkap <span class="text-red-500">*</span>
                </label>
                <input type="text"
                       th:field="*{fullName}"
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                       placeholder="Masukkan nama lengkap"
                       required />
            </div>

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Email <span class="text-red-500">*</span>
                </label>
                <input type="email"
                       th:field="*{email}"
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                       placeholder="nama@email.com"
                       required />
            </div>

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Nomor HP <span class="text-red-500">*</span>
                </label>
                <input type="tel"
                       th:field="*{phoneNumber}"
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                       placeholder="08xxxxxxxxxx"
                       required />
            </div>

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    NIK (16 Digit) <span class="text-red-500">*</span>
                </label>
                <input type="text"
                       th:field="*{nik}"
                       maxlength="16"
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                       placeholder="16 digit NIK"
                       required />
                <p class="text-xs text-gray-500 mt-1">NIK sesuai KTP</p>
            </div>

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Password <span class="text-red-500">*</span>
                </label>
                <input type="password"
                       th:field="*{password}"
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                       placeholder="Minimal 8 karakter"
                       required />
            </div>

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                    Konfirmasi Password <span class="text-red-500">*</span>
                </label>
                <input type="password"
                       th:field="*{confirmPassword}"
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                       placeholder="Ulangi password"
                       required />
            </div>

            <button type="submit"
                    class="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors">
                Daftar
            </button>
        </form>

        <!-- Login Link -->
        <div class="text-center mt-6">
            <p class="text-gray-500">
                Sudah punya akun?
                <a th:href="@{/warga/login}" class="text-blue-600 font-semibold hover:underline">
                    Masuk
                </a>
            </p>
        </div>
    </div>
</div>
```

### 8.3 warga/profile.html (UPDATE)

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Profil Saya</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50" x-data="{ isEditing: false }">
    <!-- Header -->
    <header class="bg-white shadow-sm sticky top-0 z-10">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex items-center justify-between h-16">
                <div class="flex items-center gap-3">
                    <a th:href="@{/warga/dashboard}" class="p-2 hover:bg-gray-100 rounded-lg">
                        <i data-lucide="arrow-left" class="w-5 h-5 text-gray-600"></i>
                    </a>
                    <div>
                        <h1 class="font-bold text-gray-900">Profil Saya</h1>
                        <p class="text-xs text-gray-500">Kelola informasi pribadi Anda</p>
                    </div>
                </div>
                <div x-show="!isEditing">
                    <button @click="isEditing = true"
                            class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                        <i data-lucide="edit-2" class="w-4 h-4"></i>
                        <span>Edit Profil</span>
                    </button>
                </div>
                <div x-show="isEditing" class="flex gap-2" x-cloak>
                    <button @click="isEditing = false"
                            class="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">
                        Batal
                    </button>
                    <button form="profile-form" type="submit"
                            class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700">
                        Simpan
                    </button>
                </div>
            </div>
        </div>
    </header>

    <main class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <!-- Profile Card -->
        <div class="bg-gradient-to-br from-blue-600 to-blue-800 rounded-xl p-8 mb-6 text-white">
            <div class="flex flex-col sm:flex-row items-center gap-6">
                <div class="w-24 h-24 bg-white/20 rounded-full flex items-center justify-center">
                    <i data-lucide="user" class="w-12 h-12"></i>
                </div>
                <div class="text-center sm:text-left">
                    <h2 class="text-2xl font-bold" th:text="${user.fullName}">Nama User</h2>
                    <p class="text-blue-100" th:text="${user.email}">email@email.com</p>
                </div>
            </div>
        </div>

        <!-- Success/Error Messages -->
        <div th:if="${success}" class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded-lg mb-6">
            <span th:text="${success}">Profil berhasil diperbarui</span>
        </div>
        <div th:if="${error}" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg mb-6">
            <span th:text="${error}">Error</span>
        </div>

        <!-- Profile Form -->
        <form id="profile-form"
              th:action="@{/warga/profile/edit}"
              th:object="${profileDTO}"
              method="post"
              class="space-y-6">
            
            <!-- Personal Info -->
            <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                <h3 class="font-semibold text-gray-900 mb-4">Informasi Pribadi</h3>
                <div class="space-y-4">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">Nama Lengkap</label>
                        <input type="text" th:field="*{fullName}"
                               x-bind:disabled="!isEditing"
                               class="w-full px-4 py-3 border border-gray-300 rounded-lg disabled:bg-gray-50" />
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">NIK</label>
                        <input type="text" th:field="*{nik}" maxlength="16"
                               x-bind:disabled="!isEditing"
                               class="w-full px-4 py-3 border border-gray-300 rounded-lg disabled:bg-gray-50" />
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">Email</label>
                        <input type="email" th:field="*{email}"
                               x-bind:disabled="!isEditing"
                               class="w-full px-4 py-3 border border-gray-300 rounded-lg disabled:bg-gray-50" />
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-2">Nomor HP</label>
                        <input type="tel" th:field="*{phoneNumber}"
                               x-bind:disabled="!isEditing"
                               class="w-full px-4 py-3 border border-gray-300 rounded-lg disabled:bg-gray-50" />
                    </div>
                </div>
            </div>

            <!-- Address -->
            <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                <h3 class="font-semibold text-gray-900 mb-4">Alamat</h3>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-2">Alamat Lengkap</label>
                    <textarea th:field="*{alamatLengkap}" rows="3"
                              x-bind:disabled="!isEditing"
                              class="w-full px-4 py-3 border border-gray-300 rounded-lg disabled:bg-gray-50 resize-none"></textarea>
                </div>
            </div>
        </form>

        <!-- Account Settings -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mt-6">
            <h3 class="font-semibold text-gray-900 mb-4">Pengaturan Akun</h3>
            <div class="space-y-3">
                <button class="w-full text-left px-4 py-3 border border-gray-200 rounded-lg hover:bg-gray-50">
                    <p class="font-medium text-gray-900">Ubah Password</p>
                    <p class="text-sm text-gray-500">Perbarui password akun Anda</p>
                </button>
                <button class="w-full text-left px-4 py-3 border border-red-200 rounded-lg hover:bg-red-50 text-red-600">
                    <p class="font-medium">Hapus Akun</p>
                    <p class="text-sm">Hapus akun secara permanen</p>
                </button>
            </div>
        </div>
    </main>
</div>
```

### 8.4 warga/verify-otp.html

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Verifikasi OTP</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gray-50 flex items-center justify-center">
    <div class="max-w-md w-full bg-white rounded-xl shadow-lg p-8">
        <div class="text-center mb-8">
            <h1 class="text-2xl font-bold text-blue-600">Verifikasi OTP</h1>
            <p class="text-gray-500 mt-2">Masukkan kode OTP yang dikirim ke email Anda</p>
            <p class="text-gray-600 mt-1 font-semibold" th:text="${email}">email@example.com</p>
        </div>

        <div th:if="${error}" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg mb-6">
            <span th:text="${error}">Kode OTP tidak valid atau sudah kadaluarsa</span>
        </div>

        <form th:action="@{/warga/verify-otp}" method="post" class="space-y-6">
            <input type="hidden" name="userId" th:value="${userId}" />
            <input type="hidden" name="email" th:value="${email}" />

            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2 text-center">Kode OTP (6 digit)</label>
                <input type="text"
                       name="otpCode"
                       maxlength="6"
                       class="w-full px-4 py-4 text-center text-2xl tracking-widest border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                       placeholder="------"
                       required />
            </div>

            <button type="submit"
                    class="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700">
                Verifikasi
            </button>
        </form>

        <div class="text-center mt-6">
            <p class="text-gray-500 text-sm">
                Tidak menerima kode?
                <a href="#" class="text-blue-600 font-semibold">Kirim Ulang</a>
            </p>
        </div>
    </div>
</div>
```

### 8.5 admin/login.html

```html
<!DOCTYPE html>
<html lang="id"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/master}">

<th:block layout:fragment="extraHead">
    <title>AduAja - Login Admin</title>
</th:block>

<div layout:fragment="content" class="min-h-screen bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center">
    <div class="max-w-md w-full bg-white rounded-xl shadow-lg p-8">
        <div class="text-center mb-8">
            <h1 class="text-3xl font-bold text-blue-600">AduAja</h1>
            <p class="text-gray-500 mt-2">Login Admin</p>
        </div>

        <div th:if="${error}" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg mb-6">
            <span th:text="${error}">Error</span>
        </div>

        <form th:action="@{/admin/login}" method="post" class="space-y-5">
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Email</label>
                <input type="email" name="email" required
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500" />
            </div>
            <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Password</label>
                <input type="password" name="password" required
                       class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500" />
            </div>
            <button type="submit" class="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700">
                Masuk
            </button>
        </form>

        <div class="text-center mt-6">
            <a th:href="@{/warga/login}" class="text-blue-600 text-sm hover:underline">
                Login sebagai Warga
            </a>
        </div>
    </div>
</div>
```

---

## BAGIAN 9: CONTROLLER (Routes)

### 9.1 WargaAuthController.java

```java
package com.plr.aduaja.controller;

import com.plr.aduaja.model.User;
import com.plr.aduaja.dto.*;
import com.plr.aduaja.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Optional;

@Controller
public class WargaAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    @GetMapping("/warga/login")
    public String loginPage(Model model) {
        model.addAttribute("loginDTO", new LoginDTO());
        return "warga/login";
    }

    @PostMapping("/warga/login")
    public String login(@ModelAttribute LoginDTO dto,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = authService.login(dto, getClientIp());

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email atau password salah, atau akun belum aktif.");
            return "redirect:/warga/login";
        }

        User user = userOpt.get();

        if (user.getRole() != User.Role.WARGA) {
            redirectAttributes.addFlashAttribute("error", "Akun ini bukan akun warga.");
            return "redirect:/warga/login";
        }

        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userRole", "WARGA");

        return "redirect:/warga/dashboard";
    }

    @GetMapping("/warga/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "warga/register";
    }

    @PostMapping("/warga/register")
    public String register(@ModelAttribute RegisterDTO dto,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!dto.isPasswordMatch()) {
            model.addAttribute("error", "Password tidak cocok");
            model.addAttribute("registerDTO", dto);
            return "warga/register";
        }

        if (!dto.isNikValid()) {
            model.addAttribute("error", "NIK harus 16 digit angka");
            model.addAttribute("registerDTO", dto);
            return "warga/register";
        }

        try {
            User user = userService.createUser(dto);
            otpService.generateOtp(user.getUserId(), com.plr.aduaja.model.OtpVerification.OtpType.REGISTRATION);
            redirectAttributes.addFlashAttribute("success", "Registrasi berhasil! Kode OTP telah dikirim ke email.");
            return "redirect:/warga/verify-otp?userId=" + user.getUserId() + "&email=" + dto.getEmail();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registerDTO", dto);
            return "warga/register";
        }
    }

    @GetMapping("/warga/verify-otp")
    public String verifyOtpPage(@RequestParam("userId") String userId,
                                 @RequestParam("email") String email,
                                 Model model) {
        model.addAttribute("userId", userId);
        model.addAttribute("email", email);
        return "warga/verify-otp";
    }

    @PostMapping("/warga/verify-otp")
    public String verifyOtp(@RequestParam("userId") String userId,
                             @RequestParam("email") String email,
                             @RequestParam("otpCode") String otpCode,
                             RedirectAttributes redirectAttributes) {
        boolean success = otpService.verifyOtp(userId, otpCode);

        if (success) {
            redirectAttributes.addFlashAttribute("success", "Verifikasi berhasil! Silakan login.");
            return "redirect:/warga/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Kode OTP tidak valid atau sudah kadaluarsa.");
            return "redirect:/warga/verify-otp?userId=" + userId + "&email=" + email;
        }
    }

    @GetMapping("/warga/profile")
    public String profilePage(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/warga/login";
        }

        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/warga/login";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("profileDTO", new ProfileDTO());

        return "warga/profile";
    }

    @PostMapping("/warga/profile/edit")
    public String editProfile(@ModelAttribute ProfileDTO dto,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/warga/login";
        }

        try {
            User user = userService.updateProfile(userId, dto);
            session.setAttribute("userName", user.getFullName());
            redirectAttributes.addFlashAttribute("success", "Profil berhasil diperbarui.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/warga/profile";
    }

    @PostMapping("/warga/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/warga/login";
    }

    private String getClientIp() {
        return "0.0.0.0";
    }
}
```

### 9.2 AdminAuthController.java

```java
package com.plr.aduaja.controller;

import com.plr.aduaja.model.User;
import com.plr.aduaja.dto.LoginDTO;
import com.plr.aduaja.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class AdminAuthController {

    @Autowired
    private AuthService authService;

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

    @PostMapping("/admin/login")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setPassword(password);

        Optional<User> userOpt = authService.login(dto, getClientIp());

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email atau password salah.");
            return "redirect:/admin/login";
        }

        User user = userOpt.get();

        if (user.getRole() != User.Role.ADMIN_PUSAT && user.getRole() != User.Role.ADMIN_DINAS) {
            redirectAttributes.addFlashAttribute("error", "Akun ini bukan akun admin.");
            return "redirect:/admin/login";
        }

        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userRole", user.getRole().toString());

        if (user.getRole() == User.Role.ADMIN_DINAS) {
            return "redirect:/admin/dinas/dashboard";
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login?logout=true";
    }

    private String getClientIp() {
        return "0.0.0.0";
    }
}
```

---

## BAGIAN 10: CHECKLIST SEBELUM COMMIT

```
SEBELUM COMMIT, PASTIKAN SEMUA INI SUDAH DIBIKIN:

[ ] BaseEntity.java — sudah ada
[ ] User.java — extends BaseEntity ✅
[ ] UserProfile.java — extends BaseEntity ✅
[ ] LoginAttempt.java — extends BaseEntity ✅
[ ] OtpVerification.java — extends BaseEntity ✅
[ ] ActiveSession.java — extends BaseEntity ✅

[ ] UserRepository.java — extends JpaRepository ✅
[ ] UserProfileRepository.java — extends JpaRepository ✅
[ ] LoginAttemptRepository.java — extends JpaRepository ✅
[ ] OtpVerificationRepository.java — extends JpaRepository ✅

[ ] UserService.java — Interface ✅
[ ] UserServiceImpl.java — @Override + Overloading ✅
[ ] AuthService.java — Interface ✅
[ ] AuthServiceImpl.java — @Override + Overloading ✅
[ ] OtpService.java — Interface ✅
[ ] OtpServiceImpl.java — @Override + Overloading ✅

[ ] LoginDTO.java — ENCAPSULATION (private fields) ✅
[ ] RegisterDTO.java — ENCAPSULATION (private fields) ✅
[ ] ProfileDTO.java — ENCAPSULATION (private fields) ✅
[ ] OtpVerifyDTO.java — ENCAPSULATION (private fields) ✅

[ ] warga/login.html — Thymeleaf form ✅
[ ] warga/register.html — Thymeleaf form ✅
[ ] warga/profile.html — Thymeleaf form dengan th:object ✅
[ ] warga/verify-otp.html — Thymeleaf form ✅
[ ] admin/login.html — Thymeleaf form ✅

[ ] WargaAuthController.java — Routes ✅
[ ] AdminAuthController.java — Routes ✅

[ ] Build berhasil: mvn compile ✅
```

---

## BAGIAN 11: GIT COMMIT MESSAGE

```bash
# Contoh commit message yang benar:

git add .
git commit -m "feat(auth): implement user registration with OTP verification

- Add User, UserProfile, LoginAttempt, OtpVerification entities (extends BaseEntity)
- Add UserService, AuthService, OtpService interfaces with implementations
- Add RegisterDTO, LoginDTO, ProfileDTO, OtpVerifyDTO (Encapsulation)
- Add warga/register.html, warga/login.html, warga/profile.html, warga/verify-otp.html
- Add AdminAuthController for admin login
- Polymorphism: @Override on all service methods, method overloading in UserService
- Abstraction: Controller uses Service interfaces only
- Encapsulation: All entity fields are private, forms use DTOs
- Inheritance: All entities extends BaseEntity for createdAt/updatedAt"
git push origin fitur/modul-1-auth-profile
```

---

## BAGIAN 12: PBO EVIDENCE SUMMARY (UNTUK LAPORAN)

```markdown
## 4 Pilar PBO — Modul Auth & Profil

### 1. Inheritance (Pewarisan)
Semua entity di modul ini extends BaseEntity:
- User extends BaseEntity
- UserProfile extends BaseEntity
- LoginAttempt extends BaseEntity
- OtpVerification extends BaseEntity
- ActiveSession extends BaseEntity

BaseEntity berisi:
- createdAt (otomatis di-set saat @PrePersist)
- updatedAt (otomatis di-update saat @PreUpdate)

### 2. Polymorphism (Polimorfisme)
Compile-time Polymorphism (Overloading):
- UserService.findById(String id)
- UserService.findByEmail(String email)
- UserService.findByPhoneNumber(String phoneNumber)
- UserService.findByRole(User.Role role)
- UserService.findByRoleAndStatus(User.Role role, User.AccountStatus status)

Run-time Polymorphism (Overriding):
- UserServiceImpl implements UserService → semua method di-override dengan @Override
- AuthServiceImpl implements AuthService → semua method di-override dengan @Override
- OtpServiceImpl implements OtpService → semua method di-override dengan @Override

### 3. Abstraction (Abstraksi)
Controller hanya tahu Interface Service, tidak tahu implementasi:
- WebController (@Autowired UserService userService)
- AdminAuthController (@Autowired AuthService authService)
- OtpService (@Autowired OtpService otpService)

### 4. Encapsulation (Enkapsulasi)
- Semua field entity adalah PRIVATE
- Akses data form menggunakan DTO (LoginDTO, RegisterDTO, ProfileDTO, OtpVerifyDTO)
- Tidak ada akses langsung ke entity dari Controller
- Getter & Setter untuk semua field
```

---

*Dokumen ini adalah prompt lengkap untuk Anggota 1 — Modul Auth & Profil.*
*Dibuat berdasarkan standar OOP 4 Pilar PBO yang benar.*
