package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BackupService {

    private static final String BACKUP_DIR = "backups";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    // Серверный каталог, куда SQL Server пишет .bak (локальный для сервера БД)
    @Value("${backup.serverDir:C:\\Backups}")
    private String serverBackupDir;
    // Имя SMB-шары на сервере БД (например, "Backups"), чтобы прочитать файл через UNC
    @Value("${backup.serverShare:Backups}")
    private String serverShareName;

    private final DataSource dataSource;

    public BackupService(DataSource dataSource) {
        this.dataSource = dataSource;
        try {
            Files.createDirectories(Paths.get(BACKUP_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create backup directory: " + e.getMessage());
        }
    }


    public String createBackup() throws Exception {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        String backupFileName = "backup_" + timestamp + ".bak";

        String dbName = extractDatabaseName(dbUrl);
        if (dbName == null) {
            throw new IllegalArgumentException("Cannot extract database name from URL: " + dbUrl);
        }

        Files.createDirectories(Paths.get(BACKUP_DIR));
        Path localBackupPath = Paths.get(BACKUP_DIR, backupFileName).toAbsolutePath();
        String serverBackupPath = Paths.get(serverBackupDir, backupFileName).toString();

        System.out.println("Creating backup on server at: " + serverBackupPath);
        System.out.println("Will save locally to: " + localBackupPath);

        // Делаем BACKUP DATABASE на сервере БД
        String backupSql = String.format(
                "BACKUP DATABASE [%s] TO DISK = N'%s' WITH FORMAT, INIT, NAME = N'%s-Full Database Backup'",
                dbName, serverBackupPath.replace("\\", "\\\\"), dbName
        );
        System.out.println("Executing SQL: " + backupSql);
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(backupSql);
        }

        // Пытаемся прочитать файл через UNC: \\<db-host>\\<share>\\backupFileName
        String host = extractHost(dbUrl);
        if (host != null) {
            String unc = "\\\\" + host + "\\" + serverShareName + "\\" + backupFileName;
            Path uncPath = Paths.get(unc);
            try {
                System.out.println("Attempting to copy from UNC: " + unc);
                Files.copy(uncPath, localBackupPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Backup saved locally: " + localBackupPath);
                return localBackupPath.toString();
            } catch (Exception e) {
                System.err.println("UNC copy failed: " + e.getMessage());
                // Возвращаем серверный путь, чтобы админ мог забрать файл вручную
                return serverBackupPath;
            }
        }
        // Если не удалось определить хост — возвращаем серверный путь
        return serverBackupPath;
    }

    public List<BackupInfo> listBackups() {
        Path backupPath = Paths.get(BACKUP_DIR);
        System.out.println("Checking backup directory: " + backupPath.toAbsolutePath());

        if (!Files.exists(backupPath)) {
            System.out.println("Backup directory does not exist!");
            return Collections.emptyList();
        }

        try (var stream = Files.list(backupPath)) {
            List<BackupInfo> backups = stream
                .peek(p -> System.out.println("Found file: " + p.toString()))
                .filter(p -> {
                    boolean isBak = p.toString().endsWith(".bak");
                    System.out.println("File " + p.getFileName() + " is .bak: " + isBak);
                    return isBak;
                })
                .map(p -> {
                    try {
                        BackupInfo info = new BackupInfo(
                            p.getFileName().toString(),
                            Files.size(p),
                            Files.getLastModifiedTime(p).toMillis(),
                            p.toAbsolutePath().toString()
                        );
                        System.out.println("Created BackupInfo: " + info.filename);
                        return info;
                    } catch (IOException e) {
                        System.err.println("Error reading file " + p + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .collect(Collectors.toList());

            System.out.println("Total backups found: " + backups.size());
            return backups;
        } catch (IOException e) {
            System.err.println("Error listing backups: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public File getBackupFile(String filename) throws IOException {
        Path path = Paths.get(BACKUP_DIR, filename);
        if (!Files.exists(path) || !filename.endsWith(".bak")) {
            throw new FileNotFoundException("Backup file not found: " + filename);
        }
        return path.toFile();
    }

    public void deleteBackup(String filename) throws IOException {
        Path path = Paths.get(BACKUP_DIR, filename);
        if (!Files.exists(path) || !filename.endsWith(".bak")) {
            throw new FileNotFoundException("Backup file not found: " + filename);
        }
        Files.delete(path);
    }


    private String extractDatabaseName(String url) {
        int idx = url.indexOf("databaseName=");
        if (idx == -1) {
            idx = url.indexOf("database=");
            if (idx == -1) return null;
            idx += "database=".length();
        } else {
            idx += "databaseName=".length();
        }

        int endIdx = url.indexOf(";", idx);
        if (endIdx == -1) {
            return url.substring(idx);
        }
        return url.substring(idx, endIdx);
    }

    private String extractHost(String url) {
        try {
            String prefix = "jdbc:sqlserver://";
            int i = url.indexOf(prefix);
            if (i == -1) return null;
            int start = i + prefix.length();
            int end = url.indexOf(";", start);
            String hostPort = (end == -1 ? url.substring(start) : url.substring(start, end));
            int slash = hostPort.indexOf('\\'); // instance
            String hostOnly = (slash != -1 ? hostPort.substring(0, slash) : hostPort);
            int colon = hostOnly.indexOf(':');
            return (colon != -1 ? hostOnly.substring(0, colon) : hostOnly);
        } catch (Exception e) {
            return null;
        }
    }

    public static class BackupInfo {
        public String filename;
        public long size;
        public long timestamp;
        public String path;

        public BackupInfo(String filename, long size, long timestamp, String path) {
            this.filename = filename;
            this.size = size;
            this.timestamp = timestamp;
            this.path = path;
        }

        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }

        public String getFormattedDate() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
        }
    }
}