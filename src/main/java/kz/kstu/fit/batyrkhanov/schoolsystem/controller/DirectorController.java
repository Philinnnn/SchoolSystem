package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.*;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.DirectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/director")
public class DirectorController {

    @Autowired
    private DirectorService directorService;

    @GetMapping
    public String directorDashboard(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());

        // Добавляем статистику
        model.addAttribute("teachersCount", directorService.countTeachers());
        model.addAttribute("studentsCount", directorService.countStudents());
        model.addAttribute("subjectsCount", directorService.countSubjects());
        model.addAttribute("classesCount", directorService.countClasses());

        return "director/dashboard";
    }

    // === Управление учителями ===

    @GetMapping("/teachers")
    public String teachers(Model model) {
        List<Teacher> teachers = directorService.getAllTeachers();
        model.addAttribute("teachers", teachers);
        return "director/teachers";
    }

    @GetMapping("/teachers/{id}")
    public String teacherDetails(@PathVariable Long id, Model model) {
        Teacher teacher = directorService.getTeacherById(id);
        if (teacher == null) {
            return "redirect:/director/teachers";
        }
        model.addAttribute("teacher", teacher);
        return "director/teacher_details";
    }

    // === Управление студентами ===

    @GetMapping("/students")
    public String students(@RequestParam(required = false) String className, Model model) {
        List<Student> students;
        if (className != null && !className.isBlank()) {
            students = directorService.getStudentsByClassName(className);
        } else {
            students = directorService.getAllStudents();
        }

        List<String> classNames = directorService.getAllClassNames();

        model.addAttribute("students", students);
        model.addAttribute("classNames", classNames);
        model.addAttribute("selectedClass", className);
        return "director/students";
    }

    @GetMapping("/students/{id}")
    public String studentDetails(@PathVariable Long id, Model model) {
        Student student = directorService.getStudentById(id);
        if (student == null) {
            return "redirect:/director/students";
        }
        model.addAttribute("student", student);
        return "director/student_details";
    }

    // === Управление предметами ===

    @GetMapping("/subjects")
    public String subjects(Model model) {
        List<Subject> subjects = directorService.getAllSubjects();
        model.addAttribute("subjects", subjects);
        return "director/subjects";
    }

    @GetMapping("/subjects/create")
    public String createSubjectForm(Model model) {
        return "director/subject_form";
    }

    @PostMapping("/subjects/create")
    public String createSubject(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            directorService.createSubject(name, description, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Предмет успешно создан");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/director/subjects/create";
        }
        return "redirect:/director/subjects";
    }

    @GetMapping("/subjects/{id}/edit")
    public String editSubjectForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Subject subject = directorService.getSubjectById(id);
        if (subject == null) {
            redirectAttributes.addFlashAttribute("error", "Предмет не найден");
            return "redirect:/director/subjects";
        }
        model.addAttribute("subject", subject);
        return "director/subject_form";
    }

    @PostMapping("/subjects/{id}/edit")
    public String editSubject(@PathVariable Long id,
                              @RequestParam String name,
                              @RequestParam(required = false) String description,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            directorService.updateSubject(id, name, description, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Предмет успешно обновлён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/director/subjects/" + id + "/edit";
        }
        return "redirect:/director/subjects";
    }

    @PostMapping("/subjects/{id}/delete")
    public String deleteSubject(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            directorService.deleteSubject(id, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Предмет успешно удалён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/director/subjects";
    }

    // === Расписание ===

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) String className, Model model) {
        List<Schedule> schedules;
        if (className != null && !className.isBlank()) {
            schedules = directorService.getScheduleByClassName(className);
        } else {
            schedules = directorService.getAllSchedules();
        }

        List<String> classNames = directorService.getAllClassNames();

        // Формируем сетку расписания по времени
        java.util.Set<String> timeSlotsSet = new java.util.TreeSet<>();
        java.util.Map<String, java.util.List<Schedule>> scheduleGrid = new java.util.HashMap<>();

        for (Schedule schedule : schedules) {
            String timeSlot = schedule.getStartTime().toString();
            timeSlotsSet.add(timeSlot);

            String key = schedule.getDayOfWeek().name() + "_" + timeSlot;
            scheduleGrid.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(schedule);
        }

        // Список дней без воскресенья (Пн-Сб)
        java.util.List<java.time.DayOfWeek> daysList = java.util.Arrays.asList(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY,
            java.time.DayOfWeek.SATURDAY
        );

        model.addAttribute("schedules", schedules);
        model.addAttribute("classNames", classNames);
        model.addAttribute("selectedClass", className);
        model.addAttribute("timeSlots", new java.util.ArrayList<>(timeSlotsSet));
        model.addAttribute("scheduleGrid", scheduleGrid);
        model.addAttribute("daysList", daysList);
        return "director/schedule_grid";
    }

    @GetMapping("/schedule/create")
    public String createScheduleForm(Model model) {
        model.addAttribute("subjects", directorService.getAllSubjects());
        model.addAttribute("teachers", directorService.getAllTeachers());
        model.addAttribute("classNames", directorService.getAllClassNames());
        return "director/schedule_form";
    }

    @PostMapping("/schedule/create")
    public String createSchedule(@RequestParam Long subjectId,
                                 @RequestParam Long teacherId,
                                 @RequestParam String className,
                                 @RequestParam String dayOfWeek,
                                 @RequestParam String startTime,
                                 @RequestParam String endTime,
                                 @RequestParam(required = false) String roomNumber,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            directorService.createSchedule(subjectId, teacherId, className, dayOfWeek,
                startTime, endTime, roomNumber, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Занятие успешно добавлено в расписание");
            redirectAttributes.addAttribute("className", className);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/director/schedule/create";
        }
        return "redirect:/director/schedule";
    }

    @GetMapping("/schedule/{id}/edit")
    public String editScheduleForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Schedule schedule = directorService.getScheduleById(id);
        if (schedule == null) {
            redirectAttributes.addFlashAttribute("error", "Занятие не найдено");
            return "redirect:/director/schedule";
        }
        model.addAttribute("schedule", schedule);
        model.addAttribute("subjects", directorService.getAllSubjects());
        model.addAttribute("teachers", directorService.getAllTeachers());
        model.addAttribute("classNames", directorService.getAllClassNames());
        return "director/schedule_form";
    }

    @PostMapping("/schedule/{id}/edit")
    public String editSchedule(@PathVariable Long id,
                               @RequestParam Long subjectId,
                               @RequestParam Long teacherId,
                               @RequestParam String className,
                               @RequestParam String dayOfWeek,
                               @RequestParam String startTime,
                               @RequestParam String endTime,
                               @RequestParam(required = false) String roomNumber,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            directorService.updateSchedule(id, subjectId, teacherId, className, dayOfWeek,
                startTime, endTime, roomNumber, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Занятие успешно обновлено");
            redirectAttributes.addAttribute("className", className);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/director/schedule/" + id + "/edit";
        }
        return "redirect:/director/schedule";
    }

    @PostMapping("/schedule/{id}/delete")
    public String deleteSchedule(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            Schedule schedule = directorService.getScheduleById(id);
            String className = schedule != null ? schedule.getClassName() : null;
            directorService.deleteSchedule(id, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Занятие успешно удалено из расписания");
            if (className != null) {
                redirectAttributes.addAttribute("className", className);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/director/schedule";
    }
}

