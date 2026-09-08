package com.example.mediqueue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User recipient;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(nullable = false)
    private boolean read = false;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    public Notification() {}
    public Notification(User recipient, String type, String message) { this.recipient=recipient; this.type=type; this.message=message; this.createdAt=LocalDateTime.now(); }
    public Long getId(){return id;} public User getRecipient(){return recipient;} public void setRecipient(User v){recipient=v;}
    public String getType(){return type;} public void setType(String v){type=v;} public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public boolean isRead(){return read;} public void setRead(boolean v){read=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
