package com.plr.aduaja.service;

import com.plr.aduaja.model.*;
import com.plr.aduaja.repository.*;
import com.plr.aduaja.dto.CreatePetugasDTO;
import com.plr.aduaja.dto.RegisterDTO;
import com.plr.aduaja.dto.ProfileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// ============================================================
// POLYMORPHISM (Run-time Polymorphism): UserServiceImpl
// Mengimplementasikan UserService interface → @Override setiap method
//
// ABSTRACTION: Controller tidak perlu tahu implementasi ini,
// hanya tahu interface UserService
// ============================================================
@Service
@Transactional
public class UserServiceImpl implements UserService {  // ← POLYMORPHISM

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // CATATAN: @PostConstruct activatePendingUsers() dihapus.
    // Aktivasi akun HANYA dilakukan melalui OtpServiceImpl.verifyOtp()
    // setelah user berhasil verifikasi kode OTP.
    // Mengaktifkan semua PENDING otomatis akan mem-bypass proses verifikasi OTP.

    // ===========================
    // @Override — Run-time Polymorphism
    // Mengimplementasikan semua method dari UserService interface
    // ===========================

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public List<User> findByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload: 2 parameter)
    public List<User> findByRoleAndStatus(User.Role role, User.AccountStatus status) {
        return userRepository.findByRoleAndAccountStatus(role, status);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public User createUser(RegisterDTO dto) {
        // ABSTRACTION: Semua logika kompleks disembunyikan dari Controller
        // Email — controller sudah handle PENDING sebelum panggil method ini
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }
        // Phone — hanya ACTIVE/SUSPENDED yang dianggap konflik, PENDING dianggap bebas
        if (dto.getPhoneNumber() != null) {
            Optional<User> existingPhone = userRepository.findByPhoneNumber(dto.getPhoneNumber());
            if (existingPhone.isPresent() && existingPhone.get().getAccountStatus() != User.AccountStatus.PENDING) {
                throw new RuntimeException("Nomor HP sudah terdaftar");
            }
        }

        // Buat User baru
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        // ENKAPSULASI: password di-hash, tidak pernah disimpan plaintext
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.WARGA);
        // Status PENDING: user harus verifikasi OTP dulu sebelum bisa login
        // OtpServiceImpl.verifyOtp() yang akan mengubah status ke ACTIVE
        user.setAccountStatus(User.AccountStatus.PENDING);

        User savedUser = userRepository.save(user);

        // Buat UserProfile dengan NIK
        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        if (dto.getNik() != null && !dto.getNik().isBlank()) {
            // Cek NIK — hanya ACTIVE/SUSPENDED yang dianggap konflik
            Optional<UserProfile> existingProfile = userProfileRepository.findByNik(dto.getNik());
            if (existingProfile.isPresent()) {
                User profileOwner = existingProfile.get().getUser();
                if (profileOwner.getAccountStatus() != User.AccountStatus.PENDING) {
                    throw new RuntimeException("NIK sudah terdaftar");
                }
            }
            profile.setNik(dto.getNik());
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    public User updatePendingRegistration(RegisterDTO dto, String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        if (user.getAccountStatus() != User.AccountStatus.PENDING) {
            throw new RuntimeException("Akun sudah aktif, tidak bisa update data registrasi");
        }

        // Update data user
        user.setFullName(dto.getFullName());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        // Phone: cek konflik hanya dengan ACTIVE/SUSPENDED
        if (dto.getPhoneNumber() != null) {
            Optional<User> existingPhone = userRepository.findByPhoneNumber(dto.getPhoneNumber());
            if (existingPhone.isPresent()
                && !existingPhone.get().getUserId().equals(userId)
                && existingPhone.get().getAccountStatus() != User.AccountStatus.PENDING) {
                throw new RuntimeException("Nomor HP sudah terdaftar");
            }
            user.setPhoneNumber(dto.getPhoneNumber());
        } else {
            user.setPhoneNumber(null);
        }

        User savedUser = userRepository.save(user);

        // Update profile & NIK
        UserProfile profile = userProfileRepository.findByUserUserId(userId).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(savedUser);
        }
        if (dto.getNik() != null && !dto.getNik().isBlank()) {
            Optional<UserProfile> existingProfile = userProfileRepository.findByNik(dto.getNik());
            if (existingProfile.isPresent()
                && !existingProfile.get().getUser().getUserId().equals(userId)
                && existingProfile.get().getUser().getAccountStatus() != User.AccountStatus.PENDING) {
                throw new RuntimeException("NIK sudah terdaftar");
            }
            profile.setNik(dto.getNik());
        } else {
            profile.setNik(null);
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    public User createPetugas(CreatePetugasDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }
        // Tidak cek uniqueness nomor HP — satu orang boleh punya akun warga dan petugas
        // dengan nomor HP yang sama (kolom phone_number memang tidak unique di DB)

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.PETUGAS);
        user.setAccountStatus(User.AccountStatus.PENDING);

        if (dto.getAgencyId() != null && !dto.getAgencyId().isBlank()) {
            Agency agency = agencyRepository.findById(dto.getAgencyId())
                    .orElseThrow(() -> new RuntimeException("Agency tidak ditemukan"));
            user.setAgency(agency);
        }

        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        if (dto.getNip() != null && !dto.getNip().isBlank()) {
            profile.setNip(dto.getNip());
        }
        if (dto.getWilayahTugasRegionId() != null && !dto.getWilayahTugasRegionId().isBlank()) {
            Region wilayah = regionRepository.findById(dto.getWilayahTugasRegionId())
                    .orElseThrow(() -> new RuntimeException("Wilayah tidak ditemukan"));
            profile.setWilayahTugas(wilayah);
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public User updateProfile(String userId, ProfileDTO dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        // Update data User
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            // Cek email sudah dipakai oleh user lain
            Optional<User> existingEmail = userRepository.findByEmail(dto.getEmail());
            if (existingEmail.isPresent() && !existingEmail.get().getUserId().equals(userId)) {
                throw new RuntimeException("Email sudah dipakai oleh akun lain");
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber().isBlank() ? null : dto.getPhoneNumber());
        }

        User savedUser = userRepository.save(user);

        // Update UserProfile
        UserProfile profile = userProfileRepository.findByUserUserId(userId).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }
        if (dto.getNik() != null && !dto.getNik().isBlank()) {
            profile.setNik(dto.getNik());
        }
        if (dto.getNip() != null && !dto.getNip().isBlank()) {
            profile.setNip(dto.getNip());
        }
        if (dto.getWilayahTugasRegionId() != null && !dto.getWilayahTugasRegionId().isBlank()) {
            Region wilayah = regionRepository.findById(dto.getWilayahTugasRegionId())
                    .orElseThrow(() -> new RuntimeException("Wilayah tidak ditemukan"));
            profile.setWilayahTugas(wilayah);
        }
        if (dto.getAlamatLengkap() != null) {
            profile.setAlamatLengkap(dto.getAlamatLengkap());
        }
        if (dto.getDomisiliLatitude() != null && !dto.getDomisiliLatitude().isBlank()) {
            profile.setDomisiliLatitude(new BigDecimal(dto.getDomisiliLatitude()));
        }
        if (dto.getDomisiliLongitude() != null && !dto.getDomisiliLongitude().isBlank()) {
            profile.setDomisiliLongitude(new BigDecimal(dto.getDomisiliLongitude()));
        }
        if (dto.getProfilePhotoUrl() != null && !dto.getProfilePhotoUrl().isBlank()) {
            profile.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public UserProfile getProfileByUserId(String userId) {
        return userRepository.findById(userId)
            .map(User::getUserProfile)
            .orElse(null);
    }

    @Override
    public void changePassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            user.setAccountStatus(User.AccountStatus.ACTIVE);
        }
        userRepository.save(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public long countByRole(User.Role role) {
        return userRepository.findByRole(role).size();
    }
}
