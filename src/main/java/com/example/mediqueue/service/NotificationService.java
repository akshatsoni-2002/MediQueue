package com.example.mediqueue.service;

import com.example.mediqueue.model.*;
import com.example.mediqueue.repository.NotificationRepository;
import com.example.mediqueue.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    public NotificationService(NotificationRepository n, UserRepository u){this.notificationRepository=n;this.userRepository=u;}

    public Notification notifyUser(String userId, String type, String message){
        User user=userRepository.findByUserId(userId).orElse(null); if(user==null || !user.isActive()) return null;
        Notification n=notificationRepository.save(new Notification(user,type,message)); push(userId,n); return n;
    }
    public void notifyRole(Role role, String type, String message){
        userRepository.findByRoleAndActiveTrue(role).forEach(u -> { Notification n=notificationRepository.save(new Notification(u,type,message)); push(u.getUserId(),n); });
    }
    public void notifyPatient(Long patientId, String type, String message){ userRepository.findByPatient_IdAndActiveTrue(patientId).forEach(u -> { Notification n=notificationRepository.save(new Notification(u,type,message)); push(u.getUserId(),n); }); }
    public void notifyDoctor(Long doctorId, String type, String message){
        userRepository.findByDoctor_IdAndActiveTrue(doctorId).forEach(u -> { Notification n=notificationRepository.save(new Notification(u,type,message)); push(u.getUserId(),n); });
    }
    public void notifyStaffAndAdmin(String type,String message){notifyRole(Role.STAFF,type,message);notifyRole(Role.ADMIN,type,message);}
    public List<Notification> recent(String userId){return notificationRepository.findTop50ByRecipient_UserIdOrderByCreatedAtDesc(userId);}
    public long unread(String userId){return notificationRepository.countByRecipient_UserIdAndReadFalse(userId);}
    public void markRead(Long id,String userId){notificationRepository.findById(id).filter(n->n.getRecipient().getUserId().equals(userId)).ifPresent(n->{n.setRead(true);notificationRepository.save(n);});}
    public void markAllRead(String userId){recent(userId).stream().filter(n->!n.isRead()).forEach(n->{n.setRead(true);notificationRepository.save(n);});}
    public SseEmitter subscribe(String userId){SseEmitter e=new SseEmitter(0L);emitters.computeIfAbsent(userId,k->new java.util.concurrent.CopyOnWriteArrayList<>()).add(e);e.onCompletion(()->remove(userId,e));e.onTimeout(()->remove(userId,e));e.onError(x->remove(userId,e));return e;}
    private void remove(String userId,SseEmitter e){List<SseEmitter> l=emitters.get(userId);if(l!=null)l.remove(e);}
    private void push(String userId,Notification n){List<SseEmitter> l=emitters.get(userId);if(l==null)return;for(SseEmitter e:l){try{e.send(SseEmitter.event().name("notification").data(n.getMessage()));}catch(IOException ex){remove(userId,e);}}}
}
