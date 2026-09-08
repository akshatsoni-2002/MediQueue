package com.example.mediqueue.model;
import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="consultations") public class Consultation{
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;@OneToOne(optional=false) @JoinColumn(name="appointment_id",unique=true) Appointment appointment;@Column(length=5000) String notes;@Column(length=2000) String diagnosis;LocalDateTime createdAt=LocalDateTime.now();
public Consultation(){} public Long getId(){return id;}public Appointment getAppointment(){return appointment;}public void setAppointment(Appointment a){appointment=a;}public String getNotes(){return notes;}public void setNotes(String n){notes=n;}public String getDiagnosis(){return diagnosis;}public void setDiagnosis(String d){diagnosis=d;}public LocalDateTime getCreatedAt(){return createdAt;}}
