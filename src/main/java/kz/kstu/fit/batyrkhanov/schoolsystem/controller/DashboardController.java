package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return "redirect:/admin";

        boolean isDirector = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR"));
        if (isDirector) return "redirect:/director";

        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        if (isTeacher) return "redirect:/teacher";

        boolean isStudent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) return "redirect:/student";

        return "redirect:/login";
    }
}