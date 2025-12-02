package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class BackupService {

    private static final String BACKUP_DIR = "backups";
    private static final Path INDEX_PATH = Paths.get(BACKUP_DIR, "index.json");

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

    /**
     * Проверяет доступность функций бэкапа (вызывается при старте)
     */
    public void checkBackupCapabilities() {
        System.out.println("\n=== Checking Backup Capabilities ===");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Сначала включаем отображение расширенных опций
            stmt.execute("EXEC sp_configure 'show advanced options', 1");
            stmt.execute("RECONFIGURE");

            // Теперь проверяем xp_cmdshell
            try (ResultSet rs = stmt.executeQuery("EXEC sp_configure 'xp_cmdshell'")) {
                while (rs.next()) {
                    int runValue = rs.getInt("run_value");
                    if (runValue == 1) {
                        System.out.println("✓ xp_cmdshell is enabled - backup operations available");
                    } else {
                        System.out.println("⚠️  xp_cmdshell is DISABLED - backup size/download/delete will not work");
                        System.out.println("   To enable, run on SQL Server:");
                        System.out.println("   EXEC sp_configure 'show advanced options', 1; RECONFIGURE;");
                        System.out.println("   EXEC sp_configure 'xp_cmdshell', 1; RECONFIGURE;");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️  Could not check xp_cmdshell status: " + e.getMessage());
            System.err.println("   This may indicate insufficient permissions or SQL Server version issues");
        }
        System.out.println("=== End of Backup Capabilities Check ===\n");
    }


    public String createBackup() throws Exception {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        String backupFileName = "backup_" + timestamp + ".bak";

        String dbName = extractDatabaseName(dbUrl);
        if (dbName == null) {
            throw new IllegalArgumentException("Cannot extract database name from URL: " + dbUrl);
        }

        Files.createDirectories(Paths.get(BACKUP_DIR));
        String serverBackupPath = Paths.get(serverBackupDir, backupFileName).toString();

        System.out.println("Creating backup on server at: " + serverBackupPath);

        // Делаем BACKUP DATABASE на сервере БД
        String backupSql = String.format(
                "BACKUP DATABASE [%s] TO DISK = N'%s' WITH FORMAT, INIT, NAME = N'%s-Full Database Backup'",
                dbName, serverBackupPath.replace("\\", "\\\\"), dbName
        );
        System.out.println("Executing SQL: " + backupSql);
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(backupSql);
        }

        // Получаем размер файла через SQL Server
        long fileSize = 0L;
        try {
            fileSize = getFileSizeViaSQL(serverBackupPath);
            System.out.println("Backup file size via SQL: " + fileSize + " bytes");
        } catch (Exception e) {
            System.err.println("Could not read file size via SQL: " + e.getMessage());
            e.printStackTrace();
        }

        // Записываем в индекс серверный путь (не UNC!) для корректной работы SQL операций
        addToIndex(backupFileName, serverBackupPath, System.currentTimeMillis(), fileSize);
        System.out.println("Backup created and indexed: " + serverBackupPath + " (size: " + fileSize + " bytes)");
        return serverBackupPath;
    }

    public boolean backupExists(String filename) {
        Path local = Paths.get(BACKUP_DIR, filename);
        if (Files.exists(local)) return true;
        BackupInfo idx = findInIndex(filename);
        if (idx == null) return false;
        try {
            return Files.exists(Paths.get(idx.path));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteBackupAny(String filename) {
        boolean deleted = false;

        // Пытаемся удалить локально
        try {
            deleteBackup(filename);
            deleted = true;
        } catch (Exception e) {
            System.err.println("Local delete failed: " + e.getMessage());
        }

        // Если есть запись в индексе — удаляем по её пути
        BackupInfo idx = findInIndex(filename);
        if (idx != null) {
            String path = idx.path;
            System.out.println("Found in index: " + path);

            // Если путь содержит серверную директорию - удаляем через SQL
            if (path.contains(serverBackupDir)) {
                System.out.println("Deleting remote backup via SQL: " + path);
                if (deleteFileViaSQL(path)) {
                    deleted = true;
                    System.out.println("✓ Deleted backup via SQL");
                } else {
                    System.err.println("✗ Failed to delete via SQL");
                }
            } else {
                // Локальный файл
                System.out.println("Deleting local backup: " + path);
                try {
                    Path p = Paths.get(path);
                    if (Files.exists(p)) {
                        Files.delete(p);
                        deleted = true;
                        System.out.println("✓ Deleted local file");
                    } else {
                        System.err.println("✗ File not found: " + path);
                    }
                } catch (Exception e) {
                    System.err.println("✗ Delete failed: " + e.getMessage());
                }
            }

            // Удаляем из индекса в любом случае
            try {
                List<BackupInfo> list = readIndex();
                list.removeIf(b -> Objects.equals(b.filename, filename));
                writeIndex(list);
            } catch (Exception e) {
                System.err.println("Index remove failed: " + e.getMessage());
            }

            return deleted;
        }

        return deleted;
    }

    public List<BackupInfo> listBackups() {
        Path backupPath = Paths.get(BACKUP_DIR);
        System.out.println("Checking backup directory: " + backupPath.toAbsolutePath());

        List<BackupInfo> result = new ArrayList<>();

        // Локальные .bak
        if (Files.exists(backupPath)) {
            try (var stream = Files.list(backupPath)) {
                stream
                    .peek(p -> System.out.println("Found local file: " + p))
                    .filter(p -> p.toString().endsWith(".bak"))
                    .forEach(p -> {
                        try {
                            result.add(new BackupInfo(
                                p.getFileName().toString(),
                                Files.size(p),
                                Files.getLastModifiedTime(p).toMillis(),
                                p.toAbsolutePath().toString()
                            ));
                        } catch (IOException e) {
                            System.err.println("Error reading local file " + p + ": " + e.getMessage());
                        }
                    });
            } catch (IOException e) {
                System.err.println("Error listing local backups: " + e.getMessage());
            }
        }

        // Записи из локального индекса (включая серверные пути, без попытки листинга UNC)
        try {
            for (BackupInfo info : readIndex()) {
                boolean existsLocally = result.stream().anyMatch(b -> Objects.equals(b.filename, info.filename));
                if (!existsLocally) {
                    result.add(info);
                }
            }
        } catch (Exception e) {
            System.err.println("Index read failed: " + e.getMessage());
        }

        // Сортировка по времени (новые сверху)
        result.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        System.out.println("Total backups found (local+UNC+index): " + result.size());
        return result;
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

    private synchronized void addToIndex(String filename, String path, long timestamp, long size) {
        try {
            List<BackupInfo> idx = readIndex();
            // обновить/добавить
            idx.removeIf(b -> Objects.equals(b.filename, filename));
            idx.add(new BackupInfo(filename, size, timestamp, path));
            writeIndex(idx);
        } catch (Exception e) {
            System.err.println("Add to index failed: " + e.getMessage());
        }
    }

    private List<BackupInfo> readIndex() throws IOException {
        if (!Files.exists(INDEX_PATH)) return new ArrayList<>();
        String json = Files.readString(INDEX_PATH);
        // Простой формат: одна запись на строку: filename\t size\t timestamp\t path
        List<BackupInfo> list = new ArrayList<>();
        for (String line : json.split("\r?\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\t", 4);
            if (parts.length == 4) {
                try {
                    list.add(new BackupInfo(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]), parts[3]));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private void writeIndex(List<BackupInfo> items) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (BackupInfo b : items) {
            sb.append(b.filename).append('\t').append(b.size).append('\t').append(b.timestamp).append('\t').append(b.path).append('\n');
        }
        Files.createDirectories(INDEX_PATH.getParent());
        Files.writeString(INDEX_PATH, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public byte[] readBackup(String filename) throws IOException {
        // Сначала пробуем локально
        Path local = Paths.get(BACKUP_DIR, filename);
        if (Files.exists(local)) {
            return Files.readAllBytes(local);
        }

        // Иначе ищем в индексе
        BackupInfo idx = findInIndex(filename);
        if (idx != null) {
            String path = idx.path;

            // Если это UNC путь или путь на удаленном сервере, пробуем читать через SQL
            if (path.contains(serverBackupDir) || path.startsWith("\\\\")) {
                // Преобразуем UNC путь обратно в локальный путь сервера
                String serverPath = path;
                if (path.startsWith("\\\\")) {
                    // UNC путь: \\host\share\file -> C:\Backups\file
                    String[] parts = path.split("\\\\");
                    if (parts.length > 0) {
                        String fileName = parts[parts.length - 1];
                        serverPath = Paths.get(serverBackupDir, fileName).toString();
                    }
                }

                try {
                    byte[] data = readFileViaSQL(serverPath);
                    // Если размер был 0, обновим индекс фактическим размером
                    if (idx.size == 0 && data.length > 0) {
                        addToIndex(filename, path, idx.timestamp, data.length);
                    }
                    return data;
                } catch (Exception e) {
                    System.err.println("SQL read failed for " + serverPath + ": " + e.getMessage());
                    throw new FileNotFoundException("Backup not accessible via SQL: " + filename);
                }
            }

            // Пробуем читать как обычный файл
            try {
                Path p = Paths.get(path);
                byte[] data = Files.readAllBytes(p);
                // если размер был 0, обновим индекс фактическим размером и временем
                if (idx.size == 0) {
                    addToIndex(filename, path, System.currentTimeMillis(), data.length);
                }
                return data;
            } catch (Exception e) {
                System.err.println("File read failed for " + path + ": " + e.getMessage());
                throw new FileNotFoundException("Backup not accessible: " + filename);
            }
        }
        throw new FileNotFoundException("Backup not found: " + filename);
    }

    private BackupInfo findInIndex(String filename) {
        try {
            for (BackupInfo b : readIndex()) {
                if (Objects.equals(b.filename, filename)) return b;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Получает размер файла через SQL Server используя DATALENGTH
     */
    private long getFileSizeViaSQL(String filePath) {
        try (Connection conn = dataSource.getConnection()) {
            // Используем DATALENGTH + OPENROWSET - самый надежный способ
            String sql = "SELECT DATALENGTH(BulkColumn) AS FileSize FROM OPENROWSET(BULK N'" + filePath + "', SINGLE_BLOB) AS FileData";

            System.out.println("Getting file size via DATALENGTH: " + filePath);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    long size = rs.getLong("FileSize");
                    System.out.println("✓ File size: " + size + " bytes");
                    return size;
                }
            }

        } catch (SQLException e) {
            System.err.println("SQL error getting file size: " + e.getMessage());
            System.err.println("Make sure the SQL Server service account has read access to: " + filePath);
        }
        return 0L;
    }

    /**
     * Удаляет файл на SQL Server используя xp_cmdshell
     */
    private boolean deleteFileViaSQL(String filePath) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "EXEC xp_cmdshell 'del /F \"" + filePath + "\"'";
            System.out.println("Deleting file: " + sql);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                boolean hasOutput = false;
                while (rs.next()) {
                    String line = rs.getString(1);
                    if (line != null) {
                        System.out.println("Delete output: " + line);
                        hasOutput = true;
                    }
                }
                System.out.println("File delete command executed: " + filePath);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("SQL error deleting file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Читает содержимое файла через SQL Server используя OPENROWSET
     */
    private byte[] readFileViaSQL(String filePath) throws IOException {
        try (Connection conn = dataSource.getConnection()) {
            // Используем OPENROWSET для чтения файла как BLOB
            String sql = "SELECT BulkColumn FROM OPENROWSET(BULK N'" + filePath + "', SINGLE_BLOB) AS FileData";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    byte[] data = rs.getBytes(1);
                    System.out.println("Read file via SQL: " + filePath + ", size: " + data.length);
                    return data;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error reading file: " + e.getMessage());
            throw new IOException("Failed to read file via SQL: " + e.getMessage(), e);
        }
        throw new IOException("File not found or empty: " + filePath);
    }
}
