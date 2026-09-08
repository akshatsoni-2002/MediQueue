package com.example.mediqueue.service;

import com.example.mediqueue.model.Appointment;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PatientNotificationService {
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public PatientNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,@Value("${mediqueue.mail.enabled:false}") boolean enabled,@Value("${spring.mail.username:}") String from){
        this.mailSender=mailSenderProvider.getIfAvailable(); this.enabled=enabled; this.from=from;
    }
    public void sendToPatient(String email,String subject,String body){
        if(!enabled||mailSender==null||email==null||email.isBlank())return;
        try{SimpleMailMessage m=new SimpleMailMessage();if(from!=null&&!from.isBlank())m.setFrom(from);m.setTo(email);m.setSubject(subject);m.setText(body);mailSender.send(m);}catch(Exception ex){System.out.println("Patient email could not be sent: "+ex.getMessage());}
    }
    public void appointmentUpdate(Appointment appointment,String subject,String body){
        if(appointment==null||appointment.getPatient()==null)return;
        sendToPatient(appointment.getPatient().getEmail(),subject,body);
    }
}
