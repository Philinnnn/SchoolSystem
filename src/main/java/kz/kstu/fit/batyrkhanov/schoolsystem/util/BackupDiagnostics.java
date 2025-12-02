package kz.kstu.fit.batyrkhanov.schoolsystem.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Утилита для тестирования и диагностики работы с бэкапами
 */
public class BackupDiagnostics {

    /**
     * Проверяет, включен ли xp_cmdshell на SQL Server
     */
    public static boolean isXpCmdshellEnabled(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXEC sp_configure 'xp_cmdshell'")) {

            while (rs.next()) {
                int runValue = rs.getInt("run_value");
                System.out.println("xp_cmdshell run_value: " + runValue);
                return runValue == 1;
            }
        } catch (Exception e) {
            System.err.println("Failed to check xp_cmdshell status: " + e.getMessage());
        }
        return false;
    }

    /**
     * Проверяет доступность каталога бэкапов через SQL Server
     */
    public static boolean testBackupDirectory(DataSource dataSource, String backupDir) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXEC xp_cmdshell 'dir \"" + backupDir + "\"'")) {

            System.out.println("Testing backup directory: " + backupDir);
            boolean hasContent = false;
            while (rs.next()) {
                String line = rs.getString(1);
                if (line != null && !line.trim().isEmpty()) {
                    System.out.println(line);
                    hasContent = true;
                }
            }
            return hasContent;
        } catch (Exception e) {
            System.err.println("Failed to test backup directory: " + e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет работу PowerShell через xp_cmdshell
     */
    public static boolean testPowerShell(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXEC xp_cmdshell 'powershell -Command \"Write-Output test\"'")) {

            while (rs.next()) {
                String line = rs.getString(1);
                if (line != null && line.contains("test")) {
                    System.out.println("PowerShell test successful");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to test PowerShell: " + e.getMessage());
        }
        return false;
    }

    /**
     * Проверяет работу OPENROWSET для чтения файлов
     */
    public static boolean testOpenRowset(DataSource dataSource, String testFilePath) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT BulkColumn FROM OPENROWSET(BULK N'" + testFilePath + "', SINGLE_BLOB) AS FileData";
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                byte[] data = rs.getBytes(1);
                System.out.println("OPENROWSET test successful, read " + data.length + " bytes");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to test OPENROWSET: " + e.getMessage());
        }
        return false;
    }

    /**
     * Запускает все диагностические тесты
     */
    public static void runAllTests(DataSource dataSource, String backupDir) {
        System.out.println("\n=== Backup Diagnostics ===\n");

        System.out.println("1. Checking xp_cmdshell status...");
        boolean xpEnabled = isXpCmdshellEnabled(dataSource);
        System.out.println("   Result: " + (xpEnabled ? "✓ ENABLED" : "✗ DISABLED"));

        if (xpEnabled) {
            System.out.println("\n2. Testing backup directory access...");
            boolean dirAccessible = testBackupDirectory(dataSource, backupDir);
            System.out.println("   Result: " + (dirAccessible ? "✓ ACCESSIBLE" : "✗ NOT ACCESSIBLE"));

            System.out.println("\n3. Testing PowerShell...");
            boolean psWorks = testPowerShell(dataSource);
            System.out.println("   Result: " + (psWorks ? "✓ WORKS" : "✗ FAILED"));
        } else {
            System.out.println("\n⚠️  xp_cmdshell is disabled. Backup operations will not work.");
            System.out.println("   To enable, run: EXEC sp_configure 'xp_cmdshell', 1; RECONFIGURE;");
        }

        System.out.println("\n=== End of Diagnostics ===\n");
    }
}

