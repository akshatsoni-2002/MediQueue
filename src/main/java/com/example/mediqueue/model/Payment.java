package com.example.mediqueue.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="payments")
public class Payment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="patient_id") private Patient patient;
 @ManyToOne @JoinColumn(name="appointment_id") private Appointment appointment;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=30) private String method;
 @Column(nullable=false,length=30) private String status="PENDING";
 @Column(length=100) private String transactionReference;
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 public Payment(){}
 public Long getId(){return id;} public Patient getPatient(){return patient;} public void setPatient(Patient v){patient=v;} public Appointment getAppointment(){return appointment;} public void setAppointment(Appointment v){appointment=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public String getMethod(){return method;} public void setMethod(String v){method=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getTransactionReference(){return transactionReference;} public void setTransactionReference(String v){transactionReference=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
