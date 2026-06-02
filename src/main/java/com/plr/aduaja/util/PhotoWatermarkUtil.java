package com.plr.aduaja.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * PhotoWatermarkUtil — Menambahkan watermark informatif pada foto bukti kerja.
 *
 * Sesuai Foto harus disegel dengan:
 *  - ID Tiket laporan
 *  - Timestamp server (waktu pengambilan foto versi server)
 *  - Koordinat GPS petugas saat mengambil foto
 *
 * Implementasi menggunakan Java AWT Graphics2D untuk menggambar teks langsung
 * pada gambar JPEG, tanpa dependency eksternal.
 */
public final class PhotoWatermarkUtil {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private PhotoWatermarkUtil() {}

    /**
     * Tambahkan watermark pada foto berformat base64 data URL.
     *
     * @param base64DataUrl   Foto original dalam format "data:image/jpeg;base64,..."
     * @param ticketNumber    Nomor tiket laporan (mis. "AJ-2026-00001")
     * @param officerName     Nama petugas
     * @param latitude        Koordinat GPS latitude petugas
     * @param longitude       Koordinat GPS longitude petugas
     * @param timestamp       Waktu server saat foto di-submit
     * @return base64 data URL dengan watermark terpasang, atau original jika gagal
     */
    public static String addWatermark(
            String base64DataUrl,
            String ticketNumber,
            String officerName,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime timestamp) {

        if (base64DataUrl == null || base64DataUrl.isBlank()) {
            return base64DataUrl;
        }

        try {
            // Ekstrak base64 content dari data URL
            String base64Content = extractBase64(base64DataUrl);
            if (base64Content == null) return base64DataUrl;

            byte[] imageBytes = Base64.getDecoder().decode(base64Content);
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null) return base64DataUrl;

            // Buat salinan image yang bisa digambar
            BufferedImage watermarked = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = watermarked.createGraphics();

            // Rendering hints untuk kualitas teks
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Gambar image original
            g2d.drawImage(originalImage, 0, 0, null);

            // Hitung ukuran font relatif terhadap lebar gambar
            int imgWidth  = originalImage.getWidth();
            int imgHeight = originalImage.getHeight();
            int fontSize  = Math.max(14, imgWidth / 55);

            // Baris teks watermark
            String line1 = "AduAja | Tiket: " + (ticketNumber != null ? ticketNumber : "N/A");
            String line2 = "Petugas: " + (officerName != null ? officerName : "N/A");
            String line3 = "GPS: " + (latitude  != null ? String.format("%.5f", latitude)  : "N/A")
                         + ", "   + (longitude != null ? String.format("%.5f", longitude) : "N/A");
            String line4 = "Waktu: " + (timestamp != null ? timestamp.format(DT_FMT) : LocalDateTime.now().format(DT_FMT));

            String[] lines = {line1, line2, line3, line4};

            Font font      = new Font("SansSerif", Font.BOLD, fontSize);
            FontMetrics fm = g2d.getFontMetrics(font);
            int lineHeight = fm.getHeight() + 4;
            int padding    = 12;
            int boxHeight  = lineHeight * lines.length + padding * 2;
            int boxWidth   = imgWidth; // full-width banner

            // Latar belakang hitam semi-transparan di bagian bawah gambar
            int boxY = imgHeight - boxHeight;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, boxY, boxWidth, boxHeight);

            // Reset komposit ke opaque untuk teks
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.setFont(font);

            for (int i = 0; i < lines.length; i++) {
                int textY = boxY + padding + (i + 1) * lineHeight - fm.getDescent();
                // Shadow
                g2d.setColor(Color.BLACK);
                g2d.drawString(lines[i], padding + 2, textY + 2);
                // Teks utama putih
                g2d.setColor(Color.WHITE);
                g2d.drawString(lines[i], padding, textY);
            }

            g2d.dispose();

            // Konversi kembali ke base64 JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(watermarked, "jpg", baos);
            String watermarkedBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());

            return "data:image/jpeg;base64," + watermarkedBase64;

        } catch (Exception e) {
            // Jika gagal watermark (mis. format tidak didukung), kembalikan original
            return base64DataUrl;
        }
    }

    /**
     * Ekstrak base64 content dari data URL.
     * Format: "data:image/jpeg;base64,<content>"
     */
    private static String extractBase64(String dataUrl) {
        int commaIdx = dataUrl.indexOf(',');
        if (commaIdx < 0) {
            // Mungkin sudah raw base64 tanpa prefix
            return dataUrl;
        }
        return dataUrl.substring(commaIdx + 1);
    }
}
