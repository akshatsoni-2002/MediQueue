package com.example.mediqueue.controller;
import com.example.mediqueue.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
@Controller
@RequestMapping("/notifications")
public class NotificationController {
 private final NotificationService service;
 public NotificationController(NotificationService s){service=s;}
 @GetMapping public String page(Authentication a, Model m){m.addAttribute("notifications",service.recent(a.getName()));m.addAttribute("unread",service.unread(a.getName()));return "notifications";}
 @GetMapping("/stream") @ResponseBody public SseEmitter stream(Authentication a){return service.subscribe(a.getName());}
 @GetMapping("/unread") @ResponseBody public long unread(Authentication a){return service.unread(a.getName());}
 @PostMapping("/{id}/read") public String read(@PathVariable Long id,Authentication a){service.markRead(id,a.getName());return "redirect:/notifications";}
 @PostMapping("/read-all") public String readAll(Authentication a){service.markAllRead(a.getName());return "redirect:/notifications";}
}
