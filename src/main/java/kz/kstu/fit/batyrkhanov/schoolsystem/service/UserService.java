package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username);
        if (user == null) return false;
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) return false;
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditService.log("PASSWORD_CHANGE", username, "User changed own password");
        return true;
    }

    @Transactional
    public void enableTotp(String username, String secret) {
        User user = userRepository.findByUsername(username);
        if (user == null) return;
        user.setTotpSecret(secret);
        user.setTotpEnabled(true);
        userRepository.save(user);
        auditService.log("TOTP_ENABLE", username, "Enabled TOTP");
    }

    @Transactional
    public boolean disableTotp(String username, String currentPassword) {
        User user = userRepository.findByUsername(username);
        if (user == null) return false;
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) return false;
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        auditService.log("TOTP_DISABLE", username, "Disabled TOTP");
        return true;
    }

    @Transactional
    public boolean archiveUser(Long userId, String adminUsername) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        boolean isAdmin = user.getRole() != null && "ROLE_ADMIN".equals(user.getRole().getName());
        if (isAdmin) return false;

        user.setArchived(true);
        userRepository.save(user);
        auditService.log("USER_ARCHIVE", adminUsername, "Archived user: " + user.getUsername());
        return true;
    }

    @Transactional
    public boolean unarchiveUser(Long userId, String adminUsername) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        user.setArchived(false);
        userRepository.save(user);
        auditService.log("USER_UNARCHIVE", adminUsername, "Unarchived user: " + user.getUsername());
        return true;
    }
}
