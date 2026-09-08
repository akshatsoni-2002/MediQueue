package com.example.mediqueue.controller;

import com.example.mediqueue.model.Appointment;
import com.example.mediqueue.model.Doctor;
import com.example.mediqueue.model.Patient;
import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.AppointmentRepository;
import com.example.mediqueue.repository.DepartmentRepository;
import com.example.mediqueue.repository.DoctorRepository;
import com.example.mediqueue.repository.PatientRepository;
import com.example.mediqueue.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DepartmentRepository departmentRepository;

    public DashboardController(
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        String role = authentication.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .orElse("");

        model.addAttribute("userId", authentication.getName());
        model.addAttribute("role", role);

        /*
         * Dashboard metrics are deliberately loaded defensively. A missing
         * optional table/column must not turn a successful login into a 500.
         * No data is deleted or reset here.
         */
        List<Appointment> allAppointments = List.of();
        long activeDoctors = 0;
        long activePatients = 0;
        long departmentCount = 0;
        long activeUserCount = 0;

        try {
            allAppointments = appointmentRepository.findAll();
        } catch (Exception ex) {
            System.err.println("MediQueue dashboard: appointments could not be loaded: " + ex.getMessage());
        }

        try {
            activeDoctors = doctorRepository.findAll()
                    .stream()
                    .filter(Doctor::isActive)
                    .count();
        } catch (Exception ex) {
            System.err.println("MediQueue dashboard: doctors could not be loaded: " + ex.getMessage());
        }

        try {
            activePatients = patientRepository.findAll()
                    .stream()
                    .filter(Patient::isActive)
                    .count();
        } catch (Exception ex) {
            System.err.println("MediQueue dashboard: patients could not be loaded: " + ex.getMessage());
        }

        try {
            departmentCount = departmentRepository.count();
        } catch (Exception ex) {
            System.err.println("MediQueue dashboard: departments could not be loaded: " + ex.getMessage());
        }

        try {
            activeUserCount = userRepository.findAll()
                    .stream()
                    .filter(User::isActive)
                    .count();
        } catch (Exception ex) {
            System.err.println("MediQueue dashboard: users could not be loaded: " + ex.getMessage());
        }

        LocalDate today = LocalDate.now();

        List<Appointment> todayAppointments = allAppointments.stream()
                .filter(a -> a != null && today.equals(a.getAppointmentDate()))
                .sorted(Comparator.comparingInt(Appointment::getTokenNumber)
                        .thenComparing(
                                a -> a.getAppointmentTime() == null
                                        ? LocalTime.MAX
                                        : a.getAppointmentTime()
                        ))
                .toList();

        long waiting = countStatus(todayAppointments, "WAITING");
        long inConsultation = countStatus(todayAppointments, "IN_CONSULTATION");
        long completed = countStatus(todayAppointments, "COMPLETED");

        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("todayCount", todayAppointments.size());
        model.addAttribute("waitingCount", waiting);
        model.addAttribute("consultationCount", inConsultation);
        model.addAttribute("completedCount", completed);
        model.addAttribute("activeDoctorCount", activeDoctors);
        model.addAttribute("activePatientCount", activePatients);
        model.addAttribute("departmentCount", departmentCount);
        model.addAttribute("activeUserCount", activeUserCount);

        if ("ROLE_ADMIN".equals(role)) {
            return "admin-dashboard";
        }

        if ("ROLE_STAFF".equals(role)) {
            return "staff-dashboard";
        }

        if ("ROLE_DOCTOR".equals(role)) {
            return doctorDashboard(authentication, model, todayAppointments);
        }

        if ("ROLE_PATIENT".equals(role)) {
            return patientDashboard(authentication, model, activeDoctors, allAppointments);
        }

        return "dashboard";
    }

    private String doctorDashboard(
            Authentication authentication,
            Model model,
            List<Appointment> todayAppointments) {

        User user = userRepository
                .findByUserId(authentication.getName())
                .orElse(null);

        Doctor doctor = user == null ? null : user.getDoctor();

        model.addAttribute("doctor", doctor);

        List<Appointment> doctorAppointments = doctor == null
                ? List.of()
                : todayAppointments.stream()
                        .filter(a -> a.getDoctor() != null
                                && a.getDoctor().getId() != null
                                && a.getDoctor().getId().equals(doctor.getId()))
                        .toList();

        model.addAttribute("doctorTodayAppointments", doctorAppointments);
        model.addAttribute("doctorTodayCount", doctorAppointments.size());
        model.addAttribute(
                "doctorWaitingCount",
                countStatus(doctorAppointments, "WAITING")
        );
        model.addAttribute(
                "doctorCompletedCount",
                countStatus(doctorAppointments, "COMPLETED")
        );
        model.addAttribute(
                "doctorConsultationCount",
                countStatus(doctorAppointments, "IN_CONSULTATION")
        );

        Appointment next = doctorAppointments.stream()
                .filter(a -> a.getStatus() != null
                        && !"COMPLETED".equals(a.getStatus())
                        && !"CANCELLED".equals(a.getStatus()))
                .min(Comparator.comparingInt(Appointment::getTokenNumber)
                        .thenComparing(
                                a -> a.getAppointmentTime() == null
                                        ? LocalTime.MAX
                                        : a.getAppointmentTime()
                        ))
                .orElse(null);

        model.addAttribute("nextDoctorAppointment", next);

        return "doctor-dashboard";
    }

    private String patientDashboard(
            Authentication authentication,
            Model model,
            long activeDoctors,
            List<Appointment> allAppointments) {

        User user = userRepository
                .findByUserId(authentication.getName())
                .orElse(null);

        Patient patient = user == null ? null : user.getPatient();

        List<Appointment> patientAppointments = patient == null
                ? List.of()
                : allAppointments.stream()
                        .filter(a -> a != null
                                && a.getPatient() != null
                                && a.getPatient().getId() != null
                                && a.getPatient().getId().equals(patient.getId())
                                && !"CANCELLED".equals(a.getStatus()))
                        .sorted(
                                Comparator.comparing(
                                        Appointment::getAppointmentDate,
                                        Comparator.nullsLast(Comparator.naturalOrder())
                                ).thenComparing(
                                        a -> a.getAppointmentTime() == null
                                                ? LocalTime.MAX
                                                : a.getAppointmentTime()
                                )
                        )
                        .toList();

        model.addAttribute("appointments", patientAppointments);
        model.addAttribute(
                "nextAppointment",
                patientAppointments.isEmpty()
                        ? null
                        : patientAppointments.get(0)
        );
        model.addAttribute("doctorCount", activeDoctors);

        String concern = patient == null
                ? null
                : patient.getHealthConcern();

        String summary = patient == null
                ? ""
                : "Conditions: "
                    + (isBlank(patient.getKnownConditions())
                        ? "Not added"
                        : patient.getKnownConditions())
                    + " · Allergies: "
                    + (isBlank(patient.getAllergies())
                        ? "Not added"
                        : patient.getAllergies());

        model.addAttribute("patientHealthConcern", concern);
        model.addAttribute("patientHealthSummary", summary);

        return "patient-dashboard";
    }

    private long countStatus(
            List<Appointment> appointments,
            String status) {
        return appointments.stream()
                .filter(a -> status.equals(a.getStatus()))
                .count();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
