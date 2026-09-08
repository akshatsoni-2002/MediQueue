package com.example.mediqueue.controller;

import com.example.mediqueue.model.Appointment;
import com.example.mediqueue.model.Doctor;
import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.AppointmentHistoryRepository;
import com.example.mediqueue.repository.AppointmentRepository;
import com.example.mediqueue.repository.UserRepository;
import com.example.mediqueue.service.NotificationService;
import com.example.mediqueue.service.PatientNotificationService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

@Controller
@RequestMapping("/doctor/appointments")
public class DoctorAppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final PatientNotificationService patientNotificationService;

    public DoctorAppointmentController(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AppointmentHistoryRepository historyRepository,
            NotificationService notificationService,
            PatientNotificationService patientNotificationService) {

        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
        this.patientNotificationService = patientNotificationService;
    }

    private Doctor getLoggedInDoctor(Authentication authentication) {

        return userRepository
                .findByUserId(authentication.getName())
                .map(User::getDoctor)
                .orElse(null);
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {

        Doctor doctor = getLoggedInDoctor(authentication);

        if (doctor == null) {
            model.addAttribute(
                    "error",
                    "Doctor account is not linked to a doctor profile."
            );

            return "doctor-appointments";
        }

        var appointments = appointmentRepository.findAll()
                .stream()
                .filter(a ->
                        a.getDoctor() != null
                                && a.getDoctor().getId().equals(doctor.getId()))
                .sorted(
                        Comparator
                                .comparing(Appointment::getAppointmentDate)
                                .thenComparing(a ->
                                        a.getAppointmentTime() == null
                                                ? LocalTime.MAX
                                                : a.getAppointmentTime())
                )
                .toList();

        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", appointments);

        return "doctor-appointments";
    }

    @GetMapping("/edit/{id}")
    public String edit(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        Doctor doctor = getLoggedInDoctor(authentication);

        Appointment appointment =
                appointmentRepository.findById(id).orElseThrow();

        if (doctor == null
                || appointment.getDoctor() == null
                || !appointment.getDoctor().getId().equals(doctor.getId())) {

            return "redirect:/doctor/appointments";
        }

        // Only appointments that have not already been completed/cancelled
        // can be rescheduled.
        if ("COMPLETED".equals(appointment.getStatus())
                || "CANCELLED".equals(appointment.getStatus())) {

            return "redirect:/doctor/appointments";
        }

        model.addAttribute("appointment", appointment);

        return "doctor-edit-appointment";
    }

    @PostMapping("/edit/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam LocalDate appointmentDate,
            @RequestParam(required = false) LocalTime appointmentTime,
            Authentication authentication) {

        Doctor doctor = getLoggedInDoctor(authentication);

        Appointment appointment =
                appointmentRepository.findById(id).orElseThrow();

        if (doctor == null
                || appointment.getDoctor() == null
                || !appointment.getDoctor().getId().equals(doctor.getId())) {

            return "redirect:/doctor/appointments";
        }

        if ("COMPLETED".equals(appointment.getStatus())
                || "CANCELLED".equals(appointment.getStatus())) {

            return "redirect:/doctor/appointments";
        }

        boolean changed =
                !appointmentDate.equals(appointment.getAppointmentDate())
                        || (appointment.getAppointmentTime() == null
                        ? appointmentTime != null
                        : !appointment.getAppointmentTime().equals(appointmentTime));

        if (changed) {

            String oldDateTime =
                    appointment.getAppointmentDate()
                            + (appointment.getAppointmentTime() != null
                            ? " " + appointment.getAppointmentTime()
                            : "");

            // If the doctor moves the appointment to another date,
            // generate the next token for that date.
            if (!appointmentDate.equals(appointment.getAppointmentDate())) {

                int token =
                        appointmentRepository
                                .findTopByDoctor_IdAndAppointmentDateOrderByTokenNumberDesc(
                                        doctor.getId(),
                                        appointmentDate)
                                .map(Appointment::getTokenNumber)
                                .orElse(0) + 1;

                appointment.setTokenNumber(token);
            }

            appointment.setAppointmentDate(appointmentDate);
            appointment.setAppointmentTime(appointmentTime);

            appointmentRepository.save(appointment);

            String message =
                    "Dr. " + doctor.getFullName()
                            + " rescheduled the appointment for "
                            + appointment.getPatient().getFullName()
                            + " from "
                            + oldDateTime
                            + " to "
                            + appointmentDate
                            + (appointmentTime != null
                            ? " at " + appointmentTime
                            : "")
                            + " (Token #"
                            + appointment.getTokenNumber()
                            + ").";

            // Notify staff and admin.
            notificationService.notifyStaffAndAdmin(
                    "APPOINTMENT_UPDATED",
                    message
            );

            // Notify patient.
            patientNotificationService.appointmentUpdate(
                    appointment,
                    "MediQueue Appointment Rescheduled",
                    message
            );

            // Save history.
            historyRepository.save(
                    new com.example.mediqueue.model.AppointmentHistory(
                            appointment,
                            "DOCTOR_RESCHEDULED",
                            message,
                            authentication.getName()
                    )
            );
        }

        return "redirect:/doctor/appointments";
    }

    @PostMapping("/cancel/{id}")
    public String cancel(
            @PathVariable Long id,
            Authentication authentication) {

        Doctor doctor = getLoggedInDoctor(authentication);

        Appointment appointment =
                appointmentRepository.findById(id).orElseThrow();

        // Security: doctor can only cancel their own appointments.
        if (doctor == null
                || appointment.getDoctor() == null
                || !appointment.getDoctor().getId().equals(doctor.getId())) {

            return "redirect:/doctor/appointments";
        }

        // Completed or already cancelled appointments cannot be cancelled again.
        if ("COMPLETED".equals(appointment.getStatus())
                || "CANCELLED".equals(appointment.getStatus())) {

            return "redirect:/doctor/appointments";
        }

        appointment.setStatus("CANCELLED");

        appointmentRepository.save(appointment);

        String message =
                "Dr. " + doctor.getFullName()
                        + " cancelled the appointment for "
                        + appointment.getPatient().getFullName()
                        + " on "
                        + appointment.getAppointmentDate()
                        + (appointment.getAppointmentTime() != null
                        ? " at " + appointment.getAppointmentTime()
                        : "")
                        + " (Token #"
                        + appointment.getTokenNumber()
                        + ").";

        // Notify staff and admin.
        notificationService.notifyStaffAndAdmin(
                "APPOINTMENT_CANCELLED",
                message
        );

        // Notify patient.
        patientNotificationService.appointmentUpdate(
                appointment,
                "MediQueue Appointment Cancelled",
                message
        );

        // Save appointment history.
        historyRepository.save(
                new com.example.mediqueue.model.AppointmentHistory(
                        appointment,
                        "DOCTOR_CANCELLED",
                        message,
                        authentication.getName()
                )
        );

        return "redirect:/doctor/appointments";
    }
}