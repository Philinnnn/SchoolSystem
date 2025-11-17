package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Grade;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Schedule;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Student;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Subject;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Teacher;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping
    public String studentDashboard(Authentication auth, Model model) {
        try {
            Student student = studentService.findStudentByUsername(auth.getName());
            
            if (student != null && student.getUser() != null) {
                model.addAttribute("student", student);
                model.addAttribute("fullName", student.getUser().getFullName());
                model.addAttribute("className", student.getClassName());
            } else {
                model.addAttribute("fullName", auth.getName());
                model.addAttribute("className", "Не назначен");
            }
            
            model.addAttribute("username", auth.getName());
            return "student/dashboard";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/grades")
    public String viewGrades(Authentication auth, Model model) {
        try {
            Student student = studentService.findStudentByUsername(auth.getName());
            
            if (student != null) {
                List<Grade> grades = studentService.getStudentGrades(student);
                model.addAttribute("grades", grades != null ? grades : new ArrayList<>());
                model.addAttribute("student", student);
                if (student.getUser() != null) {
                    model.addAttribute("fullName", student.getUser().getFullName());
                }
            } else {
                model.addAttribute("grades", new ArrayList<>());
                model.addAttribute("fullName", auth.getName());
            }
            
            model.addAttribute("username", auth.getName());
            return "student/grades";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", auth.getName());
            return "student/grades";
        }
    }

    @GetMapping("/subjects")
    public String viewSubjects(Authentication auth, Model model) {
        try {
            Student student = studentService.findStudentByUsername(auth.getName());
            List<Subject> subjects = studentService.getAllSubjects();
            
            model.addAttribute("subjects", subjects != null ? subjects : new ArrayList<>());
            model.addAttribute("username", auth.getName());
            
            if (student != null && student.getUser() != null) {
                model.addAttribute("fullName", student.getUser().getFullName());
            } else {
                model.addAttribute("fullName", auth.getName());
            }
            
            return "student/subjects";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("subjects", new ArrayList<>());
            model.addAttribute("username", auth.getName());
            return "student/subjects";
        }
    }

    @GetMapping("/teachers")
    public String viewTeachers(Authentication auth, Model model) {
        try {
            Student student = studentService.findStudentByUsername(auth.getName());
            List<Teacher> teachers = studentService.getAllTeachers();
            
            model.addAttribute("teachers", teachers != null ? teachers : new ArrayList<>());
            model.addAttribute("username", auth.getName());
            
            if (student != null && student.getUser() != null) {
                model.addAttribute("fullName", student.getUser().getFullName());
            } else {
                model.addAttribute("fullName", auth.getName());
            }
            
            return "student/teachers";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("teachers", new ArrayList<>());
            model.addAttribute("username", auth.getName());
            return "student/teachers";
        }
    }

    @GetMapping("/schedule")
    public String viewSchedule(Authentication auth, Model model) {
        try {
            Student student = studentService.findStudentByUsername(auth.getName());

            if (student != null) {
                List<Schedule> schedules = studentService.getStudentSchedule(student);
                model.addAttribute("schedules", schedules != null ? schedules : new ArrayList<>());
                model.addAttribute("student", student);
                if (student.getUser() != null) {
                    model.addAttribute("fullName", student.getUser().getFullName());
                }
                model.addAttribute("className", student.getClassName());
            } else {
                model.addAttribute("schedules", new ArrayList<>());
                model.addAttribute("fullName", auth.getName());
                model.addAttribute("className", "Не назначен");
            }

            model.addAttribute("username", auth.getName());
            return "student/schedule";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("schedules", new ArrayList<>());
            model.addAttribute("username", auth.getName());
            return "student/schedule";
        }
    }
}
