package com.plr.aduaja.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DatabaseStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupRecovery.class);
    private static final String H2_DB_PATH = resolveDbPath();
    private static final Path DATA_DIR = Paths.get(H2_DB_PATH).getParent();
    private static final String DB_BASE_NAME = Paths.get(H2_DB_PATH).getFileName().toString();
    private static final String JDBC_URL = "jdbc:h2:file:" + H2_DB_PATH + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private DatabaseStartupRecovery() {
    }

    private static String resolveDbPath() {
        String envPath = System.getenv("H2_DB_PATH");
        return (envPath != null && !envPath.isBlank()) ? envPath : "./data/aduaja";
    }

    public static void prepareDatabase() {
        String profiles = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profiles != null && profiles.contains("prod")) {
            log.info("Profile 'prod' terdeteksi — H2 recovery dilewati.");
            return;
        }
        try {
            if (DATA_DIR != null) {
                Files.createDirectories(DATA_DIR);
            }
            verifyConnection();
        } catch (SQLException ex) {
            if (!isCorruptionError(ex)) {
                throw new IllegalStateException("Gagal membuka database H2: " + ex.getMessage(), ex);
            }

            log.warn("Database H2 terdeteksi korup. Membuat backup lalu membangun database baru.");
            backupCorruptedDatabaseFiles();
            try {
                verifyConnection();
            } catch (SQLException recoveryException) {
                throw new IllegalStateException("Database baru gagal diinisialisasi setelah recovery.", recoveryException);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Gagal menyiapkan folder database.", ex);
        }
    }

    static boolean isCorruptionError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                if (sqlException.getErrorCode() == 90030 || "90030".equals(sqlException.getSQLState())) {
                    return true;
                }
                String message = sqlException.getMessage();
                if (message != null && message.contains("File corrupted while reading record")) {
                    return true;
                }
                if (message != null && message.contains("File is corrupted - unable to recover a valid set of chunks")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static void verifyConnection() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {
            // Driver sudah tersedia dari dependency H2, tetapi tetap aman jika loader berubah.
        }

        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            // Membuka dan menutup koneksi untuk memastikan file database bisa digunakan.
        }
    }

    private static void backupCorruptedDatabaseFiles() {
        Path backupDir = DATA_DIR.resolve("recovery-backup");
        try {
            Files.createDirectories(backupDir);
            moveIfExists(DATA_DIR.resolve(DB_BASE_NAME + ".mv.db"), backupDir);
            moveIfExists(DATA_DIR.resolve(DB_BASE_NAME + ".trace.db"), backupDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Gagal memindahkan file database korup ke folder backup.", ex);
        }
    }

    private static void moveIfExists(Path source, Path backupDir) throws IOException {
        if (!Files.exists(source)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP);
        String baseName = source.getFileName().toString().replace(".db", "");
        Path target = backupDir.resolve(baseName + ".corrupt-" + timestamp + ".db");
        int counter = 1;
        while (Files.exists(target)) {
            target = backupDir.resolve(baseName + ".corrupt-" + timestamp + "-" + counter++ + ".db");
        }

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Backup database korup: {} -> {}", source, target);
    }
}



