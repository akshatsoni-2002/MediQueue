package com.example.mediqueue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false, length = 20)
    private String gender;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String address;

    private boolean active = true;

    @Column(length = 500) private String healthConcern;
    @Column(length = 1200) private String knownConditions;
    @Column(length = 800) private String allergies;
    @Column(length = 100) private String emergencyContact;

    public Patient() {
    }

    public Patient(String fullName, int age, String gender,
                   String phone, String email, String address) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHealthConcern(){return healthConcern;} public void setHealthConcern(String v){healthConcern=v;}
    public String getKnownConditions(){return knownConditions;} public void setKnownConditions(String v){knownConditions=v;}
    public String getAllergies(){return allergies;} public void setAllergies(String v){allergies=v;}
    public String getEmergencyContact(){return emergencyContact;} public void setEmergencyContact(String v){emergencyContact=v;}

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}