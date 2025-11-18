package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Grade;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Schedule;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Student;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Subject;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Teacher;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @GetMapping
    public String teacherDashboard(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
        if (teacher != null && teacher.getUser() != null) {
            model.addAttribute("fullName", teacher.getUser().getFullName());
        } else {
            model.addAttribute("fullName", auth.getName());
        }
        return "teacher/dashboard";
    }

    @GetMapping("/subjects")
    public String mySubjects(Authentication auth, Model model) {
        Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
        List<Subject> subjects = teacherService.getTeacherSubjects(teacher);
        model.addAttribute("username", auth.getName());
        model.addAttribute("subjects", subjects != null ? subjects : new ArrayList<>());
        model.addAttribute("fullName", teacher != null && teacher.getUser() != null ? teacher.getUser().getFullName() : auth.getName());
        return "teacher/subjects";
    }

    @GetMapping("/schedule")
    public String mySchedule(Authentication auth, Model model) {
        Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
        List<Schedule> schedules = teacherService.getTeacherSchedule(teacher);
        model.addAttribute("username", auth.getName());
        model.addAttribute("schedules", schedules != null ? schedules : new ArrayList<>());
        model.addAttribute("fullName", teacher != null && teacher.getUser() != null ? teacher.getUser().getFullName() : auth.getName());
        return "teacher/schedule";
    }

    @GetMapping("/classes")
    public String myClasses(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        try {
            Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
            List<String> classes = teacherService.getTeacherClasses(teacher);
            if (classes == null || classes.isEmpty()) {
                classes = Arrays.asList("10А", "10Б");
            }
            model.addAttribute("classes", classes);
            model.addAttribute("fullName", teacher != null && teacher.getUser() != null ? teacher.getUser().getFullName() : auth.getName());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("classes", Arrays.asList("10А", "10Б"));
            model.addAttribute("fullName", auth.getName());
        }
        return "teacher/classes";
    }

    @GetMapping(value = "/classes.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<String>> myClassesJson(Authentication auth) {
        try {
            Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
            List<String> classes = teacherService.getTeacherClasses(teacher);
            if (classes == null || classes.isEmpty()) {
                classes = Arrays.asList("10А", "10Б");
            }
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            return ResponseEntity.ok(Arrays.asList("10А", "10Б"));
        }
    }

    @GetMapping("/students")
    public String studentsByClass(@RequestParam("className") String className,
                                  Authentication auth,
                                  Model model) {
        Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
        List<Student> students = teacherService.getStudentsByClassName(className);
        List<Subject> subjects = teacherService.getTeacherSubjects(teacher);
        List<Grade> grades = teacherService.getTeacherGradesForClass(teacher, className);
        model.addAttribute("username", auth.getName());
        model.addAttribute("className", className);
        model.addAttribute("students", students != null ? students : new ArrayList<>());
        model.addAttribute("subjects", subjects != null ? subjects : new ArrayList<>());
        model.addAttribute("grades", grades != null ? grades : new ArrayList<>());
        model.addAttribute("fullName", teacher != null && teacher.getUser() != null ? teacher.getUser().getFullName() : auth.getName());
        return "teacher/students";
    }

    @PostMapping("/grades")
    public String addGrade(@RequestParam("studentId") Long studentId,
                           @RequestParam("subjectId") Long subjectId,
                           @RequestParam("grade") Integer gradeValue,
                           @RequestParam(value = "date", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam("className") String className,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
            teacherService.addGrade(studentId, subjectId, teacher, gradeValue, date);
            redirectAttributes.addFlashAttribute("success", "Оценка успешно добавлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        redirectAttributes.addAttribute("className", className);
        return "redirect:/teacher/students";
    }

    @PostMapping("/grades/{id}/update")
    public String updateGrade(@PathVariable("id") Long gradeId,
                              @RequestParam(value = "grade", required = false) Integer gradeValue,
                              @RequestParam(value = "subjectId", required = false) Long subjectId,
                              @RequestParam(value = "date", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @RequestParam("className") String className,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
            teacherService.updateGrade(gradeId, teacher, gradeValue, date, subjectId);
            redirectAttributes.addFlashAttribute("success", "Оценка обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        redirectAttributes.addAttribute("className", className);
        return "redirect:/teacher/students";
    }

    @PostMapping("/grades/{id}/delete")
    public String deleteGrade(@PathVariable("id") Long gradeId,
                              @RequestParam("className") String className,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            Teacher teacher = teacherService.findTeacherByUsername(auth.getName());
            teacherService.deleteGrade(gradeId, teacher);
            redirectAttributes.addFlashAttribute("success", "Оценка удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        redirectAttributes.addAttribute("className", className);
        return "redirect:/teacher/students";
    }
}