package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.config.TelegramRuntimeConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final TelegramRuntimeConfig telegramRuntimeConfig;

    public HomeController(TelegramRuntimeConfig telegramRuntimeConfig) {
        this.telegramRuntimeConfig = telegramRuntimeConfig;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("telegramBotUsername", telegramRuntimeConfig.getUsername());
        return "login";
    }
}