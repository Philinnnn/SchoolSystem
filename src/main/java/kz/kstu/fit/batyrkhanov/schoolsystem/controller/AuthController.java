package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import jakarta.servlet.http.HttpSession;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.security.AuthConstants;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.TotpService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth/totp")
public class AuthController {

    @GetMapping
    public String verifyPage(Authentication authentication, HttpSession session, Model model) {
        if (authentication == null) return "redirect:/login";
        Boolean verified = (Boolean) session.getAttribute(AuthConstants.SESSION_TOTP_VERIFIED);
        if (Boolean.TRUE.equals(verified)) return "redirect:/dashboard";

        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        User user = userRepository.findByUsername(username);
        if (user == null || !Boolean.TRUE.equals(user.getTotpEnabled())) {
            // нет 2FA — не требуем
            session.setAttribute(AuthConstants.SESSION_TOTP_VERIFIED, true);
            return "redirect:/dashboard";
        }
        model.addAttribute("error", null);
        return "auth/totp_verify";
    }

    @PostMapping("/verify")
    public String verify(Authentication authentication,
                         HttpSession session,
                         @RequestParam("code") String code,
                         Model model) {
        if (authentication == null) return "redirect:/login";
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        User user = userRepository.findByUsername(username);
        if (user == null) return "redirect:/login";
        if (!Boolean.TRUE.equals(user.getTotpEnabled())) {
            session.setAttribute(AuthConstants.SESSION_TOTP_VERIFIED, true);
            return "redirect:/dashboard";
        }
        try {
            int c = Integer.parseInt(code.trim());
            boolean ok = totpService.verifyCode(user.getTotpSecret(), c);
            if (ok) {
                session.setAttribute(AuthConstants.SESSION_TOTP_VERIFIED, true);
                return "redirect:/dashboard";
            } else {
                model.addAttribute("error", "Неверный код. Попробуйте ещё раз.");
                return "auth/totp_verify";
            }
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Некорректный формат кода.");
            return "auth/totp_verify";
        }
    }

    private final TotpService totpService;
    private final UserRepository userRepository;

    public AuthController(TotpService totpService, UserRepository userRepository) {
        this.totpService = totpService;
        this.userRepository = userRepository;
    }
}

