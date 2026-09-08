package com.example.mediqueue.controller;

import com.example.mediqueue.model.Doctor;
import com.example.mediqueue.model.Role;
import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.DoctorRepository;
import com.example.mediqueue.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String showUsers(Model model) {

        List<User> users = userRepository.findAll();

        List<Doctor> doctors = doctorRepository.findAll()
                .stream()
                .filter(Doctor::isActive)
                .toList();

        model.addAttribute("users", users);
        model.addAttribute("doctors", doctors);
        model.addAttribute("roles", List.of(Role.STAFF, Role.DOCTOR, Role.ADMIN));

        return "user-management";
    }

    @PostMapping("/create")
    public String createUser(
            @RequestParam String userId,
            @RequestParam String password,
            @RequestParam Role role,
            @RequestParam(required = false) Long doctorId) {

        userId = userId.trim();

        if (userId.isEmpty() || password.isEmpty()) {
            return "redirect:/admin/users?error=empty";
        }

        if (userRepository.existsByUserId(userId)) {
            return "redirect:/admin/users?error=exists";
        }

        Doctor doctor = null;

        if (role == Role.DOCTOR) {

            if (doctorId == null) {
                return "redirect:/admin/users?error=doctorrequired";
            }

            doctor = doctorRepository
                    .findById(doctorId)
                    .orElse(null);

            if (doctor == null) {
                return "redirect:/admin/users?error=doctorinvalid";
            }
        }

        if (role != Role.DOCTOR) {
            doctor = null;
        }

        User user = new User(
                userId,
                passwordEncoder.encode(password),
                role,
                doctor
        );

        userRepository.save(user);

        return "redirect:/admin/users?success=created";
    }

    @PostMapping("/{id}/toggle")
    public String toggleUser(@PathVariable Long id) {

        User user = userRepository
                .findById(id)
                .orElse(null);

        if (user == null) {
            return "redirect:/admin/users";
        }

        // ADMIN001 is the protected primary administrator account.
        if ("ADMIN001".equals(user.getUserId())) {
            return "redirect:/admin/users?error=protected";
        }

        user.setActive(!user.isActive());

        userRepository.save(user);

        return "redirect:/admin/users";
    }
}