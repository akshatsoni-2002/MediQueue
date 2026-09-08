package com.example.mediqueue.controller;

import com.example.mediqueue.model.*;
import com.example.mediqueue.repository.*;
import com.example.mediqueue.service.NotificationService;
import com.example.mediqueue.service.PatientNotificationService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/patient")
public class PatientController {

    private final UserRepository users;
    private final DoctorRepository doctors;
    private final DepartmentRepository departments;
    private final AppointmentRepository appointments;
    private final AppointmentHistoryRepository history;
    private final NotificationService notifications;
    private final PatientNotificationService email;
    private final PaymentRepository payments;

    public PatientController(
            UserRepository users,
            DoctorRepository doctors,
            DepartmentRepository departments,
            AppointmentRepository appointments,
            AppointmentHistoryRepository history,
            NotificationService notifications,
            PatientNotificationService email,
            PaymentRepository payments) {

        this.users = users;
        this.doctors = doctors;
        this.departments = departments;
        this.appointments = appointments;
        this.history = history;
        this.notifications = notifications;
        this.email = email;
        this.payments = payments;
    }

    private User me(Authentication authentication) {
        return users.findByUserId(authentication.getName())
                .orElseThrow();
    }

    private Patient patient(Authentication authentication) {

        User user = me(authentication);

        if (user.getPatient() == null) {
            throw new IllegalStateException(
                    "Patient account is not linked to a patient profile."
            );
        }

        return user.getPatient();
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/dashboard";
    }

    @GetMapping("/doctors")
    public String doctors(Model model) {

        model.addAttribute(
                "doctors",
                doctors.findAll()
                        .stream()
                        .filter(Doctor::isActive)
                        .toList()
        );

        return "patient-doctors";
    }

    @GetMapping("/appointments")
    public String myAppointments(
            Authentication authentication,
            Model model) {

        Patient patient = patient(authentication);

        model.addAttribute("patient", patient);

        model.addAttribute(
                "appointments",
                appointments.findAll()
                        .stream()
                        .filter(appointment ->
                                appointment.getPatient() != null
                                && appointment.getPatient()
                                        .getId()
                                        .equals(patient.getId()))
                        .sorted((first, second) -> {

                            int dateComparison =
                                    second.getAppointmentDate()
                                            .compareTo(
                                                    first.getAppointmentDate()
                                            );

                            if (dateComparison != 0) {
                                return dateComparison;
                            }

                            LocalTime firstTime =
                                    first.getAppointmentTime() == null
                                            ? LocalTime.MAX
                                            : first.getAppointmentTime();

                            LocalTime secondTime =
                                    second.getAppointmentTime() == null
                                            ? LocalTime.MAX
                                            : second.getAppointmentTime();

                            return secondTime.compareTo(firstTime);
                        })
                        .toList()
        );

        return "patient-appointments";
    }

    @GetMapping("/book")
    public String book(Model model) {

        model.addAttribute(
                "doctors",
                doctors.findAll()
                        .stream()
                        .filter(Doctor::isActive)
                        .toList()
        );

        model.addAttribute(
                "departments",
                departments.findAll()
                        .stream()
                        .filter(Department::isActive)
                        .toList()
        );

        return "patient-book-appointment";
    }

    @PostMapping("/book")
    public String book(
            Authentication authentication,
            @RequestParam Long doctorId,
            @RequestParam Long departmentId,
            @RequestParam LocalDate appointmentDate,
            @RequestParam(required = false) LocalTime appointmentTime) {

        Patient patient = patient(authentication);

        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow();

        Department department = departments.findById(departmentId)
                .orElseThrow();

        int token =
                appointments
                        .findTopByDoctor_IdAndAppointmentDateOrderByTokenNumberDesc(
                                doctorId,
                                appointmentDate
                        )
                        .map(Appointment::getTokenNumber)
                        .orElse(0)
                        + 1;

        Appointment appointment =
                new Appointment(
                        patient,
                        doctor,
                        department,
                        appointmentDate,
                        token
                );

        appointment.setAppointmentTime(appointmentTime);

        appointments.save(appointment);

        String message =
                "Appointment booked with "
                + doctor.getFullName()
                + " on "
                + appointmentDate
                + (appointmentTime != null
                        ? " at " + appointmentTime
                        : "")
                + " (Token #"
                + token
                + ").";

        notifications.notifyPatient(
                patient.getId(),
                "APPOINTMENT_CONFIRMED",
                message
        );

        notifications.notifyDoctor(
                doctor.getId(),
                "PATIENT_APPOINTMENT",
                patient.getFullName()
                + " booked an appointment with you. "
                + message
        );

        notifications.notifyStaffAndAdmin(
                "PATIENT_APPOINTMENT",
                patient.getFullName()
                + " booked an appointment with "
                + doctor.getFullName()
                + ". "
                + message
        );

        email.appointmentUpdate(
                appointment,
                "MediQueue Appointment Confirmed",
                message
        );

        history.save(
                new AppointmentHistory(
                        appointment,
                        "PATIENT_BOOKED",
                        message,
                        authentication.getName()
                )
        );

        return "redirect:/patient/appointments?booked=true";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(
            Authentication authentication,
            @PathVariable Long id) {

        Patient patient = patient(authentication);

        Appointment appointment =
                appointments.findById(id)
                        .orElseThrow();

        if (appointment.getPatient() == null
                || !appointment.getPatient()
                        .getId()
                        .equals(patient.getId())) {

            return "redirect:/patient/appointments";
        }

        String status = appointment.getStatus();

        if (!"COMPLETED".equals(status)
                && !"CANCELLED".equals(status)) {

            appointment.setStatus("CANCELLED");

            appointments.save(appointment);

            String message =
                    "Patient "
                    + patient.getFullName()
                    + " cancelled the appointment with "
                    + appointment.getDoctor().getFullName()
                    + " on "
                    + appointment.getAppointmentDate()
                    + ".";

            notifications.notifyPatient(
                    patient.getId(),
                    "APPOINTMENT_CANCELLED",
                    message
            );

            notifications.notifyDoctor(
                    appointment.getDoctor().getId(),
                    "APPOINTMENT_CANCELLED",
                    message
            );

            notifications.notifyStaffAndAdmin(
                    "APPOINTMENT_CANCELLED",
                    message
            );

            email.appointmentUpdate(
                    appointment,
                    "MediQueue Appointment Cancelled",
                    message
            );

            history.save(
                    new AppointmentHistory(
                            appointment,
                            "PATIENT_CANCELLED",
                            message,
                            authentication.getName()
                    )
            );
        }

        return "redirect:/patient/appointments";
    }

    @PostMapping("/doctors/{id}/contact")
    public String contact(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String action) {

        Patient patient = patient(authentication);

        Doctor doctor = doctors.findById(id)
                .orElseThrow();

        String type =
                "CALL".equalsIgnoreCase(action)
                        ? "VIDEO_CALL_REQUEST"
                        : "CHAT_REQUEST";

        String message =
                patient.getFullName()
                + " requested a "
                + ("CALL".equalsIgnoreCase(action)
                        ? "video call"
                        : "chat")
                + " with you.";

        notifications.notifyDoctor(
                doctor.getId(),
                type,
                message
        );

        return "redirect:/patient/doctors?requested=" + action;
    }

    @GetMapping("/payments")
    public String payments(
            Authentication authentication,
            Model model) {

        Patient patient = patient(authentication);

        model.addAttribute("patient", patient);

        model.addAttribute(
                "appointments",
                appointments.findAll()
                        .stream()
                        .filter(appointment ->
                                appointment.getPatient() != null
                                && appointment.getPatient()
                                        .getId()
                                        .equals(patient.getId()))
                        .toList()
        );

        model.addAttribute(
                "payments",
                payments.findByPatient_IdOrderByCreatedAtDesc(
                        patient.getId()
                )
        );

        return "patient-payments";
    }

    @PostMapping("/payments")
    public String createPayment(
            Authentication authentication,
            @RequestParam(required = false) Long appointmentId,
            @RequestParam BigDecimal amount,
            @RequestParam String method,
            @RequestParam(required = false) String transactionReference) {

        Patient patient = patient(authentication);

        if (amount == null || amount.signum() <= 0) {
            return "redirect:/patient/payments?error=amount";
        }

        Payment payment = new Payment();

        payment.setPatient(patient);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setTransactionReference(transactionReference);
        payment.setStatus("PENDING");

        if (appointmentId != null) {

            appointments.findById(appointmentId)
                    .filter(appointment ->
                            appointment.getPatient() != null
                            && appointment.getPatient()
                                    .getId()
                                    .equals(patient.getId()))
                    .ifPresent(payment::setAppointment);
        }

        payments.save(payment);

        return "redirect:/patient/payments?submitted=true";
    }
}