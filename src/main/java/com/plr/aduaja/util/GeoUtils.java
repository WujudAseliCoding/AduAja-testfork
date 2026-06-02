package com.plr.aduaja.util;

import java.math.BigDecimal;

/**
 * GeoUtils — Utilitas kalkulasi jarak geografis (Haversine formula).
 *
 * Digunakan untuk:
 *  - Validasi geofencing check-in
 *  - Validasi jarak petugas ke lokasi tugas sebelum startTask
 */
public final class GeoUtils {

    // Radius bumi dalam kilometer
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {}

    /**
     * Hitung jarak antara dua titik koordinat menggunakan formula Haversine.
     *
     * @param lat1 Latitude titik 1 (derajat)
     * @param lon1 Longitude titik 1 (derajat)
     * @param lat2 Latitude titik 2 (derajat)
     * @param lon2 Longitude titik 2 (derajat)
     * @return Jarak dalam kilometer
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Overload dengan BigDecimal (untuk kompatibilitas dengan model JPA).
     */
    public static double haversineKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE; // Tidak bisa hitung jarak, anggap sangat jauh
        }
        return haversineKm(lat1.doubleValue(), lon1.doubleValue(),
                           lat2.doubleValue(), lon2.doubleValue());
    }

    /**
     * Cek apakah koordinat (lat, lon) berada dalam radius tertentu dari titik pusat.
     *
     * @param centerLat  Latitude pusat (derajat)
     * @param centerLon  Longitude pusat (derajat)
     * @param pointLat   Latitude titik yang dicek
     * @param pointLon   Longitude titik yang dicek
     * @param radiusKm   Radius maksimum (km)
     * @return true jika dalam radius
     */
    public static boolean isWithinRadius(double centerLat, double centerLon,
                                         double pointLat, double pointLon,
                                         double radiusKm) {
        return haversineKm(centerLat, centerLon, pointLat, pointLon) <= radiusKm;
    }

    /**
     * Overload BigDecimal untuk isWithinRadius.
     */
    public static boolean isWithinRadius(BigDecimal centerLat, BigDecimal centerLon,
                                          BigDecimal pointLat, BigDecimal pointLon,
                                          double radiusKm) {
        double dist = haversineKm(centerLat, centerLon, pointLat, pointLon);
        return dist <= radiusKm;
    }

    /**
     * Format jarak ke string yang ramah pengguna.
     *
     * @param km Jarak dalam kilometer
     * @return String misal "1.2 km" atau "500 m"
     */
    public static String formatDistance(double km) {
        if (km >= 1.0) {
            return String.format("%.1f km", km);
        } else {
            return String.format("%d m", (int) (km * 1000));
        }
    }
}
