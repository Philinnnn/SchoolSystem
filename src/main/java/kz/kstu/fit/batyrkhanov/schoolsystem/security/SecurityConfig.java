package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import kz.kstu.fit.batyrkhanov.schoolsystem.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
    public TotpVerificationFilter totpVerificationFilter(UserRepository userRepository) {
        return new TotpVerificationFilter(userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TotpVerificationFilter totpVerificationFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
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
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(auditLogoutSuccessHandler())
                        .permitAll()
                );

        http.addFilterAfter(totpVerificationFilter, UsernamePasswordAuthenticationFilter.class);

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