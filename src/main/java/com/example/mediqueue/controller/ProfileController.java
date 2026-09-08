package com.example.mediqueue.controller;

import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
@RequestMapping("/profile")
public class ProfileController {
    private final UserRepository userRepository;
    public ProfileController(UserRepository u){userRepository=u;}
    private User me(Authentication a){return userRepository.findByUserId(a.getName()).orElseThrow();}
    @GetMapping public String profile(Authentication a,Model m){m.addAttribute("profile",me(a));return "profile";}
    @PostMapping public String update(Authentication a,@RequestParam String displayName,@RequestParam(required=false) String email,@RequestParam(required=false) String mobile,@RequestParam(required=false) String bio,@RequestParam(required=false) String healthConcern,@RequestParam(required=false) String knownConditions,@RequestParam(required=false) String allergies,@RequestParam(required=false) String emergencyContact,@RequestParam(required=false) MultipartFile profilePhoto){
        User u=me(a);u.setDisplayName(displayName.trim());u.setEmail(email==null?null:email.trim());u.setMobile(mobile==null?null:mobile.trim());u.setBio(bio==null?null:bio.trim());
        if(profilePhoto!=null && !profilePhoto.isEmpty()){try{u.setProfilePhoto(profilePhoto.getBytes());u.setProfilePhotoType(profilePhoto.getContentType());}catch(IOException ignored){}}
        if(u.getPatient()!=null){u.getPatient().setFullName(displayName.trim());u.getPatient().setHealthConcern(healthConcern);u.getPatient().setKnownConditions(knownConditions);u.getPatient().setAllergies(allergies);u.getPatient().setEmergencyContact(emergencyContact);if(email!=null&&!email.isBlank())u.getPatient().setEmail(email.trim());if(mobile!=null&&!mobile.isBlank())u.getPatient().setPhone(mobile.trim());}
        if(u.getDoctor()!=null){u.getDoctor().setFullName(displayName.trim());if(email!=null&&!email.isBlank())u.getDoctor().setEmail(email.trim());if(mobile!=null&&!mobile.isBlank())u.getDoctor().setPhone(mobile.trim());u.getDoctor().setBio(bio);}
        userRepository.save(u);return "redirect:/profile?saved=true";
    }
    @GetMapping("/photo/{id}") @ResponseBody public byte[] photo(@PathVariable Long id,HttpServletResponse response){User u=userRepository.findById(id).orElseThrow();if(u.getProfilePhoto()==null){response.setStatus(404);return new byte[0];}response.setContentType(u.getProfilePhotoType()==null?"image/jpeg":u.getProfilePhotoType());return u.getProfilePhoto();}
}
