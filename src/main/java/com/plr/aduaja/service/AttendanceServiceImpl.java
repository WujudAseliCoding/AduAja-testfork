package com.plr.aduaja.service;

import com.plr.aduaja.model.*;
import com.plr.aduaja.model.OfficerAttendance.ShiftStatus;
import com.plr.aduaja.repository.*;
import com.plr.aduaja.util.GeoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private OfficerAttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<OfficerAttendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    @Override
    public List<OfficerAttendance> getAttendanceByOfficer(String officerId) {
        return attendanceRepository.findByOfficerUserId(officerId);
    }

    @Override
    public List<OfficerAttendance> getAttendanceByOfficerAndDateRange(String officerId, LocalDateTime start, LocalDateTime end) {
        return attendanceRepository.findByOfficerUserIdAndCheckInAtBetween(officerId, start, end);
    }

    @Override
    public Optional<OfficerAttendance> getCurrentShift(String officerId) {
        return attendanceRepository.findTopByOfficerUserIdAndShiftStatusNotOrderByCheckInAtDesc(
                officerId, ShiftStatus.SELESAI_SHIFT);
    }

    @Override
    public OfficerAttendance checkIn(String officerId, BigDecimal latitude, BigDecimal longitude, String deviceInfo) {
        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Officer not found"));

        OfficerAttendance attendance = new OfficerAttendance();
        attendance.setOfficer(officer);
        attendance.setCheckInAt(LocalDateTime.now());
        attendance.setCheckInLatitude(latitude);
        attendance.setCheckInLongitude(longitude);
        attendance.setDeviceInfo(deviceInfo);
        attendance.setShiftStatus(ShiftStatus.AKTIF);

        return attendanceRepository.save(attendance);
    }

    @Override
    public OfficerAttendance checkInWithGeofence(String officerId,
                                                  BigDecimal latitude, BigDecimal longitude,
                                                  String deviceInfo,
                                                  double maxRadiusKm,
                                                  double centerLat, double centerLon) {
        // Validasi GPS dalam radius wilayah kerja (backend enforcement)
        if (latitude != null && longitude != null) {
            double distKm = GeoUtils.haversineKm(latitude, longitude,
                    new java.math.BigDecimal(centerLat),
                    new java.math.BigDecimal(centerLon));
            
            // Dapatkan officer untuk mengecek apakah ini akun dummy
            User officer = userRepository.findById(officerId).orElse(null);
            boolean isDummyAccount = officer != null && officer.getEmail().endsWith("@aduaja.go.id");
            
            if (distKm > maxRadiusKm && !isDummyAccount) {
                throw new IllegalStateException(
                    String.format("Lokasi Anda (%.2f km) berada di luar radius wilayah kerja (%.0f km). " +
                                  "Pastikan GPS aktif dan Anda berada di area tugas.", distKm, maxRadiusKm));
            } else if (distKm > maxRadiusKm && isDummyAccount) {
                // Log bypass untuk akun dummy
                System.out.println("GEOFENCING BYPASS: Akun dummy " + officer.getEmail() + " diizinkan check-in meski di luar radius.");
            }
        }
        // Jika koordinat null, tetap izinkan check-in namun tandai GPS_MANUAL
        String finalDeviceInfo = (latitude == null || longitude == null)
                ? (deviceInfo != null ? deviceInfo + " [GPS_MANUAL]" : "[GPS_MANUAL]")
                : deviceInfo;
        return checkIn(officerId, latitude, longitude, finalDeviceInfo);
    }

    @Override
    public OfficerAttendance checkOut(String attendanceId) {
        OfficerAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
        attendance.setCheckOutAt(LocalDateTime.now());
        attendance.setShiftStatus(ShiftStatus.SELESAI_SHIFT);
        return attendanceRepository.save(attendance);
    }

    @Override
    public OfficerAttendance setBreak(String attendanceId) {
        OfficerAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
        attendance.setShiftStatus(ShiftStatus.ISTIRAHAT);
        return attendanceRepository.save(attendance);
    }

    @Override
    public OfficerAttendance resumeFromBreak(String attendanceId) {
        OfficerAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));
        attendance.setShiftStatus(ShiftStatus.AKTIF);
        return attendanceRepository.save(attendance);
    }
}
