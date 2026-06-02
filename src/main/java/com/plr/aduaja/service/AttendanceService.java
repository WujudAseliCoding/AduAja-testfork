package com.plr.aduaja.service;

import com.plr.aduaja.model.OfficerAttendance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceService {

    List<OfficerAttendance> getAllAttendance();

    List<OfficerAttendance> getAttendanceByOfficer(String officerId);

    List<OfficerAttendance> getAttendanceByOfficerAndDateRange(String officerId, LocalDateTime start, LocalDateTime end);

    Optional<OfficerAttendance> getCurrentShift(String officerId);

    /**
     * Check-in standar (tanpa validasi geofencing).
     */
    OfficerAttendance checkIn(String officerId, BigDecimal latitude, BigDecimal longitude, String deviceInfo);

    /**
     * Check-in dengan validasi geofencing.
     * Melempar IllegalStateException jika koordinat di luar radius kerja.
     *
     * @param officerId    ID petugas
     * @param latitude     Latitude petugas saat check-in
     * @param longitude    Longitude petugas saat check-in
     * @param deviceInfo   Info perangkat
     * @param maxRadiusKm  Radius maksimum wilayah kerja (km)
     * @param centerLat    Latitude pusat kantor/wilayah kerja
     * @param centerLon    Longitude pusat kantor/wilayah kerja
     */
    OfficerAttendance checkInWithGeofence(String officerId,
                                          BigDecimal latitude, BigDecimal longitude,
                                          String deviceInfo,
                                          double maxRadiusKm,
                                          double centerLat, double centerLon);

    OfficerAttendance checkOut(String attendanceId);

    OfficerAttendance setBreak(String attendanceId);

    OfficerAttendance resumeFromBreak(String attendanceId);
}
