package kz.kstu.fit.batyrkhanov.schoolsystem.security;

import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        kz.kstu.fit.batyrkhanov.schoolsystem.entity.User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("Пользователь не найден: " + username);
        }

        if (Boolean.TRUE.equals(user.getArchived())) {
            throw new UsernameNotFoundException("Учётная запись архивирована: " + username);
        }

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(Boolean.TRUE.equals(user.getArchived()))
                .build();
    }
}
