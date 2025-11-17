package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @GetMapping
    public String teacherDashboard(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        return "teacher/dashboard";
    }
}

