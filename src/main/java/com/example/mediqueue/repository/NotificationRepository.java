package com.example.mediqueue.repository;
import com.example.mediqueue.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByRecipient_UserIdOrderByCreatedAtDesc(String userId);
    long countByRecipient_UserIdAndReadFalse(String userId);
}
