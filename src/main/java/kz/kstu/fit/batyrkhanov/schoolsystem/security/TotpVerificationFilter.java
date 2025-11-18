package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class TotpVerificationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    private static final Set<String> WHITELIST = Set.of(
            "/login", "/logout",
            "/login/telegram", "/login/telegram/",
            "/login/telegram/start", "/login/telegram/request/status", "/login/telegram/consume",
            "/auth/totp", "/auth/totp/verify", "/error"
    );

    public TotpVerificationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails) {
            String path = request.getRequestURI();
            if (isStatic(path) || isWhitelisted(path)) {
                filterChain.doFilter(request, response);
                return;
            }
            HttpSession session = request.getSession(false);
            Boolean verified = session != null ? (Boolean) session.getAttribute(AuthConstants.SESSION_TOTP_VERIFIED) : null;

            String username = ((UserDetails) auth.getPrincipal()).getUsername();
            User user = userRepository.findByUsername(username);

            if (user != null && Boolean.TRUE.equals(user.getTotpEnabled())) {
                if (!Boolean.TRUE.equals(verified)) {
                    response.sendRedirect(request.getContextPath() + "/auth/totp");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String path) {
        if (path == null) return false;
        if (WHITELIST.contains(path)) return true;
        // Разрешаем статику и css/js
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/favicon") || path.startsWith("/images/");
    }

    private boolean isStatic(String path) {
        return path != null && (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/favicon") || path.startsWith("/images/"));
    }
}
