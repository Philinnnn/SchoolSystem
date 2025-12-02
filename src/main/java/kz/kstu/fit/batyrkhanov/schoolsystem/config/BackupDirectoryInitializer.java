package kz.kstu.fit.batyrkhanov.schoolsystem.config;

import kz.kstu.fit.batyrkhanov.schoolsystem.service.BackupService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class BackupDirectoryInitializer implements CommandLineRunner {

    private final BackupService backupService;

    public BackupDirectoryInitializer(BackupService backupService) {
        this.backupService = backupService;
    }

    @Override
    public void run(String... args) {
        try {
            Path backupPath = Paths.get("backups");
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
                System.out.println("✓ Created backup directory: " + backupPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("⚠️ Failed to create backup directory: " + e.getMessage());
        }

        // Проверяем возможности бэкапа
        backupService.checkBackupCapabilities();
    }
}

