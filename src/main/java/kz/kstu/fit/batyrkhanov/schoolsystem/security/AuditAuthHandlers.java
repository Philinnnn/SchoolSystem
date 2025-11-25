package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

public class AuditAuthHandlers {
    public static class AuditAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
        private final AuditService auditService;
        public AuditAuthenticationSuccessHandler(AuditService auditService) { this.auditService = auditService; }
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
            String username = authentication.getName();
            auditService.log("LOGIN", username, "Form login success", request);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    public static class AuditLogoutSuccessHandler implements LogoutSuccessHandler {
        private final AuditService auditService;
        public AuditLogoutSuccessHandler(AuditService auditService) { this.auditService = auditService; }
        @Override
        public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
            if (authentication != null) {
                auditService.log("LOGOUT", authentication.getName(), "User logged out", request);
            }
            response.sendRedirect(request.getContextPath() + "/login?logout");
        }
    }
}

