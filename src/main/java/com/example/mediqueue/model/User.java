package com.example.mediqueue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String userId;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(length = 120) private String displayName;
    @Column(length = 150) private String email;
    @Column(length = 20) private String mobile;
    @Column(length = 1200) private String bio;
    @Lob @Column(name = "profile_photo") private byte[] profilePhoto;
    @Column(name = "profile_photo_type", length = 100) private String profilePhotoType;

    public User() {}

    public User(String userId, String password, Role role, Doctor doctor) {
        this.userId=userId; this.password=password; this.role=role; this.doctor=doctor; this.active=true;
    }

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public Role getRole(){return role;} public void setRole(Role v){role=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Doctor getDoctor(){return doctor;} public void setDoctor(Doctor v){doctor=v;}
    public Patient getPatient(){return patient;} public void setPatient(Patient v){patient=v;}
    public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getMobile(){return mobile;} public void setMobile(String v){mobile=v;}
    public String getBio(){return bio;} public void setBio(String v){bio=v;}
    public byte[] getProfilePhoto(){return profilePhoto;} public void setProfilePhoto(byte[] v){profilePhoto=v;}
    public String getProfilePhotoType(){return profilePhotoType;} public void setProfilePhotoType(String v){profilePhotoType=v;}
}
