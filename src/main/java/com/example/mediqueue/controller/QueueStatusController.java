package com.example.mediqueue.controller;

import com.example.mediqueue.model.Appointment;
import com.example.mediqueue.model.Doctor;
import com.example.mediqueue.repository.AppointmentRepository;
import com.example.mediqueue.repository.DoctorRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/queue")
public class QueueStatusController {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public QueueStatusController(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository) {

        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
    }

    // =========================
    // SHOW QUEUE STATUS PAGE
    // =========================

    @GetMapping("/status")
    public String showQueueStatusPage(Model model) {

        List<Doctor> doctors = doctorRepository.findAll()
                .stream()
                .filter(Doctor::isActive)
                .sorted(
                        Comparator.comparing(
                                Doctor::getFullName
                        )
                )
                .toList();

        model.addAttribute("doctors", doctors);
        model.addAttribute("today", LocalDate.now());

        return "queue-status";
    }

    // =========================
    // CHECK QUEUE STATUS
    // =========================

    @PostMapping("/status")
    public String checkQueueStatus(
            @RequestParam Long doctorId,
            @RequestParam LocalDate appointmentDate,
            @RequestParam int tokenNumber,
            Model model) {

        // =========================
        // LOAD ACTIVE DOCTORS
        // =========================

        List<Doctor> doctors = doctorRepository.findAll()
                .stream()
                .filter(Doctor::isActive)
                .sorted(
                        Comparator.comparing(
                                Doctor::getFullName
                        )
                )
                .toList();

        model.addAttribute("doctors", doctors);
        model.addAttribute("today", LocalDate.now());

        // Keep entered values on page
        model.addAttribute(
                "selectedDoctorId",
                doctorId
        );

        model.addAttribute(
                "selectedDate",
                appointmentDate
        );

        model.addAttribute(
                "enteredToken",
                tokenNumber
        );

        // =========================
        // FIND DOCTOR
        // =========================

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElse(null);

        if (doctor == null) {

            model.addAttribute(
                    "error",
                    "Doctor not found."
            );

            return "queue-status";
        }

        // =========================
        // FIND APPOINTMENT
        // =========================

        Appointment appointment =
                appointmentRepository.findAll()
                        .stream()
                        .filter(a ->
                                a.getDoctor() != null &&
                                a.getDoctor()
                                        .getId()
                                        .equals(doctorId) &&

                                a.getAppointmentDate() != null &&
                                a.getAppointmentDate()
                                        .equals(appointmentDate) &&

                                a.getTokenNumber() == tokenNumber
                        )
                        .findFirst()
                        .orElse(null);

        if (appointment == null) {

            model.addAttribute(
                    "error",
                    "No appointment found for this doctor, date and token number."
            );

            return "queue-status";
        }

        // =========================
        // ADD APPOINTMENT
        // =========================

        model.addAttribute(
                "appointment",
                appointment
        );

        // =========================
        // ALL ACTIVE QUEUE
        // =========================

        List<Appointment> queueAppointments =
                appointmentRepository.findAll()
                        .stream()
                        .filter(a ->
                                a.getDoctor() != null &&
                                a.getDoctor()
                                        .getId()
                                        .equals(doctorId) &&

                                a.getAppointmentDate() != null &&
                                a.getAppointmentDate()
                                        .equals(appointmentDate) &&

                                !"CANCELLED".equals(
                                        a.getStatus()
                                )
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        Appointment::getTokenNumber
                                )
                        )
                        .toList();

        // =========================
        // CURRENTLY SERVING
        // =========================

        Appointment currentAppointment =
                queueAppointments.stream()
                        .filter(a ->
                                "CALLED".equals(a.getStatus()) ||
                                "IN_CONSULTATION".equals(
                                        a.getStatus()
                                )
                        )
                        .findFirst()
                        .orElse(null);

        model.addAttribute(
                "currentAppointment",
                currentAppointment
        );

        // =========================
        // WAITING PATIENTS AHEAD
        // =========================

        if ("WAITING".equals(
                appointment.getStatus())) {

            int patientsAhead =
                    (int) queueAppointments.stream()
                            .filter(a ->
                                    "WAITING".equals(
                                            a.getStatus()
                                    ) &&

                                    a.getTokenNumber()
                                            < tokenNumber
                            )
                            .count();

            model.addAttribute(
                    "patientsAhead",
                    patientsAhead
            );

            // =========================
            // ESTIMATED WAIT
            // =========================

            int consultationMinutes =
                    Math.max(
                            doctor.getAverageConsultationMinutes(),
                            1
                    );

            int estimatedWaitMinutes =
                    patientsAhead *
                    consultationMinutes;

            model.addAttribute(
                    "estimatedWaitMinutes",
                    estimatedWaitMinutes
            );
        }

        // =========================
        // CALLED / CONSULTATION
        // =========================

        if ("CALLED".equals(
                appointment.getStatus()) ||
            "IN_CONSULTATION".equals(
                appointment.getStatus())) {

            model.addAttribute(
                    "patientsAhead",
                    0
            );

            model.addAttribute(
                    "estimatedWaitMinutes",
                    0
            );
        }

        // =========================
        // COMPLETED
        // =========================

        if ("COMPLETED".equals(
                appointment.getStatus())) {

            model.addAttribute(
                    "patientsAhead",
                    0
            );

            model.addAttribute(
                    "estimatedWaitMinutes",
                    0
            );
        }

        return "queue-status";
    }
}