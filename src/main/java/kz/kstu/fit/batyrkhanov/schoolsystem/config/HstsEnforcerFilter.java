package kz.kstu.fit.batyrkhanov.schoolsystem.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ensures HSTS header present on HTTPS responses (ssl profile only).
 */
@Component
@Profile("ssl")
public class HstsEnforcerFilter implements Filter {
    private static final String HSTS = "max-age=31536000; includeSubDomains";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        if (req.isSecure() && !res.containsHeader("Strict-Transport-Security")) {
            res.setHeader("Strict-Transport-Security", HSTS);
        }
        chain.doFilter(request, response);
    }
}
