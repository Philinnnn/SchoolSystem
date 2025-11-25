package kz.kstu.fit.batyrkhanov.schoolsystem.controller;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.AuditLog;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.Role;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.RoleRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AuditService;
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
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
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
                        Model model) {
        List<User> all = userRepository.findAll();
        if (query != null && !query.isBlank()) {
            String ql = query.toLowerCase();
            all = all.stream().filter(u -> u.getUsername()!=null && u.getUsername().toLowerCase().contains(ql)).toList();
        }
        if (roleFilter != null && !roleFilter.isBlank()) {
            all = all.stream().filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals(roleFilter))).toList();
        }
        model.addAttribute("users", all);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("q", query);
        model.addAttribute("roleFilter", roleFilter);
        return "admin/users";
    }

    // Вспомогательный метод для внутренних вызовов без параметров (валидация/ошибки)
    private String users(Model model) {
        return users(null, null, model);
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
            if (r != null) user.getRoles().add(r);
        }
        userRepository.save(user);
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
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
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
                              @RequestParam(name = "roles", required = false) List<String> roleNames,
                              HttpServletRequest request) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/users";
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        if (isAdmin) return "redirect:/admin/users"; // не менять роли админа
        Set<Role> newRoles = new HashSet<>();
        if (roleNames != null) {
            for (String rn : roleNames) {
                if ("ROLE_ADMIN".equals(rn)) continue;
                Role r = roleRepository.findByName(rn);
                if (r != null) newRoles.add(r);
            }
        }
        user.setRoles(newRoles);
        userRepository.save(user);
        auditService.log("ROLE_CHANGE", user.getUsername(), "Roles=" + newRoles.stream().map(Role::getName).toList(), request);
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime from = null, to = null;
        try { if (fromStr != null && !fromStr.isBlank()) from = LocalDateTime.parse(fromStr, fmt); } catch (Exception ignored) {}
        try { if (toStr != null && !toStr.isBlank()) to = LocalDateTime.parse(toStr, fmt); } catch (Exception ignored) {}
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime from = null, to = null;
        try { if (fromStr != null && !fromStr.isBlank()) from = LocalDateTime.parse(fromStr, fmt); } catch (Exception ignored) {}
        try { if (toStr != null && !toStr.isBlank()) to = LocalDateTime.parse(toStr, fmt); } catch (Exception ignored) {}
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime from = null, to = null;
        try { if (fromStr != null && !fromStr.isBlank()) from = LocalDateTime.parse(fromStr, fmt); } catch (Exception ignored) {}
        try { if (toStr != null && !toStr.isBlank()) to = LocalDateTime.parse(toStr, fmt); } catch (Exception ignored) {}
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
}
