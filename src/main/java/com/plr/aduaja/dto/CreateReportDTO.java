package com.plr.aduaja.dto;

import java.math.BigDecimal;

// ============================================================
// ENCAPSULATION (Enkapsulasi): DTO memisahkan form input dari Entity
// Semua field PRIVATE — Controller tidak langsung pakai Entity
// ============================================================
public class CreateReportDTO {

    // ENKAPSULASI: semua field PRIVATE
    private String description;
    private String locationHint;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String photoBase64;
    private String categoryId;
    private String regionId;
    private String photoTakenAt;  // ISO-8601 string from EXIF or client timestamp

    // ENKAPSULASI: Hanya getter & setter
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocationHint() { return locationHint; }
    public void setLocationHint(String locationHint) { this.locationHint = locationHint; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }

    public String getPhotoTakenAt() { return photoTakenAt; }
    public void setPhotoTakenAt(String photoTakenAt) { this.photoTakenAt = photoTakenAt; }
}
