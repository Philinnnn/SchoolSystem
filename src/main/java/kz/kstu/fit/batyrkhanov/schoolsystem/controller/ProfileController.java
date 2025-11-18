package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import jakarta.servlet.http.HttpSession;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.QrCodeService;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.TotpService;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final String SESSION_TOTP_SETUP_SECRET = "TOTP_SETUP_SECRET";

    private final UserService userService;
    private final UserRepository userRepository;
    private final TotpService totpService;
    private final QrCodeService qrCodeService;

    public ProfileController(UserService userService, UserRepository userRepository, TotpService totpService, QrCodeService qrCodeService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByUsername(principal.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("passwordChangeSuccess", null);
        model.addAttribute("passwordChangeError", null);
        model.addAttribute("totpEnabled", user != null && Boolean.TRUE.equals(user.getTotpEnabled()));
        return "profile/index";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        User user = userRepository.findByUsername(principal.getUsername());
        model.addAttribute("user", user);
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            model.addAttribute("passwordChangeError", "Новый пароль должен быть не короче 6 символов.");
            return "profile/index";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("passwordChangeError", "Пароли не совпадают.");
            return "profile/index";
        }
        boolean ok = userService.changePassword(principal.getUsername(), currentPassword, newPassword);
        if (ok) {
            model.addAttribute("passwordChangeSuccess", "Пароль успешно изменён.");
        } else {
            model.addAttribute("passwordChangeError", "Текущий пароль неверный.");
        }
        model.addAttribute("totpEnabled", user != null && Boolean.TRUE.equals(user.getTotpEnabled()));
        return "profile/index";
    }

    @GetMapping("/totp")
    public String totpSetup(@AuthenticationPrincipal UserDetails principal, HttpSession session, Model model) {
        User user = userRepository.findByUsername(principal.getUsername());
        if (user != null && Boolean.TRUE.equals(user.getTotpEnabled())) {
            model.addAttribute("alreadyEnabled", true);
            return "profile/totp_setup";
        }
        String secret = (String) session.getAttribute(SESSION_TOTP_SETUP_SECRET);
        if (!StringUtils.hasText(secret)) {
            secret = totpService.generateSecret();
            session.setAttribute(SESSION_TOTP_SETUP_SECRET, secret);
        }
        String otpauth = totpService.buildOtpAuthUrl("SchoolSystem", principal.getUsername(), secret);
        model.addAttribute("secret", secret);
        model.addAttribute("otpauth", otpauth);
        model.addAttribute("alreadyEnabled", false);
        model.addAttribute("error", null);
        return "profile/totp_setup";
    }

    @GetMapping("/totp/qr")
    @ResponseBody
    public ResponseEntity<byte[]> totpQr(HttpSession session, @AuthenticationPrincipal UserDetails principal) {
        String secret = (String) session.getAttribute(SESSION_TOTP_SETUP_SECRET);
        if (!StringUtils.hasText(secret)) {
            secret = totpService.generateSecret();
            session.setAttribute(SESSION_TOTP_SETUP_SECRET, secret);
        }
        String otpauth = totpService.buildOtpAuthUrl("SchoolSystem", principal.getUsername(), secret);
        byte[] png = qrCodeService.generatePng(otpauth, 256, 256);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @PostMapping("/totp/enable")
    public String enableTotp(@AuthenticationPrincipal UserDetails principal,
                             HttpSession session,
                             @RequestParam("code") String code,
                             Model model) {
        String secret = (String) session.getAttribute(SESSION_TOTP_SETUP_SECRET);
        if (!StringUtils.hasText(secret)) {
            return "redirect:/profile/totp";
        }
        try {
            int c = Integer.parseInt(code.trim());
            if (totpService.verifyCode(secret, c)) {
                userService.enableTotp(principal.getUsername(), secret);
                session.removeAttribute(SESSION_TOTP_SETUP_SECRET);
                return "redirect:/profile";
            } else {
                model.addAttribute("error", "Неверный код. Попробуйте снова.");
                model.addAttribute("secret", secret);
                model.addAttribute("otpauth", totpService.buildOtpAuthUrl("SchoolSystem", principal.getUsername(), secret));
                model.addAttribute("alreadyEnabled", false);
                return "profile/totp_setup";
            }
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Некорректный формат кода.");
            model.addAttribute("secret", secret);
            model.addAttribute("otpauth", totpService.buildOtpAuthUrl("SchoolSystem", principal.getUsername(), secret));
            model.addAttribute("alreadyEnabled", false);
            return "profile/totp_setup";
        }
    }

    @PostMapping("/totp/disable")
    public String disableTotp(@AuthenticationPrincipal UserDetails principal,
                              @RequestParam("currentPassword") String currentPassword,
                              Model model) {
        boolean ok = userService.disableTotp(principal.getUsername(), currentPassword);
        if (!ok) {
            model.addAttribute("alreadyEnabled", true);
            model.addAttribute("disableError", "Пароль неверный.");
            return "profile/totp_setup";
        }
        return "redirect:/profile";
    }
}

