package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

public class AuditAuthHandlers {
    public record AuditAuthenticationSuccessHandler(AuditService auditService) implements AuthenticationSuccessHandler {
        @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                String username = authentication.getName();
                auditService.log("LOGIN", username, "Form login success", request);
                response.sendRedirect(request.getContextPath() + "/dashboard");
            }
        }

    public record AuditLogoutSuccessHandler(AuditService auditService) implements LogoutSuccessHandler {
        @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                if (authentication != null) {
                    auditService.log("LOGOUT", authentication.getName(), "User logged out", request);
                }
                response.sendRedirect(request.getContextPath() + "/login?logout");
            }
        }

    public record AuditLoginFailureHandler(AuditService auditService) implements AuthenticationFailureHandler {

        @Override
            public void onAuthenticationFailure(HttpServletRequest request,
                                                HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, ServletException {
                String username = request.getParameter("username");
                auditService.log("FAIL_LOGIN", username, "Login failed", request);
                request.getSession().setAttribute("LOGIN_ERROR_MSG", "Неверный логин или пароль");
                response.sendRedirect(request.getContextPath() + "/login?error");
            }
        }
}