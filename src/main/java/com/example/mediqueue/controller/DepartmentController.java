package com.example.mediqueue.controller;

import com.example.mediqueue.model.Department;
import com.example.mediqueue.repository.DepartmentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public String showDepartments(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        return "departments";
    }

    @PostMapping("/add")
    public String addDepartment(
            @RequestParam String name,
            @RequestParam(required = false) String description) {

        Department department = new Department(name, description);
        departmentRepository.save(department);

        return "redirect:/admin/departments";
    }

    @GetMapping("/edit/{id}")
    public String showEditDepartment(@PathVariable Long id, Model model) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        model.addAttribute("department", department);

        return "edit-department";
    }

    @PostMapping("/edit/{id}")
    public String updateDepartment(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        department.setName(name);
        department.setDescription(description);

        departmentRepository.save(department);

        return "redirect:/admin/departments";
    }

    @GetMapping("/toggle/{id}")
    public String toggleDepartment(@PathVariable Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        department.setActive(!department.isActive());
        departmentRepository.save(department);

        return "redirect:/admin/departments";
    }

    @GetMapping("/delete/{id}")
    public String deleteDepartment(@PathVariable Long id) {
        departmentRepository.deleteById(id);
        return "redirect:/admin/departments";
    }
}