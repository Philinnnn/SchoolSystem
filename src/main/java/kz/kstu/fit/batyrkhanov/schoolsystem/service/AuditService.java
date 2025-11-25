package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import jakarta.servlet.http.HttpServletRequest;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.AuditLog;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String action, String username, String details, HttpServletRequest req) {
        String ip = req != null ? req.getRemoteAddr() : null;
        String sessionId = null;
        if (req != null && req.getSession(false) != null) sessionId = req.getSession(false).getId();
        auditLogRepository.save(new AuditLog(username, action, details, ip, sessionId));
    }

    @Transactional
    public void log(String action, String username, String details) {
        auditLogRepository.save(new AuditLog(username, action, details, null, null));
    }

    public List<AuditLog> findAll() { return auditLogRepository.findAll(); }

    public String toCsv(List<AuditLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,event_time,username,action,details,ip,session_id\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AuditLog l : logs) {
            sb.append(l.getId()).append(',')
              .append(escape(l.getEventTime() != null ? l.getEventTime().format(fmt) : ""))
              .append(',').append(escape(l.getUsername()))
              .append(',').append(escape(l.getAction()))
              .append(',').append(escape(l.getDetails()))
              .append(',').append(escape(l.getIp()))
              .append(',').append(escape(l.getSessionId()))
              .append('\n');
        }
        return sb.toString();
    }

    public Page<AuditLog> search(String user, String action, LocalDateTime from, LocalDateTime to, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1), 200));
        return auditLogRepository.search(emptyToNull(user), emptyToNull(action), from, to, pageable);
    }

    public Set<String> actions() {
        return auditLogRepository.findAll().stream().map(AuditLog::getAction).filter(s -> s != null && !s.isBlank()).collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    private String escape(String v) {
        if (v == null) return "";
        boolean needQuotes = v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"");
        String escaped = v.replace("\"", "\"\"");
        return needQuotes ? ('"' + escaped + '"') : escaped;
    }

    private String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}
