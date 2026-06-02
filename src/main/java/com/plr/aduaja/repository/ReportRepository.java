package com.plr.aduaja.repository;

import com.plr.aduaja.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {

    Optional<Report> findByTicketNumber(String ticketNumber);

    List<Report> findByStatus(Report.ReportStatus status);

    // findByReporterUserId — dipakai service lama
    List<Report> findByReporterUserId(String reporterId);

    // Ordered variant untuk service baru
    List<Report> findByReporterUserIdOrderBySubmittedAtDesc(String reporterId);

    List<Report> findByStatusOrderBySubmittedAtDesc(Report.ReportStatus status);

    List<Report> findAllByOrderBySubmittedAtDesc();

    List<Report> findByDescriptionContainingIgnoreCase(String description);

    List<Report> findByDescriptionContainingIgnoreCaseOrLocationHintContainingIgnoreCase(
            String description, String locationHint);

    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.submittedAt BETWEEN :start AND :end")
    List<Report> findByStatusAndDateRange(
            @Param("status") Report.ReportStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT r FROM Report r WHERE r.submittedAt BETWEEN :start AND :end")
    List<Report> findByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Alias for backward compatibility with old service
    default List<Report> findByStatusAndSubmittedAtBetween(Report.ReportStatus status,
                                                            LocalDateTime start, LocalDateTime end) {
        return findByStatusAndDateRange(status, start, end);
    }

    long countByStatus(Report.ReportStatus status);

    long countByReporterUserId(String reporterId);

    List<Report> findByParentReportIsNull();

    List<Report> findByParentReportReportId(String parentReportId);

    long countByParentReportReportId(String parentReportId);

    List<Report> findByRegionRegionId(String regionId);

    List<Report> findByStatusAndRegionRegionIdOrderBySubmittedAtDesc(Report.ReportStatus status, String regionId);

    long countByStatusAndRegionRegionId(Report.ReportStatus status, String regionId);

    List<Report> findByCategoryCategoryId(String categoryId);

    @Query("SELECT r FROM Report r WHERE r.category.categoryId = :categoryId " +
           "AND r.latitude BETWEEN :minLat AND :maxLat " +
           "AND r.longitude BETWEEN :minLng AND :maxLng")
    List<Report> findByCategoryAndCoordinateRange(
            @Param("categoryId") String categoryId,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng
    );
}
