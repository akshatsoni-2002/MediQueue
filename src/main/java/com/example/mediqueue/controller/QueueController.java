package com.example.mediqueue.controller;

import com.example.mediqueue.model.Appointment;
import com.example.mediqueue.model.Doctor;
import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.AppointmentRepository;
import com.example.mediqueue.repository.DoctorRepository;
import com.example.mediqueue.repository.UserRepository;
import com.example.mediqueue.service.NotificationService;
import com.example.mediqueue.service.PatientNotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/doctor/queue")
public class QueueController {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final NotificationService notifications;
    private final PatientNotificationService patientNotifications;

    public QueueController(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            NotificationService notifications,
            PatientNotificationService patientNotifications) {

        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.notifications = notifications;
        this.patientNotifications = patientNotifications;
    }

    // =========================================================
    // SHOW LIVE QUEUE
    // =========================================================

    @GetMapping
    public String showQueue(
            @RequestParam(required = false) Long doctorId,
            Authentication authentication,
            Model model) {

        boolean doctorUser = isDoctor(authentication);

        model.addAttribute("isDoctor", doctorUser);
        model.addAttribute("isStaffOrAdmin", !doctorUser);
        model.addAttribute("today", LocalDate.now());

        // =====================================================
        // DOCTOR LOGIN
        // =====================================================

        if (doctorUser) {

            Doctor loggedInDoctor =
                    getLoggedInDoctor(authentication);

            if (loggedInDoctor == null) {

                model.addAttribute(
                        "error",
                        "Your doctor account is not linked to a doctor profile."
                );

                return "doctor-queue";
            }

            doctorId = loggedInDoctor.getId();

            model.addAttribute(
                    "selectedDoctorId",
                    doctorId
            );

            model.addAttribute(
                    "selectedDoctor",
                    loggedInDoctor
            );
        }

        // =====================================================
        // STAFF / ADMIN LOGIN
        // =====================================================

        else {

            List<Doctor> doctors =
                    doctorRepository.findAll()
                            .stream()
                            .filter(Doctor::isActive)
                            .sorted(
                                    Comparator.comparing(
                                            Doctor::getFullName
                                    )
                            )
                            .toList();

            model.addAttribute(
                    "doctors",
                    doctors
            );

            model.addAttribute(
                    "selectedDoctorId",
                    doctorId
            );

            if (doctorId == null) {
                return "doctor-queue";
            }
        }

        // IMPORTANT:
        // From this point onward use a FINAL variable.
        // This fixes the lambda errors in VS Code.

        final Long selectedDoctorId = doctorId;

        // =====================================================
        // FIND DOCTOR
        // =====================================================

        Doctor doctor =
                doctorRepository.findById(selectedDoctorId)
                        .orElse(null);

        if (doctor == null) {

            model.addAttribute(
                    "error",
                    "Doctor not found."
            );

            return "doctor-queue";
        }

        // =====================================================
        // TODAY'S APPOINTMENTS
        // =====================================================

        List<Appointment> todayAppointments =
                appointmentRepository.findAll()
                        .stream()
                        .filter(a ->
                                a.getDoctor() != null &&
                                a.getDoctor()
                                        .getId()
                                        .equals(selectedDoctorId) &&

                                a.getAppointmentDate() != null &&

                                a.getAppointmentDate()
                                        .equals(LocalDate.now()) &&

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

        // =====================================================
        // CURRENT APPOINTMENT
        // =====================================================

        Appointment currentAppointment =
                todayAppointments.stream()
                        .filter(a ->
                                "CALLED".equals(
                                        a.getStatus()
                                )
                                ||
                                "IN_CONSULTATION".equals(
                                        a.getStatus()
                                )
                        )
                        .findFirst()
                        .orElse(null);

        // =====================================================
        // WAITING APPOINTMENTS
        // =====================================================

        List<Appointment> waitingAppointments =
                todayAppointments.stream()
                        .filter(a ->
                                "WAITING".equals(
                                        a.getStatus()
                                )
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        Appointment::getTokenNumber
                                )
                        )
                        .toList();

        // =====================================================
        // COMPLETED APPOINTMENTS
        // =====================================================

        List<Appointment> completedAppointments =
                todayAppointments.stream()
                        .filter(a ->
                                "COMPLETED".equals(
                                        a.getStatus()
                                )
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        Appointment::getTokenNumber
                                )
                        )
                        .toList();

        // =====================================================
        // SEND DATA TO PAGE
        // =====================================================

        model.addAttribute(
                "selectedDoctor",
                doctor
        );

        model.addAttribute(
                "todayAppointments",
                todayAppointments
        );

        model.addAttribute(
                "currentAppointment",
                currentAppointment
        );

        model.addAttribute(
                "waitingAppointments",
                waitingAppointments
        );

        model.addAttribute(
                "completedAppointments",
                completedAppointments
        );

        return "doctor-queue";
    }

    // =========================================================
    // CALL NEXT PATIENT
    // =========================================================

    @PostMapping("/call-next")
    public String callNext(
            @RequestParam(required = false) Long doctorId,
            Authentication authentication) {

        final Long selectedDoctorId =
                resolveDoctorId(
                        doctorId,
                        authentication
                );

        if (selectedDoctorId == null) {
            return "redirect:/doctor/queue";
        }

        Doctor doctor =
                doctorRepository.findById(selectedDoctorId)
                        .orElse(null);

        if (doctor == null) {
            return "redirect:/doctor/queue";
        }

        // =====================================================
        // CHECK CURRENT PATIENT
        // =====================================================

        boolean patientAlreadyInProgress =
                appointmentRepository.findAll()
                        .stream()
                        .anyMatch(a ->

                                a.getDoctor() != null &&

                                a.getDoctor()
                                        .getId()
                                        .equals(selectedDoctorId) &&

                                a.getAppointmentDate() != null &&

                                a.getAppointmentDate()
                                        .equals(LocalDate.now()) &&

                                (
                                        "CALLED".equals(
                                                a.getStatus()
                                        )
                                        ||
                                        "IN_CONSULTATION".equals(
                                                a.getStatus()
                                        )
                                )
                        );

        if (patientAlreadyInProgress) {

            return redirectToDoctorQueue(
                    selectedDoctorId
            );
        }

        // =====================================================
        // FIND NEXT WAITING PATIENT
        // =====================================================

        Appointment nextAppointment =
                appointmentRepository.findAll()
                        .stream()
                        .filter(a ->

                                a.getDoctor() != null &&

                                a.getDoctor()
                                        .getId()
                                        .equals(selectedDoctorId) &&

                                a.getAppointmentDate() != null &&

                                a.getAppointmentDate()
                                        .equals(LocalDate.now()) &&

                                "WAITING".equals(
                                        a.getStatus()
                                )
                        )
                        .min(
                                Comparator.comparingInt(
                                        Appointment::getTokenNumber
                                )
                        )
                        .orElse(null);

        // =====================================================
        // CALL PATIENT
        // =====================================================

        if (nextAppointment != null) {

            nextAppointment.setStatus(
                    "CALLED"
            );

            appointmentRepository.save(
                    nextAppointment
            );

            String message = "Dr. " + doctor.getFullName() + " called Token #"
                    + nextAppointment.getTokenNumber() + " - "
                    + nextAppointment.getPatient().getFullName()
                    + ". Please send the patient to the consultation room.";
            notifications.notifyStaffAndAdmin("QUEUE_CALLED", message);
            notifications.notifyPatient(nextAppointment.getPatient().getId(), "QUEUE_CALLED", message);
            patientNotifications.appointmentUpdate(nextAppointment, "MediQueue - Your turn is being called", message);
        }

        return redirectToDoctorQueue(
                selectedDoctorId
        );
    }

    // =========================================================
    // START CONSULTATION
    // =========================================================

    @PostMapping("/start/{id}")
    public String startConsultation(
            @PathVariable Long id,
            Authentication authentication) {

        Appointment appointment =
                appointmentRepository.findById(id)
                        .orElse(null);

        if (appointment == null ||
            appointment.getDoctor() == null) {

            return "redirect:/doctor/queue";
        }

        if (!canAccessAppointment(
                appointment,
                authentication
        )) {

            return "redirect:/doctor/queue";
        }

        if ("CALLED".equals(
                appointment.getStatus()
        )) {

            appointment.setStatus(
                    "IN_CONSULTATION"
            );

            appointmentRepository.save(
                    appointment
            );
            notifications.notifyStaffAndAdmin("CONSULTATION_STARTED",
                    "Consultation started for Token #" + appointment.getTokenNumber() + " - " + appointment.getPatient().getFullName() + " by Dr. " + appointment.getDoctor().getFullName() + ".");
            notifications.notifyPatient(appointment.getPatient().getId(), "CONSULTATION_STARTED", "Your consultation has started with Dr. " + appointment.getDoctor().getFullName() + ".");
            patientNotifications.appointmentUpdate(appointment, "MediQueue - Consultation started", "Your consultation has started with Dr. " + appointment.getDoctor().getFullName() + ".");
        }

        return redirectToDoctorQueue(
                appointment.getDoctor().getId()
        );
    }

    // =========================================================
    // COMPLETE CONSULTATION
    // =========================================================

    @PostMapping("/complete/{id}")
    public String completeConsultation(
            @PathVariable Long id,
            Authentication authentication) {

        Appointment appointment =
                appointmentRepository.findById(id)
                        .orElse(null);

        if (appointment == null ||
            appointment.getDoctor() == null) {

            return "redirect:/doctor/queue";
        }

        if (!canAccessAppointment(
                appointment,
                authentication
        )) {

            return "redirect:/doctor/queue";
        }

        if ("IN_CONSULTATION".equals(
                appointment.getStatus()
        )) {

            appointment.setStatus(
                    "COMPLETED"
            );

            appointmentRepository.save(
                    appointment
            );
            notifications.notifyStaffAndAdmin("CONSULTATION_COMPLETED",
                    "Consultation completed for Token #" + appointment.getTokenNumber() + " - " + appointment.getPatient().getFullName() + " by Dr. " + appointment.getDoctor().getFullName() + ".");
            notifications.notifyPatient(appointment.getPatient().getId(), "CONSULTATION_COMPLETED", "Your consultation with Dr. " + appointment.getDoctor().getFullName() + " has been completed.");
            patientNotifications.appointmentUpdate(appointment, "MediQueue - Consultation completed", "Your consultation with Dr. " + appointment.getDoctor().getFullName() + " has been completed.");
        }

        return redirectToDoctorQueue(
                appointment.getDoctor().getId()
        );
    }

    // =========================================================
    // SKIP PATIENT
    // =========================================================

    @PostMapping("/skip/{id}")
    public String skipPatient(
            @PathVariable Long id,
            Authentication authentication) {

        Appointment appointment =
                appointmentRepository.findById(id)
                        .orElse(null);

        if (appointment == null ||
            appointment.getDoctor() == null) {

            return "redirect:/doctor/queue";
        }

        if (!canAccessAppointment(
                appointment,
                authentication
        )) {

            return "redirect:/doctor/queue";
        }

        if ("WAITING".equals(
                appointment.getStatus()
        )
        ||
        "CALLED".equals(
                appointment.getStatus()
        )) {

            appointment.setStatus(
                    "CANCELLED"
            );

            appointmentRepository.save(
                    appointment
            );
            notifications.notifyStaffAndAdmin("QUEUE_SKIPPED",
                    "Token #" + appointment.getTokenNumber() + " - " + appointment.getPatient().getFullName() + " was skipped by Dr. " + appointment.getDoctor().getFullName() + ".");
            notifications.notifyPatient(appointment.getPatient().getId(), "QUEUE_SKIPPED", "Your queue status was updated by Dr. " + appointment.getDoctor().getFullName() + ".");
            patientNotifications.appointmentUpdate(appointment, "MediQueue - Queue update", "Your queue status was updated by Dr. " + appointment.getDoctor().getFullName() + ". Please contact reception if needed.");
        }

        return redirectToDoctorQueue(
                appointment.getDoctor().getId()
        );
    }

    // =========================================================
    // CHECK WHETHER USER IS DOCTOR
    // =========================================================

    private boolean isDoctor(
            Authentication authentication) {

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(
                        authority ->
                                "ROLE_DOCTOR".equals(
                                        authority.getAuthority()
                                )
                );
    }

    // =========================================================
    // GET LOGGED-IN DOCTOR
    // =========================================================

    private Doctor getLoggedInDoctor(
            Authentication authentication) {

        User user =
                userRepository
                        .findByUserId(
                                authentication.getName()
                        )
                        .orElse(null);

        if (user == null) {
            return null;
        }

        return user.getDoctor();
    }

    // =========================================================
    // RESOLVE DOCTOR
    //
    // DOCTOR:
    // Always use linked doctor.
    //
    // STAFF / ADMIN:
    // Use selected doctor.
    // =========================================================

    private Long resolveDoctorId(
            Long requestedDoctorId,
            Authentication authentication) {

        if (isDoctor(authentication)) {

            Doctor loggedInDoctor =
                    getLoggedInDoctor(
                            authentication
                    );

            if (loggedInDoctor == null) {
                return null;
            }

            return loggedInDoctor.getId();
        }

        return requestedDoctorId;
    }

    // =========================================================
    // APPOINTMENT ACCESS CHECK
    // =========================================================

    private boolean canAccessAppointment(
            Appointment appointment,
            Authentication authentication) {

        // STAFF / ADMIN can access all queues.

        if (!isDoctor(authentication)) {
            return true;
        }

        // DOCTOR can access only own appointments.

        Doctor loggedInDoctor =
                getLoggedInDoctor(
                        authentication
                );

        if (loggedInDoctor == null) {
            return false;
        }

        return appointment
                .getDoctor()
                .getId()
                .equals(
                        loggedInDoctor.getId()
                );
    }

    // =========================================================
    // REDIRECT
    // =========================================================

    private String redirectToDoctorQueue(
            Long doctorId) {

        return "redirect:/doctor/queue?doctorId="
                + doctorId;
    }
}