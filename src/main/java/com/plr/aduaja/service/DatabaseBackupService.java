package com.plr.aduaja.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatabaseBackupService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseBackupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String getNewAccessToken() throws Exception {
        String appKey = System.getenv("DROPBOX_APP_KEY");
        String appSecret = System.getenv("DROPBOX_APP_SECRET");
        String refreshToken = System.getenv("DROPBOX_REFRESH_TOKEN");

        if (appKey == null || appSecret == null || refreshToken == null) {
            throw new RuntimeException("Kredensial Dropbox di Environment Variables tidak lengkap!");
        }

        String auth = appKey + ":" + appSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String encodedRefreshToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        String requestBody = "grant_type=refresh_token&refresh_token=" + encodedRefreshToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.dropbox.com/oauth2/token"))
                .header("Authorization", "Basic " + encodedAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gagal mendapat akses token: " + response.body());
        }

        Pattern pattern = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response.body());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new RuntimeException("Token tidak ditemukan!");
        }
    }

    @Scheduled(fixedRate = 1800000)
    public void backupDatabase() {
        try {
            // Cek apakah database utama ada
            Path dbPath = Paths.get("data/aduaja.mv.db");
            if (!Files.exists(dbPath)) {
                System.out.println("Database belum ada, melewati backup.");
                return;
            }

            // 1. Suruh H2 membuat file ZIP secara aman (bebas lock)
            String zipFilePath = "data/aduaja_backup.zip";
            jdbcTemplate.execute("BACKUP TO '" + zipFilePath + "'");

            Path zipPath = Paths.get(zipFilePath);

            // 2. Upload file ZIP tersebut ke Dropbox
            String newAccessToken = getNewAccessToken();
            byte[] fileBytes = Files.readAllBytes(zipPath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://content.dropboxapi.com/2/files/upload"))
                    // Perhatikan format nama di Dropbox sekarang adalah .zip
                    .header("Authorization", "Bearer " + newAccessToken)
                    .header("Dropbox-API-Arg", "{\"path\": \"/aduaja_backup.zip\",\"mode\": \"overwrite\",\"autorename\": false,\"mute\": false}")
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Status Backup Dropbox: BERHASIL MENGAMANKAN DATA.");
            } else {
                System.err.println("Gagal upload backup: " + response.body());
            }

            // 3. Hapus file ZIP lokal agar disk tidak penuh
            Files.deleteIfExists(zipPath);

        } catch (Exception e) {
            System.err.println("Sistem Backup Error: " + e.getMessage());
        }
    }
}