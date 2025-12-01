package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ArchivedUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public ArchivedUserFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {

            String username = authentication.getName();
            User user = userRepository.findByUsername(username);

            if (user != null && Boolean.TRUE.equals(user.getArchived())) {
                SecurityContextHolder.clearContext();
                request.getSession().invalidate();
                response.sendRedirect("/login?archived=true");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

