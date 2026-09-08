package com.example.mediqueue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.Properties;

@Configuration
public class PatientMailConfig {
 @Bean
 @ConditionalOnProperty(name="mediqueue.mail.enabled", havingValue="true")
 public JavaMailSender patientMailSender(@Value("${mediqueue.mail.host:smtp.gmail.com}") String host,@Value("${mediqueue.mail.port:587}") int port,@Value("${mediqueue.mail.username:}") String username,@Value("${mediqueue.mail.password:}") String password,@Value("${mediqueue.mail.starttls:true}") boolean starttls){
  JavaMailSenderImpl sender=new JavaMailSenderImpl(); sender.setHost(host); sender.setPort(port); sender.setUsername(username); sender.setPassword(password);
  Properties props=sender.getJavaMailProperties(); props.put("mail.smtp.auth","true"); props.put("mail.smtp.starttls.enable",String.valueOf(starttls)); props.put("mail.smtp.starttls.required",String.valueOf(starttls));
  return sender;
 }
}
