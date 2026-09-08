package com.example.mediqueue.repository;
import com.example.mediqueue.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PaymentRepository extends JpaRepository<Payment,Long>{ List<Payment> findByPatient_IdOrderByCreatedAtDesc(Long patientId); }
