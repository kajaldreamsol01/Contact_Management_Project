package com.contactmanagement.user.config;

import com.contactmanagement.user.entity.AppUser;
import com.contactmanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String @NonNull ... args) {
        ensure("Admin", "admin@dreamsol.com", "Admin@123", "ADMIN");
        ensure("HOD", "hod@dreamsol.com", "Hod@123", "HOD");
        ensure("Management", "management@dreamsol.com", "Management@123", "MANAGEMENT");
        ensure("Normal User", "user@dreamsol.com", "User@123", "USER");
    }

    private void ensure(String name, String email, String password, String role) {
        AppUser user = repository.findByEmailIgnoreCase(email).orElseGet(AppUser::new);
        user.setName(name);
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setRole(role);
        user.setStatus(false);
        if (Objects.isNull(user.getPassword()) || !passwordEncoder.matches(password, user.getPassword()))
            user.setPassword(passwordEncoder.encode(password));
        repository.save(user);
    }
}
