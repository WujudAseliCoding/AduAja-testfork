package com.plr.aduaja.controller;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.Report.ReportStatus;
import com.plr.aduaja.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReportById(@PathVariable String id) {
        Optional<Report> report = reportService.getReportById(id);
        return report.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ticket/{ticketNumber}")
    public ResponseEntity<Report> getReportByTicketNumber(@PathVariable String ticketNumber) {
        Optional<Report> report = reportService.getReportByTicketNumber(ticketNumber);
        return report.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Report>> getReportsByStatus(@PathVariable ReportStatus status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Report>> getReportsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(reportService.getReportsByUser(userId));
    }

    @GetMapping("/disposisi-ready")
    public ResponseEntity<List<Report>> getReportsForDisposisi() {
        return ResponseEntity.ok(reportService.getReportsForDisposisi());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Report>> searchReports(@RequestParam String q) {
        return ResponseEntity.ok(reportService.searchReports(q));
    }

    @PostMapping
    public ResponseEntity<Report> createReport(@RequestBody Report report,
                                                @RequestParam String userId) {
        try {
            Report created = reportService.createReport(report, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Gagal buat report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Report> updateStatus(@PathVariable String id,
                                                @RequestParam ReportStatus status) {
        try {
            Report report = reportService.updateStatus(id, status);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Gagal update status report {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getReportCounts() {
        Map<String, Long> counts = Map.of(
                "menunggu_verifikasi", reportService.countByStatus(ReportStatus.MENUNGGU_VERIFIKASI),
                "diterima", reportService.countByStatus(ReportStatus.DITERIMA),
                "ditugaskan", reportService.countByStatus(ReportStatus.DITUGASKAN),
                "selesai", reportService.countByStatus(ReportStatus.SELESAI),
                "sengketa", reportService.countByStatus(ReportStatus.SENGKETA)
        );
        return ResponseEntity.ok(counts);
    }
}
