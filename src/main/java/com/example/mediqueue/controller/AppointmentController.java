package com.example.mediqueue.controller;

import com.example.mediqueue.model.*;
import com.example.mediqueue.repository.*;
import com.example.mediqueue.service.NotificationService;
import com.example.mediqueue.service.PatientNotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/appointments")
public class AppointmentController {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final NotificationService notifications;
    private final PatientNotificationService patientNotifications;
    public AppointmentController(AppointmentRepository a,PatientRepository p,DoctorRepository d,DepartmentRepository dep,AppointmentHistoryRepository h,NotificationService n,PatientNotificationService pn){appointmentRepository=a;patientRepository=p;doctorRepository=d;departmentRepository=dep;historyRepository=h;notifications=n;patientNotifications=pn;}

    @GetMapping public String showAppointments(Model model){model.addAttribute("appointments",appointmentRepository.findAll());model.addAttribute("patients",patientRepository.findAll());model.addAttribute("doctors",doctorRepository.findAll());model.addAttribute("departments",departmentRepository.findAll());return "appointments";}

    @PostMapping("/add") public String addAppointment(@RequestParam Long patientId,@RequestParam Long doctorId,@RequestParam Long departmentId,@RequestParam LocalDate appointmentDate,@RequestParam(required=false) LocalTime appointmentTime,Authentication auth){
        Patient p=patientRepository.findById(patientId).orElseThrow(); Doctor d=doctorRepository.findById(doctorId).orElseThrow(); Department dep=departmentRepository.findById(departmentId).orElseThrow();
        int token=appointmentRepository.findTopByDoctor_IdAndAppointmentDateOrderByTokenNumberDesc(doctorId,appointmentDate).map(Appointment::getTokenNumber).orElse(0)+1;
        Appointment a=new Appointment(p,d,dep,appointmentDate,token);a.setAppointmentTime(appointmentTime);appointmentRepository.save(a);
        String msg="New appointment: "+p.getFullName()+" with "+d.getFullName()+" on "+appointmentDate+(appointmentTime!=null?" at "+appointmentTime:"")+" (Token #"+token+").";
        notifications.notifyDoctor(d.getId(),"APPOINTMENT",msg); notifications.notifyStaffAndAdmin("APPOINTMENT",msg); patientNotifications.appointmentUpdate(a,"MediQueue appointment confirmed",msg);
        historyRepository.save(new AppointmentHistory(a,"CREATED",msg,auth.getName())); return "redirect:/admin/appointments";
    }

    @GetMapping("/edit/{id}") public String edit(@PathVariable Long id,Model model){Appointment a=appointmentRepository.findById(id).orElseThrow();model.addAttribute("appointment",a);model.addAttribute("patients",patientRepository.findAll());model.addAttribute("doctors",doctorRepository.findAll());model.addAttribute("departments",departmentRepository.findAll());return "edit-appointment";}

    @PostMapping("/edit/{id}") public String update(@PathVariable Long id,@RequestParam Long patientId,@RequestParam Long doctorId,@RequestParam Long departmentId,@RequestParam LocalDate appointmentDate,@RequestParam(required=false) LocalTime appointmentTime,Authentication auth){
        Appointment a=appointmentRepository.findById(id).orElseThrow();Patient p=patientRepository.findById(patientId).orElseThrow();Doctor d=doctorRepository.findById(doctorId).orElseThrow();Department dep=departmentRepository.findById(departmentId).orElseThrow();
        String old=""+a.getAppointmentDate()+(a.getAppointmentTime()!=null?" "+a.getAppointmentTime():""); boolean doctorChanged=a.getDoctor()==null||!a.getDoctor().getId().equals(doctorId); boolean dateChanged=!appointmentDate.equals(a.getAppointmentDate()); boolean timeChanged=(a.getAppointmentTime()==null?appointmentTime!=null:!a.getAppointmentTime().equals(appointmentTime));
        a.setPatient(p);a.setDoctor(d);a.setDepartment(dep);a.setAppointmentDate(appointmentDate);a.setAppointmentTime(appointmentTime);
        if(doctorChanged||dateChanged){int token=appointmentRepository.findTopByDoctor_IdAndAppointmentDateOrderByTokenNumberDesc(doctorId,appointmentDate).map(Appointment::getTokenNumber).orElse(0)+1;a.setTokenNumber(token);} appointmentRepository.save(a);
        if(doctorChanged||dateChanged||timeChanged){String now="Appointment updated for "+p.getFullName()+": "+appointmentDate+(appointmentTime!=null?" at "+appointmentTime:"")+" (Token #"+a.getTokenNumber()+").";notifications.notifyDoctor(d.getId(),"APPOINTMENT_UPDATED",now);notifications.notifyStaffAndAdmin("APPOINTMENT_UPDATED",now);patientNotifications.appointmentUpdate(a,"MediQueue appointment changed",now);historyRepository.save(new AppointmentHistory(a,"UPDATED","Previous: "+old+". New: "+now,auth.getName()));}
        return "redirect:/admin/appointments";
    }

    @GetMapping("/toggle/{id}") public String toggle(@PathVariable Long id,Authentication auth){Appointment a=appointmentRepository.findById(id).orElseThrow();if("WAITING".equals(a.getStatus()))a.setStatus("CANCELLED");else if("CANCELLED".equals(a.getStatus()))a.setStatus("WAITING");appointmentRepository.save(a);String msg="Appointment for "+a.getPatient().getFullName()+" is now "+a.getStatus()+" (Token #"+a.getTokenNumber()+").";notifications.notifyDoctor(a.getDoctor().getId(),"APPOINTMENT_STATUS",msg);notifications.notifyStaffAndAdmin("APPOINTMENT_STATUS",msg);patientNotifications.appointmentUpdate(a,"MediQueue appointment status changed",msg);historyRepository.save(new AppointmentHistory(a,"STATUS_CHANGED",msg,auth.getName()));return "redirect:/admin/appointments";}
    @GetMapping("/delete/{id}") public String delete(@PathVariable Long id,Authentication auth){Appointment a=appointmentRepository.findById(id).orElseThrow();String msg="Appointment for "+a.getPatient().getFullName()+" was deleted/cancelled.";notifications.notifyDoctor(a.getDoctor().getId(),"APPOINTMENT_DELETED",msg);notifications.notifyStaffAndAdmin("APPOINTMENT_DELETED",msg);patientNotifications.appointmentUpdate(a,"MediQueue appointment cancelled",msg);historyRepository.save(new AppointmentHistory(a,"DELETED",msg,auth.getName()));appointmentRepository.deleteById(id);return "redirect:/admin/appointments";}
}
