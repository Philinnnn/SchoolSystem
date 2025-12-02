package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простой rate limiting без внешних библиотек:
 * - Глобально: до 120 запросов за 60 секунд на один IP.
 * - Для POST /login: до 5 запросов за 60 секунд на IP (анти-brute-force).
 * Возвращает 429 Too Many Requests при превышении + Retry-After.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofSeconds(60);

    private static final int GLOBAL_LIMIT = 120; // r/мин/IP
    private static final int LOGIN_LIMIT = 5; // POST /login r/мин/IP

    private final Map<String, Deque<Long>> globalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> loginBuckets = new ConcurrentHashMap<>();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (pathMatcher.match("/admin/**", path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isStatic(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);
        long now = System.currentTimeMillis();

        if (isLoginAttempt(request)) {
            Deque<Long> q = loginBuckets.computeIfAbsent(ip, k -> new ArrayDeque<>());
            long retryAfter = tryConsume(q, now, LOGIN_LIMIT, WINDOW);
            if (retryAfter > 0) {
                sendTooMany(response, retryAfter);
                return;
            }
        }

        Deque<Long> q = globalBuckets.computeIfAbsent(ip, k -> new ArrayDeque<>());
        long retryAfter = tryConsume(q, now, GLOBAL_LIMIT, WINDOW);
        if (retryAfter > 0) {
            sendTooMany(response, retryAfter);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginAttempt(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && pathMatcher.match("/login", request.getRequestURI());
    }

    private boolean isStatic(String path) {
        // Каталоги статики
        if (pathMatcher.match("/css/**", path) ||
            pathMatcher.match("/js/**", path) ||
            pathMatcher.match("/images/**", path) ||
            pathMatcher.match("/static/**", path) ||
            pathMatcher.match("/public/**", path) ||
            pathMatcher.match("/resources/**", path) ||
            pathMatcher.match("/webjars/**", path) ||
            pathMatcher.match("/assets/**", path) ||
            "/favicon.ico".equals(path)) {
            return true;
        }
        // Файлы по расширениям
        return pathMatcher.match("/**/*.js", path) ||
               pathMatcher.match("/**/*.css", path) ||
               pathMatcher.match("/**/*.map", path) ||
               pathMatcher.match("/**/*.png", path) ||
               pathMatcher.match("/**/*.jpg", path) ||
               pathMatcher.match("/**/*.jpeg", path) ||
               pathMatcher.match("/**/*.svg", path) ||
               pathMatcher.match("/**/*.ico", path) ||
               pathMatcher.match("/**/*.gif", path) ||
               pathMatcher.match("/**/*.woff", path) ||
               pathMatcher.match("/**/*.woff2", path) ||
               pathMatcher.match("/**/*.ttf", path);
    }

    /**
     * Возвращает 0 если токен выдан, либо сек до следующего запроса при превышении
     */
    private long tryConsume(Deque<Long> queue, long nowMillis, int capacity, Duration window) {
        long windowStart = nowMillis - window.toMillis();
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() < windowStart) {
                queue.pollFirst();
            }
            if (queue.size() >= capacity) {
                long oldest = Objects.requireNonNull(queue.peekFirst());
                long waitMs = (oldest + window.toMillis()) - nowMillis;
                return Math.max(1, (long) Math.ceil(waitMs / 1000.0));
            }
            queue.addLast(nowMillis);
            return 0;
        }
    }

    private void sendTooMany(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("text/plain; charset=UTF-8");
        response.getWriter().write("Слишком много запросов. Повторите попытку позже.");
    }

    private String extractClientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) return cf.trim();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Берем первый IP из списка
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }
}
