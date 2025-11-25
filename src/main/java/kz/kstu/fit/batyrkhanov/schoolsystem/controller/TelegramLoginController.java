package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.TelegramAuthService;
import kz.kstu.fit.batyrkhanov.schoolsystem.config.TelegramRuntimeConfig;
import kz.kstu.fit.batyrkhanov.schoolsystem.telegram.LoginRequestStore;
import kz.kstu.fit.batyrkhanov.schoolsystem.telegram.TelegramBot;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Controller
@RequestMapping("/login/telegram")
public class TelegramLoginController {

    private final UserRepository userRepository;
    private final TelegramAuthService telegramAuthService;
    private final UserDetailsService userDetailsService;
    private final TelegramRuntimeConfig runtimeConfig;
    private final LoginRequestStore loginRequestStore;
    private final TelegramBot telegramBot;
    private final AuditService auditService;

    public TelegramLoginController(UserRepository userRepository,
                                   TelegramAuthService telegramAuthService,
                                   UserDetailsService userDetailsService,
                                   TelegramRuntimeConfig runtimeConfig,
                                   LoginRequestStore loginRequestStore,
                                   TelegramBot telegramBot,
                                   AuditService auditService) {
        this.userRepository = userRepository;
        this.telegramAuthService = telegramAuthService;
        this.userDetailsService = userDetailsService;
        this.runtimeConfig = runtimeConfig;
        this.loginRequestStore = loginRequestStore;
        this.telegramBot = telegramBot;
        this.auditService = auditService;
    }

    @PostMapping("/token")
    public String loginByToken(@RequestParam("username") String username,
                               @RequestParam("code") String code,
                               Model model,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            prepareError(model, "Пользователь не найден");
            return "login";
        }
        if (!telegramAuthService.validateLoginToken(user, code)) {
            prepareError(model, "Код неверен или устарел");
            return "login";
        }
        // одноразовое потребление токена
        telegramAuthService.consumeLoginToken(user);

        // Загружаем детали и создаём аутентификацию
        UserDetails details = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                details, details.getPassword(), details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Явно сохраняем SecurityContext в сессию, чтобы он не потерялся на редиректе
        request.getSession(true);
        new HttpSessionSecurityContextRepository()
                .saveContext(SecurityContextHolder.getContext(), request, response);

        // Перенаправляем на дашборд; фильтр TOTP при необходимости отправит на ввод кода
        return "redirect:/dashboard";
    }

    @PostMapping("/start")
    public String startLoginViaTelegram(@RequestParam("username") String username, Model model) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            prepareError(model, "Пользователь не найден");
            return "login";
        }
        if (user.getTelegramId() == null) {
            prepareError(model, "К аккаунту не привязан Telegram. Используйте привязку в профиле.");
            return "login";
        }
        // создаём запрос (TTL 120 секунд)
        LoginRequestStore.LoginRequest lr = loginRequestStore.create(user.getId(), 120);
        // отправляем сообщение в Telegram с кнопками
        Integer msgId = telegramBot.sendLoginConfirmation(user.getTelegramId(), lr.getId(), username);
        if (msgId != null) {
            lr.setTelegramMessageId(msgId);
            lr.setChatId(user.getTelegramId());
        }
        model.addAttribute("requestId", lr.getId());
        model.addAttribute("username", username);
        auditService.log("TG_LOGIN_START", username, "Started Telegram login request=" + lr.getId());
        return "login_telegram_wait";
    }

    @GetMapping("/request/status")
    public ResponseEntity<?> requestStatus(@RequestParam("id") String requestId) {
        LoginRequestStore.LoginRequest lr = loginRequestStore.get(requestId);
        if (lr == null) return ResponseEntity.ok().body("{\"status\":\"NOT_FOUND\"}");
        return ResponseEntity.ok().body("{\"status\":\"" + lr.getStatus() + "\"}");
    }

    @PostMapping("/consume")
    public String consumeApproved(@RequestParam("id") String requestId, Model model,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        LoginRequestStore.LoginRequest lr = loginRequestStore.get(requestId);
        if (lr == null) {
            prepareError(model, "Запрос не найден");
            return "login";
        }
        if (lr.getStatus() != LoginRequestStore.Status.APPROVED) {
            prepareError(model, "Запрос не подтверждён (" + lr.getStatus() + ")");
            return "login";
        }
        // Аутентифицируем пользователя
        User user = userRepository.findById(lr.getUserId()).orElse(null);
        if (user == null) {
            prepareError(model, "Пользователь исчез");
            return "login";
        }
        UserDetails details = userDetailsService.loadUserByUsername(user.getUsername());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(details, details.getPassword(), details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // удаляем запрос из хранилища
        loginRequestStore.remove(requestId);

        // Явно сохраняем SecurityContext в сессию, чтобы он не потерялся на редиректе
        request.getSession(true);
        new HttpSessionSecurityContextRepository()
                .saveContext(SecurityContextHolder.getContext(), request, response);
        auditService.log("TG_LOGIN_CONSUME", user.getUsername(), "Telegram login consumed request=" + requestId, request);
        auditService.log("LOGIN", user.getUsername(), "Telegram login success", request);
        return "redirect:/dashboard";
    }

    private void prepareError(Model model, String message) {
        model.addAttribute("tgError", message);
        model.addAttribute("telegramBotUsername", runtimeConfig.getUsername());
    }
}
