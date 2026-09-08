package com.example.mediqueue.repository;

import com.example.mediqueue.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmailIgnoreCase(String email);
}
