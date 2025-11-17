package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentController {

    @GetMapping
    public String studentDashboard(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        return "student/dashboard";
    }
}

