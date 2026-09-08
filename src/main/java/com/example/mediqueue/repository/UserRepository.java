package com.example.mediqueue.repository;

import com.example.mediqueue.model.Role;
import com.example.mediqueue.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);
    boolean existsByUserId(String userId);
    List<User> findByRoleAndActiveTrue(Role role);
    List<User> findByDoctor_IdAndActiveTrue(Long doctorId);
    List<User> findByPatient_IdAndActiveTrue(Long patientId);
    Optional<User> findFirstByDoctor_IdAndActiveTrue(Long doctorId);
}
