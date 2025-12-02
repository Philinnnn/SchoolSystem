package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SimpleWafFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<Pattern> denyPatterns = new ArrayList<>();

    public SimpleWafFilter(boolean enabled) {
        this.enabled = enabled;

        denyPatterns.add(Pattern.compile("(?i)(union\\s+select|select\\s+\\*\\s+from|information_schema|drop\\s+table|--|;\\s*shutdown)"));
        denyPatterns.add(Pattern.compile("(?i)<script[^>]*>"));
        denyPatterns.add(Pattern.compile("(?i)%3Cscript")); // URL-encoded <script>
        denyPatterns.add(Pattern.compile("(?i)onerror\\s*=|onload\\s*=|javascript:"));
        denyPatterns.add(Pattern.compile("\\.\\./\\.\\./")); // path traversal ../../
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || isStatic(request) || isHealth(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String combined = buildCombinedPayload(request);
        for (Pattern p : denyPatterns) {
            if (p.matcher(combined).find()) {
                block(response, p.pattern());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isStatic(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/favicon") || uri.startsWith("/images/");
    }

    private boolean isHealth(HttpServletRequest req) {
        return matcher.match("/error", req.getRequestURI());
    }

    private String buildCombinedPayload(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getRequestURI()).append('?');
        if (request.getQueryString() != null) sb.append(request.getQueryString());
        request.getParameterMap().forEach((k,v) -> {
            sb.append('&').append(k).append('=');
            for (String val : v) sb.append(val).append(',');
        });
        String ua = request.getHeader("User-Agent");
        if (ua != null) sb.append(" UA=").append(ua);
        String ref = request.getHeader("Referer");
        if (ref != null) sb.append(" REF=").append(ref);
        return sb.toString();
    }

    private void block(HttpServletResponse response, String pattern) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Request blocked by WAF (pattern: " + pattern + ")");
    }
}
