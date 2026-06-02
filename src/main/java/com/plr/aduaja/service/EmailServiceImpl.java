package com.plr.aduaja.service;

import com.plr.aduaja.model.OtpVerification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    // KUNCI API BREVO DITAMBAHKAN DI SINI
    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Override
    @Async("taskExecutor")
    public void sendOtpEmail(String to, String otpCode, OtpVerification.OtpType type) {
        String subject;
        String purpose;
        String icon;

        if (type == OtpVerification.OtpType.REGISTRATION) {
            subject = "AduAja - Verifikasi Akun Anda";
            purpose = "Verifikasi Pendaftaran Akun";
            icon = "🔐";
        } else if (type == OtpVerification.OtpType.FORGOT_PASSWORD) {
            subject = "AduAja - Reset Password Anda";
            purpose = "Reset Password";
            icon = "🔑";
        } else if (type == OtpVerification.OtpType.LOGIN) {
            subject = "AduAja - Kode Login Anda";
            purpose = "Verifikasi Login";
            icon = "🔓";
        } else {
            subject = "AduAja - Kode OTP Anda";
            purpose = "Verifikasi";
            icon = "📋";
        }

        String html = buildOtpEmailHtml(otpCode, purpose, icon);
        sendEmail(to, subject, html);
    }

    // FUNGSI INI DIROMBAK UNTUK MENGGUNAKAN BREVO HTTP API (ANTI BLOKIR HF)
    @Async("taskExecutor")
    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            Map<String, Object> senderInfo = new HashMap<>();
            senderInfo.put("name", "Sistem AduAja");
            senderInfo.put("email", fromEmail);

            Map<String, String> recipientInfo = new HashMap<>();
            recipientInfo.put("email", to);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sender", senderInfo);
            requestBody.put("to", List.of(recipientInfo));
            requestBody.put("subject", subject);
            requestBody.put("htmlContent", htmlBody);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("Email OTP via Brevo API berhasil dikirim ke {}", to);
        } catch (Exception e) {
            log.error("Gagal mengirim email via Brevo API ke {}: {}", to, e.getMessage(), e);
        }
    }

    // TEMPLATE HTML ASLI MILIK ANDA TETAP DIPERTAHANKAN
    private String buildOtpEmailHtml(String otpCode, String purpose, String icon) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:'Segoe UI',Arial,sans-serif;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f3f4f6;padding:40px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;">
                                <!-- Header -->
                                <tr>
                                    <td style="background:linear-gradient(135deg,#1e40af,#3b82f6);padding:40px 30px;text-align:center;">
                                        <div style="font-size:48px;margin-bottom:8px;">%s</div>
                                        <h1 style="color:#ffffff;font-size:24px;margin:12px 0 4px;font-weight:700;">AduAja</h1>
                                        <p style="color:#bfdbfe;font-size:14px;margin:0;">Sistem Pengaduan Infrastruktur Publik Kota Medan</p>
                                    </td>
                                </tr>
                                <!-- Body -->
                                <tr>
                                    <td style="padding:40px 30px;">
                                        <h2 style="color:#111827;font-size:20px;margin:0 0 8px;font-weight:600;">%s</h2>
                                        <p style="color:#6b7280;font-size:15px;line-height:1.6;margin:0 0 24px;">
                                            Gunakan kode OTP di bawah ini untuk melanjutkan proses verifikasi Anda.
                                            Kode ini berlaku selama <strong style="color:#1e40af;">10 menit</strong>.
                                        </p>
                                        <!-- OTP Box -->
                                        <div style="background:#f0f5ff;border:2px dashed #3b82f6;border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;">
                                            <p style="color:#6b7280;font-size:13px;margin:0 0 8px;text-transform:uppercase;letter-spacing:2px;">Kode OTP Anda</p>
                                            <div style="font-size:40px;font-weight:800;letter-spacing:12px;color:#1e40af;font-family:'Courier New',monospace;margin:8px 0;">%s</div>
                                        </div>
                                        <!-- Alert -->
                                        <div style="background:#fef3c7;border-left:4px solid #f59e0b;border-radius:8px;padding:16px 20px;margin-bottom:24px;">
                                            <p style="color:#92400e;font-size:13px;margin:0;line-height:1.5;">
                                                <strong>⚠️ Perhatian!</strong> Jangan bagikan kode OTP ini kepada siapa pun,
                                                termasuk pihak yang mengaku dari AduAja. Kami tidak akan pernah meminta kode OTP Anda.
                                            </p>
                                        </div>
                                        <p style="color:#9ca3af;font-size:13px;margin:0;line-height:1.5;">
                                            Jika Anda tidak merasa melakukan permintaan ini, abaikan email ini
                                            atau hubungi kami di <a href="mailto:support@aduaja.go.id" style="color:#3b82f6;text-decoration:none;">support@aduaja.go.id</a>.
                                        </p>
                                    </td>
                                </tr>
                                <!-- Footer -->
                                <tr>
                                    <td style="background:#f9fafb;padding:24px 30px;text-align:center;border-top:1px solid #e5e7eb;">
                                        <p style="color:#9ca3af;font-size:12px;margin:0 0 4px;">&copy; 2026 AduAja &mdash; Kota Medan</p>
                                        <p style="color:#9ca3af;font-size:12px;margin:0;">Email ini dikirim otomatis, jangan membalas email ini.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(icon, purpose, otpCode);
    }
}