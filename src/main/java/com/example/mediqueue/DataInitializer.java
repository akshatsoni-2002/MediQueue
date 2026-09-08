package com.example.mediqueue;

import com.example.mediqueue.model.Role;
import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            ensureAccount(userRepository, passwordEncoder,
                    "STAFF001", "Staff@123", Role.STAFF, "Staff");

            ensureAccount(userRepository, passwordEncoder,
                    "ADMIN001", "Admin@123", Role.ADMIN, "Administrator");
        };
    }

    private void ensureAccount(
            UserRepository repository,
            PasswordEncoder encoder,
            String userId,
            String rawPassword,
            Role role,
            String displayName) {

        User user = repository.findByUserId(userId).orElse(null);

        if (user == null) {
            user = new User(
                    userId,
                    encoder.encode(rawPassword),
                    role,
                    null
            );
            user.setDisplayName(displayName);
            user.setActive(true);
            repository.save(user);
            System.out.println("Created initial " + role + " account: " + userId);
            return;
        }

        // Repair only the built-in development accounts. No other user,
        // doctor, patient, appointment or hospital data is changed.
        boolean changed = false;

        if (user.getRole() != role) {
            user.setRole(role);
            changed = true;
        }

        if (!user.isActive()) {
            user.setActive(true);
            changed = true;
        }

        if (user.getPassword() == null || !encoder.matches(rawPassword, user.getPassword())) {
            user.setPassword(encoder.encode(rawPassword));
            changed = true;
        }

        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            user.setDisplayName(displayName);
            changed = true;
        }

        if (changed) {
            repository.save(user);
            System.out.println("Repaired built-in account: " + userId);
        }
    }
}
