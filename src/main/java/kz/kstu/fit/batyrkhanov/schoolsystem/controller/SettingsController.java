package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.config.TelegramRuntimeConfig;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.SettingsService;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {
    private final SettingsService settings;
    private final TelegramRuntimeConfig tgConfig;
    private final AuditService auditService;

    public SettingsController(SettingsService settings, TelegramRuntimeConfig tgConfig, AuditService auditService) {
        this.settings = settings;
        this.tgConfig = tgConfig;
        this.auditService = auditService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("tgUsername", tgConfig.getUsername());
        model.addAttribute("tgEnabled", tgConfig.isEnabled());
        model.addAttribute("tgLoginTtl", settings.get("telegram_login_token_ttl_minutes").orElse(String.valueOf(tgConfig.getLoginTokenTtlMinutes())));
        model.addAttribute("saved", false);
        return "admin/settings";
    }

    @PostMapping
    public String save(@RequestParam(name = "tgLoginTtl", required = false) String ttl,
                       HttpServletRequest request,
                       Model model) {
        if (StringUtils.hasText(ttl)) settings.set("telegram_login_token_ttl_minutes", ttl.trim());
        auditService.log("SETTINGS_UPDATE", "admin", "Updated telegram_login_token_ttl_minutes=" + ttl, request);
        model.addAttribute("tgUsername", tgConfig.getUsername());
        model.addAttribute("tgEnabled", tgConfig.isEnabled());
        model.addAttribute("tgLoginTtl", settings.get("telegram_login_token_ttl_minutes").orElse(String.valueOf(tgConfig.getLoginTokenTtlMinutes())));
        model.addAttribute("saved", true);
        return "admin/settings";
    }
}

