package com.plr.aduaja.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "confirmation_requests")
@JsonIgnoreProperties({"report"})
public class ConfirmationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "confirmation_id")
    private String confirmationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warga_id", nullable = false)
    private User warga;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "response")
    private ResponseType response;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    public enum ResponseType {
        TERIMA, TOLAK, TIMEOUT
    }

    public String getConfirmationId() { return confirmationId; }
    public void setConfirmationId(String confirmationId) { this.confirmationId = confirmationId; }

    public Report getReport() { return report; }
    public void setReport(Report report) { this.report = report; }

    public User getWarga() { return warga; }
    public void setWarga(User warga) { this.warga = warga; }

    public LocalDateTime getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }

    public ResponseType getResponse() { return response; }
    public void setResponse(ResponseType response) { this.response = response; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }
}
