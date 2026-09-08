package com.example.mediqueue.repository;
import com.example.mediqueue.model.AppointmentHistory; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory,Long>{List<AppointmentHistory> findByAppointment_IdOrderByChangedAtDesc(Long id);}
