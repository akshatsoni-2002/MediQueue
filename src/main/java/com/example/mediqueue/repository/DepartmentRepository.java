package com.example.mediqueue.repository;

import com.example.mediqueue.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}