package com.example.mediqueue.controller;

import com.example.mediqueue.model.Patient;
import com.example.mediqueue.model.Role;
import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.PatientRepository;
import com.example.mediqueue.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PatientAuthController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final boolean googleEnabled;

    public PatientAuthController(
            UserRepository userRepository,
            PatientRepository patientRepository,
            PasswordEncoder encoder,
            AuthenticationManager authenticationManager,
            @Value("${mediqueue.google.enabled:false}") boolean googleEnabled) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.googleEnabled = googleEnabled;
    }

    @GetMapping("/patient/login")
    public String login(Model model) {
        model.addAttribute("googleEnabled", googleEnabled);
        return "patient-login";
    }

    @PostMapping("/patient/login")
    public String authenticatePatient(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, password)
            );

            boolean patientRole = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_PATIENT".equals(a.getAuthority()));

            if (!patientRole) {
                model.addAttribute("error", "This account is not a patient account. Use the hospital staff / doctor login.");
                return "patient-login";
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            return "redirect:/dashboard";

        } catch (BadCredentialsException ex) {
            model.addAttribute("error", "Invalid patient email or password.");
            return "patient-login";
        } catch (Exception ex) {
            System.err.println("Patient login failed: " + ex.getMessage());
            model.addAttribute("error", "Patient login could not be completed. Please try again.");
            return "patient-login";
        }
    }

    @GetMapping("/patient/signup")
    public String signup(Model model) {
        model.addAttribute("googleEnabled", googleEnabled);
        return "patient-signup";
    }

    @PostMapping("/patient/signup")
    public String create(
            @RequestParam String fullName,
            @RequestParam int age,
            @RequestParam String gender,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByUserId(normalizedEmail)) {
            model.addAttribute("error", "An account already exists for this email.");
            model.addAttribute("googleEnabled", googleEnabled);
            return "patient-signup";
        }

        Patient patient = patientRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    Patient created = new Patient(
                            fullName.trim(),
                            age,
                            gender,
                            phone.trim(),
                            normalizedEmail,
                            ""
                    );
                    return patientRepository.save(created);
                });

        patient.setFullName(fullName.trim());
        patient.setAge(age);
        patient.setGender(gender);
        patient.setPhone(phone.trim());
        patient.setEmail(normalizedEmail);
        patient.setActive(true);
        patientRepository.save(patient);

        User user = new User(
                normalizedEmail,
                encoder.encode(password),
                Role.PATIENT,
                null
        );
        user.setPatient(patient);
        user.setDisplayName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setMobile(phone.trim());
        user.setActive(true);
        userRepository.save(user);

        return "redirect:/patient/login?registered=true";
    }
}
