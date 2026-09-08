package com.example.mediqueue.repository;
import com.example.mediqueue.model.Appointment;import org.springframework.data.jpa.repository.JpaRepository;import java.time.LocalDate;import java.util.Optional;
public interface AppointmentRepository extends JpaRepository<Appointment,Long>{Optional<Appointment> findTopByDoctor_IdAndAppointmentDateOrderByTokenNumberDesc(Long doctorId,LocalDate appointmentDate);}
