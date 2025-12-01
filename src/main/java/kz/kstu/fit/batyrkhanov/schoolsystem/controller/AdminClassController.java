package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.SchoolClass;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Student;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminClassController {

    @Autowired
    private AdminService adminService;

    // ===== Управление классами =====

    @GetMapping("/classes")
    public String classes(Model model) {
        List<SchoolClass> classes = adminService.getAllClasses();
        model.addAttribute("classes", classes);
        return "admin/classes";
    }

    @GetMapping("/classes/create")
    public String createClassForm() {
        return "admin/class_form";
    }

    @PostMapping("/classes/create")
    public String createClass(@RequestParam String name,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            adminService.createClass(name, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Класс успешно создан");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/classes/create";
        }
        return "redirect:/admin/classes";
    }

    @GetMapping("/classes/{id}/edit")
    public String editClassForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        SchoolClass schoolClass = adminService.getClassById(id);
        if (schoolClass == null) {
            redirectAttributes.addFlashAttribute("error", "Класс не найден");
            return "redirect:/admin/classes";
        }
        model.addAttribute("schoolClass", schoolClass);
        return "admin/class_form";
    }

    @PostMapping("/classes/{id}/edit")
    public String editClass(@PathVariable Long id,
                           @RequestParam String name,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        try {
            adminService.updateClass(id, name, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Класс успешно обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/classes/" + id + "/edit";
        }
        return "redirect:/admin/classes";
    }

    @PostMapping("/classes/{id}/delete")
    public String deleteClass(@PathVariable Long id,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteClass(id, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Класс успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    // ===== Назначение студентов в классы =====

    @GetMapping("/students/assign-class")
    public String assignStudentClassPage(Model model) {
        List<SchoolClass> classes = adminService.getAllClasses();
        List<Student> studentsWithoutClass = adminService.getStudentsWithoutClass();

        model.addAttribute("classes", classes);
        model.addAttribute("studentsWithoutClass", studentsWithoutClass);

        return "admin/assign_student_class";
    }

    @PostMapping("/students/{id}/assign-class")
    public String assignStudentClass(@PathVariable Long id,
                                     @RequestParam(required = false) String className,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            adminService.assignStudentToClass(id, className, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Класс студента успешно изменен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students/assign-class";
    }
}

