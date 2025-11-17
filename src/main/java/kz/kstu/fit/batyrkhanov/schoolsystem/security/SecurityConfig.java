package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Разрешённые страницы и статические ресурсы
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()

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
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

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