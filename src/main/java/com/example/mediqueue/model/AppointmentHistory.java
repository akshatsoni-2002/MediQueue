package com.example.mediqueue.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="appointment_history")
public class AppointmentHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="appointment_id") private Appointment appointment;
 @Column(nullable=false,length=50) private String action;
 @Column(length=1000) private String details;
 @Column(nullable=false) private LocalDateTime changedAt=LocalDateTime.now();
 @Column(length=50) private String changedBy;
 public AppointmentHistory(){} public AppointmentHistory(Appointment a,String action,String details,String by){this.appointment=a;this.action=action;this.details=details;this.changedBy=by;}
 public Long getId(){return id;} public Appointment getAppointment(){return appointment;} public String getAction(){return action;} public String getDetails(){return details;} public LocalDateTime getChangedAt(){return changedAt;} public String getChangedBy(){return changedBy;}
}
