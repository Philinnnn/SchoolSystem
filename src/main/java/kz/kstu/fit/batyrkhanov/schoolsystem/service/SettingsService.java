package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.AppSetting;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SettingsService {
    private final AppSettingRepository repo;
    public SettingsService(AppSettingRepository repo) { this.repo = repo; }

    public Optional<String> get(String key) { return repo.findById(key).map(AppSetting::getValue); }
    public Optional<Long> getLong(String key) {
        try { return get(key).map(Long::parseLong); } catch (Exception e) { return Optional.empty(); }
    }

    @Transactional
    public void set(String key, String value) { repo.save(new AppSetting(key, value)); }
}

