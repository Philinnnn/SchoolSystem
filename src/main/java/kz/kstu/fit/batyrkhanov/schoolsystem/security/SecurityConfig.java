package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
public class SecurityConfig {
    @Autowired
    private AuditService auditService;

    @Bean
    public AuthenticationSuccessHandler auditAuthSuccessHandler() {
        return new AuditAuthHandlers.AuditAuthenticationSuccessHandler(auditService);
    }
    @Bean
    public LogoutSuccessHandler auditLogoutSuccessHandler() {
        return new AuditAuthHandlers.AuditLogoutSuccessHandler(auditService);
    }
    @Bean
    public AuditAuthHandlers.AuditLoginFailureHandler auditLoginFailureHandler() {
        return new AuditAuthHandlers.AuditLoginFailureHandler(auditService);
    }

    @Bean
    public TotpVerificationFilter totpVerificationFilter(UserRepository userRepository) {
        return new TotpVerificationFilter(userRepository);
    }

    @Bean
    public ArchivedUserFilter archivedUserFilter(UserRepository userRepository) {
        return new ArchivedUserFilter(userRepository);
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter() { return new RateLimitingFilter(); }

    @Bean
    public SimpleWafFilter simpleWafFilter(@org.springframework.beans.factory.annotation.Value("${waf.enabled:true}") boolean enabled) {
        return new SimpleWafFilter(enabled);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                          TotpVerificationFilter totpVerificationFilter,
                                          ArchivedUserFilter archivedUserFilter,
                                          RateLimitingFilter rateLimitingFilter,
                                          SimpleWafFilter simpleWafFilter) throws Exception {

        http
                // CSRF: игнорируем Telegram + Backup эндпоинты
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/login/telegram/**", "/admin/backups/**")
                )
                // Заголовки безопасности (блочный стиль)
                .headers(headers -> {
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000)
                    );
                    headers.contentTypeOptions(Customizer.withDefaults()); // X-Content-Type-Options: nosniff
                    headers.frameOptions(frame -> frame.sameOrigin()); // X-Frame-Options: SAMEORIGIN (или .deny())
                    headers.referrerPolicy(ref -> ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
                    headers.permissionsPolicy(pp -> pp.policy("geolocation=(), microphone=(), camera=(), fullscreen=(self)"));
                    headers.contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; font-src 'self' data:; script-src 'self' 'unsafe-inline'; script-src-elem 'self' 'unsafe-inline'; connect-src 'self'; frame-ancestors 'self'; base-uri 'self'; form-action 'self'")
                    );
                })
                .authorizeHttpRequests(auth -> auth
                        // Разрешённые страницы и статические ресурсы
                        .requestMatchers("/", "/login", "/login/telegram/**", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/login/telegram/start", "/login/telegram/request/status", "/login/telegram/consume").permitAll()

                        // Роли
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/director/**").hasAnyRole("DIRECTOR", "ADMIN")
                        .requestMatchers("/teacher/**").hasAnyRole("TEACHER", "DIRECTOR", "ADMIN")
                        .requestMatchers("/student/**").hasRole("STUDENT")

                        // Остальное требует авторизации
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(auditAuthSuccessHandler())
                        .failureHandler(auditLoginFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(auditLogoutSuccessHandler())
                        .permitAll()
                );

        http.addFilterBefore(simpleWafFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(totpVerificationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(archivedUserFilter, TotpVerificationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}