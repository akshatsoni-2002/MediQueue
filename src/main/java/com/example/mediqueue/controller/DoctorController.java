package com.example.mediqueue.controller;

import com.example.mediqueue.model.Department;
import com.example.mediqueue.model.Doctor;
import com.example.mediqueue.repository.DepartmentRepository;
import com.example.mediqueue.repository.DoctorRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    public DoctorController(DoctorRepository doctorRepository,
                             DepartmentRepository departmentRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public String showDoctors(Model model) {
        model.addAttribute("doctors", doctorRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());

        return "doctors";
    }

    @PostMapping("/add")
    public String addDoctor(
            @RequestParam String fullName,
            @RequestParam String specialization,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam int averageConsultationMinutes,
            @RequestParam Long departmentId) {

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        Doctor doctor = new Doctor(
                fullName,
                specialization,
                phone,
                email,
                averageConsultationMinutes,
                department
        );

        doctorRepository.save(doctor);

        return "redirect:/admin/doctors";
    }

    @GetMapping("/edit/{id}")
    public String showEditDoctor(@PathVariable Long id, Model model) {

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        model.addAttribute("doctor", doctor);
        model.addAttribute("departments", departmentRepository.findAll());

        return "edit-doctor";
    }

    @PostMapping("/edit/{id}")
    public String updateDoctor(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String specialization,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam int averageConsultationMinutes,
            @RequestParam Long departmentId) {

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        doctor.setFullName(fullName);
        doctor.setSpecialization(specialization);
        doctor.setPhone(phone);
        doctor.setEmail(email);
        doctor.setAverageConsultationMinutes(averageConsultationMinutes);
        doctor.setDepartment(department);

        doctorRepository.save(doctor);

        return "redirect:/admin/doctors";
    }

    @GetMapping("/toggle/{id}")
    public String toggleDoctor(@PathVariable Long id) {

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        doctor.setActive(!doctor.isActive());
        doctorRepository.save(doctor);

        return "redirect:/admin/doctors";
    }

    @GetMapping("/delete/{id}")
    public String deleteDoctor(@PathVariable Long id) {

        doctorRepository.deleteById(id);

        return "redirect:/admin/doctors";
    }
}