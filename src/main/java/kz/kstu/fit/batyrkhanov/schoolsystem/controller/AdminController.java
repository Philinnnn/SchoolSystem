package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.AuditLog;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Role;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Student;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Teacher;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Director;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.RoleRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.StudentRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.TeacherRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.DirectorRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AuditService;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.BackupService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final DirectorRepository directorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final BackupService backupService;
    private final kz.kstu.fit.batyrkhanov.schoolsystem.service.UserService userService;

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           StudentRepository studentRepository,
                           TeacherRepository teacherRepository,
                           DirectorRepository directorRepository,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService,
                           BackupService backupService,
                           kz.kstu.fit.batyrkhanov.schoolsystem.service.UserService userService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.directorRepository = directorRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.backupService = backupService;
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        model.addAttribute("username", principal != null ? principal.getUsername() : "admin");
        model.addAttribute("usersCount", userRepository.count());
        model.addAttribute("roles", roleRepository.findAll(Sort.by("name")));
        model.addAttribute("totpEnabledCount", userRepository.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getTotpEnabled())).count());
        model.addAttribute("telegramLinkedCount", userRepository.findAll().stream().filter(u -> u.getTelegramId() != null).count());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(name="q", required=false) String query,
                        @RequestParam(name="role", required=false) String roleFilter,
                        @RequestParam(name="archived", required=false) String archivedFilter,
                        HttpServletRequest request,
                        Model model) {
        List<User> all = userRepository.findAll();
        if (query != null && !query.isBlank()) {
            String ql = query.toLowerCase();
            all = all.stream().filter(u -> u.getUsername()!=null && u.getUsername().toLowerCase().contains(ql)).toList();
        }
        if (roleFilter != null && !roleFilter.isBlank()) {
            all = all.stream().filter(u -> u.getRole() != null && u.getRole().getName().equals(roleFilter)).toList();
        }
        if (archivedFilter != null && !archivedFilter.isBlank()) {
            if ("true".equals(archivedFilter)) {
                all = all.stream().filter(u -> Boolean.TRUE.equals(u.getArchived())).toList();
            } else if ("false".equals(archivedFilter)) {
                all = all.stream().filter(u -> !Boolean.TRUE.equals(u.getArchived())).toList();
            }
        }

        org.springframework.security.web.csrf.CsrfToken csrfToken =
            (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }

        model.addAttribute("users", all);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("q", query);
        model.addAttribute("roleFilter", roleFilter);
        model.addAttribute("archivedFilter", archivedFilter);
        return "admin/users";
    }

    private String users(Model model) {
        return users(null, null, null, null, model);
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                             @RequestParam(required = false) String fullName,
                             @RequestParam String password,
                             @RequestParam(name = "role", required = false) String roleName,
                             HttpServletRequest request,
                             Model model) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            model.addAttribute("error", "Логин и пароль обязательны");
            return users(model);
        }
        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "Пользователь уже существует");
            return users(model);
        }
        User user = new User(username, passwordEncoder.encode(password), fullName);
        if (StringUtils.hasText(roleName)) {
            Role r = roleRepository.findByName(roleName);
            if (r != null) user.setRole(r);
        }
        userRepository.save(user);

        // Автоматически создаём запись в соответствующей таблице по роли
        if (StringUtils.hasText(roleName)) {
            if ("ROLE_STUDENT".equals(roleName)) {
                Student student = new Student();
                student.setUser(user);
                studentRepository.save(student);
            } else if ("ROLE_TEACHER".equals(roleName)) {
                Teacher teacher = new Teacher();
                teacher.setUser(user);
                teacherRepository.save(teacher);
            } else if ("ROLE_DIRECTOR".equals(roleName)) {
                Director director = new Director();
                director.setUser(user);
                directorRepository.save(director);
            }
        }

        auditService.log("USER_CREATE", username, "User created role=" + roleName, request);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id,
                             @RequestParam(name="fullName", required=false) String fullName,
                             @RequestParam(name="newPassword", required=false) String newPassword,
                             HttpServletRequest request) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/users";
        boolean isAdmin = user.getRole() != null && "ROLE_ADMIN".equals(user.getRole().getName());
        // Разрешаем менять ФИО у всех; пароль админа менять можно, но логируем отдельно
        boolean changed = false;
        if (fullName != null && !fullName.isBlank() && !Objects.equals(fullName, user.getFullName())) {
            user.setFullName(fullName.trim());
            changed = true;
            auditService.log("USER_FULLNAME_UPDATE", user.getUsername(), "Full name changed to '" + fullName.trim() + "'", request);
        }
        if (newPassword != null && !newPassword.isBlank()) {
            // простая проверка длины
            if (newPassword.length() >= 6) {
                user.setPassword(passwordEncoder.encode(newPassword));
                auditService.log("PASSWORD_RESET_ADMIN", user.getUsername(), "Password reset by admin", request);
                changed = true;
            } else {
                // Игнорируем слишком короткий пароль — можно добавить flash сообщение (упрощено)
            }
        }
        if (changed) {
            userRepository.save(user);
            auditService.log("USER_UPDATE", user.getUsername(), "Updated fields", request);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/roles")
    public String updateRoles(@PathVariable Long id,
                              @RequestParam(name = "role", required = false) String roleName,
                              HttpServletRequest request) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/users";
        boolean isAdmin = user.getRole() != null && "ROLE_ADMIN".equals(user.getRole().getName());
        if (isAdmin) return "redirect:/admin/users"; // не менять роли админа

        String oldRoleName = user.getRole() != null ? user.getRole().getName() : null;

        if (roleName != null && !roleName.isBlank()) {
            if ("ROLE_ADMIN".equals(roleName)) return "redirect:/admin/users";
            Role r = roleRepository.findByName(roleName);
            if (r != null) {
                // Удаляем старые записи в специализированных таблицах
                if (oldRoleName != null) {
                    if ("ROLE_STUDENT".equals(oldRoleName)) {
                        studentRepository.deleteById(user.getId());
                    } else if ("ROLE_TEACHER".equals(oldRoleName)) {
                        teacherRepository.deleteById(user.getId());
                    } else if ("ROLE_DIRECTOR".equals(oldRoleName)) {
                        directorRepository.deleteById(user.getId());
                    }
                }

                // Устанавливаем новую роль
                user.setRole(r);
                userRepository.save(user);

                // Создаём новые записи в специализированных таблицах
                if ("ROLE_STUDENT".equals(roleName)) {
                    Student student = new Student();
                    student.setUser(user);
                    studentRepository.save(student);
                } else if ("ROLE_TEACHER".equals(roleName)) {
                    Teacher teacher = new Teacher();
                    teacher.setUser(user);
                    teacherRepository.save(teacher);
                } else if ("ROLE_DIRECTOR".equals(roleName)) {
                    Director director = new Director();
                    director.setUser(user);
                    directorRepository.save(director);
                }

                auditService.log("ROLE_CHANGE", user.getUsername(), "Role changed from " + oldRoleName + " to " + r.getName(), request);
            }
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(name="user", required = false) String user,
                        @RequestParam(name="action", required = false) String action,
                        @RequestParam(name="from", required = false) String fromStr,
                        @RequestParam(name="to", required = false) String toStr,
                        @RequestParam(name="page", required = false, defaultValue = "0") int page,
                        @RequestParam(name="size", required = false, defaultValue = "20") int size,
                        Model model) {
        LocalDateTime from = null, to = null;
        if (fromStr != null && !fromStr.isBlank()) {
            try {
                from = LocalDateTime.parse(fromStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e) {
                // Попытка парсинга с секундами
                try {
                    from = LocalDateTime.parse(fromStr);
                } catch (Exception ex) {
                    model.addAttribute("dateError", "Неверный формат даты 'от': " + fromStr);
                }
            }
        }
        if (toStr != null && !toStr.isBlank()) {
            try {
                to = LocalDateTime.parse(toStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e) {
                // Попытка парсинга с секундами
                try {
                    to = LocalDateTime.parse(toStr);
                } catch (Exception ex) {
                    model.addAttribute("dateError", "Неверный формат даты 'до': " + toStr);
                }
            }
        }
        Page<AuditLog> p = auditService.search(user, action, from, to, page, size);
        model.addAttribute("page", p);
        model.addAttribute("actions", auditService.actions());
        return "admin/audit";
    }

    @GetMapping("/audit/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(name="user", required = false) String user,
                                            @RequestParam(name="action", required = false) String action,
                                            @RequestParam(name="from", required = false) String fromStr,
                                            @RequestParam(name="to", required = false) String toStr) {
        LocalDateTime from = null, to = null;
        if (fromStr != null && !fromStr.isBlank()) {
            try {
                from = LocalDateTime.parse(fromStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e) {
                try { from = LocalDateTime.parse(fromStr); } catch (Exception ignored) {}
            }
        }
        if (toStr != null && !toStr.isBlank()) {
            try {
                to = LocalDateTime.parse(toStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e) {
                try { to = LocalDateTime.parse(toStr); } catch (Exception ignored) {}
            }
        }
        Page<AuditLog> p = auditService.search(user, action, from, to, 0, 10000);
        String csv = auditService.toCsv(p.getContent());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_logs.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv.getBytes());
    }

    @GetMapping("/audit/export.xlsx")
    public ResponseEntity<byte[]> exportXlsx(@RequestParam(name="user", required = false) String user,
                                             @RequestParam(name="action", required = false) String action,
                                             @RequestParam(name="from", required = false) String fromStr,
                                             @RequestParam(name="to", required = false) String toStr) throws Exception {
        LocalDateTime from = null, to = null;
        if (fromStr != null && !fromStr.isBlank()) {
            try {
                from = LocalDateTime.parse(fromStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e) {
                try { from = LocalDateTime.parse(fromStr); } catch (Exception ignored) {}
            }
        }
        if (toStr != null && !toStr.isBlank()) {
            try {
                to = LocalDateTime.parse(toStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e) {
                try { to = LocalDateTime.parse(toStr); } catch (Exception ignored) {}
            }
        }
        Page<AuditLog> p = auditService.search(user, action, from, to, 0, 10000);
        java.util.List<AuditLog> logs = p.getContent();
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Audit");
            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            String[] cols = {"ID", "Time", "Username", "Action", "Details", "IP", "Session"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            for (AuditLog l : logs) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(l.getId());
                r.createCell(1).setCellValue(l.getEventTime() != null ? l.getEventTime().toString() : "");
                r.createCell(2).setCellValue(l.getUsername());
                r.createCell(3).setCellValue(l.getAction());
                r.createCell(4).setCellValue(l.getDetails());
                r.createCell(5).setCellValue(l.getIp());
                r.createCell(6).setCellValue(l.getSessionId());
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(bos);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_logs.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bos.toByteArray());
        }
    }

    @GetMapping("/data")
    public String data(Model model) {
        model.addAttribute("usersCount", userRepository.count());
        model.addAttribute("roles", roleRepository.findAll(Sort.by("name")));
        model.addAttribute("totpEnabledCount", userRepository.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getTotpEnabled())).count());
        model.addAttribute("telegramLinkedCount", userRepository.findAll().stream().filter(u -> u.getTelegramId() != null).count());
        return "admin/data";
    }

    @GetMapping("/backups")
    public String backups(Model model,
                         @RequestParam(required = false) String success,
                         @RequestParam(required = false) String error) {
        System.out.println("AdminController.backups() called");
        java.util.List<kz.kstu.fit.batyrkhanov.schoolsystem.service.BackupService.BackupInfo> backupsList = backupService.listBackups();
        System.out.println("Backups list size: " + backupsList.size());
        model.addAttribute("backups", backupsList);
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        return "admin/backups";
    }

    @PostMapping("/backups/create")
    public String createBackup(HttpServletRequest request) {
        try {
            String backupPath = backupService.createBackup();
            auditService.log("BACKUP_CREATE", getCurrentUsername(), "Created backup: " + backupPath, request);
            return "redirect:/admin/backups?success=" + java.net.URLEncoder.encode("Бэкап успешно создан", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "redirect:/admin/backups?error=" + java.net.URLEncoder.encode("Ошибка создания бэкапа: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/backups/download/{filename}")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable String filename, HttpServletRequest request) {
        try {
            java.io.File file = backupService.getBackupFile(filename);
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            auditService.log("BACKUP_DOWNLOAD", getCurrentUsername(), "Downloaded backup: " + filename, request);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/backups/delete/{filename}")
    public String deleteBackup(@PathVariable String filename, HttpServletRequest request) {
        try {
            backupService.deleteBackup(filename);
            auditService.log("BACKUP_DELETE", getCurrentUsername(), "Deleted backup: " + filename, request);
            return "redirect:/admin/backups?success=" + java.net.URLEncoder.encode("Бэкап удалён", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "redirect:/admin/backups?error=" + java.net.URLEncoder.encode("Ошибка удаления: " + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/users/{id}/archive")
    public ResponseEntity<Map<String, String>> archiveUser(@PathVariable Long id) {
        try {
            boolean success = userService.archiveUser(id, getCurrentUsername());
            if (success) {
                return ResponseEntity.ok(Map.of("success", "Пользователь архивирован"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Не удалось архивировать пользователя"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/unarchive")
    public ResponseEntity<Map<String, String>> unarchiveUser(@PathVariable Long id) {
        try {
            boolean success = userService.unarchiveUser(id, getCurrentUsername());
            if (success) {
                return ResponseEntity.ok(Map.of("success", "Пользователь разархивирован"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Не удалось разархивировать пользователя"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getCurrentUsername() {
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
