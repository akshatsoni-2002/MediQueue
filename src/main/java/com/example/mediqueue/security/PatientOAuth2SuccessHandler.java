package com.example.mediqueue.security;

import com.example.mediqueue.model.Patient;
import com.example.mediqueue.model.Role;
import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.PatientRepository;
import com.example.mediqueue.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class PatientOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public PatientOAuth2SuccessHandler(
            UserRepository userRepository,
            PatientRepository patientRepository) {

        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        // Make sure this is a Google OAuth2 login
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            response.sendRedirect("/patient/login?error=google");
            return;
        }

        var attributes = token.getPrincipal().getAttributes();

        String rawEmail = (String) attributes.get("email");
        String rawName = (String) attributes.get("name");

        String email = rawEmail == null
                ? null
                : rawEmail.trim().toLowerCase();

        String name = (rawName == null || rawName.isBlank())
                ? null
                : rawName.trim();

        // Google did not provide an email
        if (email == null || email.isBlank()) {
            response.sendRedirect("/patient/login?error=google_email");
            return;
        }

        /*
         * Check whether a MediQueue account already exists
         * using the Google email as the patient User ID.
         */
        User user = userRepository
                .findByUserId(email)
                .orElse(null);

        if (user == null) {

            /*
             * Check whether a patient profile already exists
             * with this Google email.
             */
            Patient patient = patientRepository
                    .findByEmailIgnoreCase(email)
                    .orElseGet(() -> {

                        Patient newPatient = new Patient();

                        newPatient.setFullName(
                                name == null || name.isBlank()
                                        ? email.substring(
                                                0,
                                                email.indexOf("@"))
                                        : name
                        );

                        newPatient.setAge(0);
                        newPatient.setGender("Not specified");
                        newPatient.setPhone("");
                        newPatient.setEmail(email);
                        newPatient.setActive(true);

                        return patientRepository.save(newPatient);
                    });

            /*
             * Create a MediQueue PATIENT account.
             *
             * We intentionally use BCryptPasswordEncoder directly
             * instead of injecting PasswordEncoder here. This avoids
             * the circular dependency between SecurityConfig and
             * PatientOAuth2SuccessHandler.
             */
            BCryptPasswordEncoder encoder =
                    new BCryptPasswordEncoder();

            String temporaryPassword =
                    encoder.encode(UUID.randomUUID().toString());

            user = new User(
                    email,
                    temporaryPassword,
                    Role.PATIENT,
                    null
            );

            user.setPatient(patient);
            user.setDisplayName(name);
            user.setEmail(email);
            user.setActive(true);

            userRepository.save(user);

        } else {

            /*
             * If an account exists, make sure it is actually
             * a PATIENT account and is active.
             */
            if (user.getRole() != Role.PATIENT || !user.isActive()) {

                response.sendRedirect(
                        "/patient/login?error=not_patient"
                );

                return;
            }
        }

        /*
         * Create the authenticated Spring Security session
         * for the patient.
         */
        var patientAuthentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(),
                        user.getPassword(),
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_PATIENT"
                                )
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(patientAuthentication);

        request.getSession().setAttribute(
                "SPRING_SECURITY_CONTEXT",
                SecurityContextHolder.getContext()
        );

        /*
         * Send the patient to the normal dashboard.
         */
        response.sendRedirect("/dashboard");
    }
}