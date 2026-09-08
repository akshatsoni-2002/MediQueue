package com.example.mediqueue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String specialization;

    @Column(length = 15)
    private String phone;

    @Column(length = 100, unique = true)
    private String email;

    private int averageConsultationMinutes = 15;
    @Column(length = 1200)
    private String bio;

    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public Doctor() {
    }

    public Doctor(String fullName, String specialization, String phone,
                  String email, int averageConsultationMinutes, Department department) {
        this.fullName = fullName;
        this.specialization = specialization;
        this.phone = phone;
        this.email = email;
        this.averageConsultationMinutes = averageConsultationMinutes;
        this.department = department;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAverageConsultationMinutes() { return averageConsultationMinutes; }
    public void setAverageConsultationMinutes(int averageConsultationMinutes) {
        this.averageConsultationMinutes = averageConsultationMinutes;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
}