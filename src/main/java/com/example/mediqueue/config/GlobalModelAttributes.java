package com.example.mediqueue.config;

import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.UserRepository;
import com.example.mediqueue.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public GlobalModelAttributes(
            NotificationService notificationService,
            UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addGlobalAttributes(
            Authentication authentication,
            Model model) {

        model.addAttribute("currentRole", "PUBLIC");
        model.addAttribute("currentUserId", "");
        model.addAttribute("currentUserDbId", null);
        model.addAttribute("currentProfileName", "");
        model.addAttribute("currentProfileInitial", "U");
        model.addAttribute("currentHasProfilePhoto", false);
        model.addAttribute("unreadNotificationCount", 0L);

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return;
        }

        try {
            String role = authentication.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .findFirst()
                    .orElse("USER");

            String userId = authentication.getName();

            model.addAttribute("currentRole", role);
            model.addAttribute("currentUserId", userId);

            User user = userRepository.findByUserId(userId).orElse(null);

            if (user != null) {
                model.addAttribute("currentUserDbId", user.getId());

                String name = user.getDisplayName();

                if (name == null || name.isBlank()) {
                    if (user.getPatient() != null) {
                        name = user.getPatient().getFullName();
                    } else if (user.getDoctor() != null) {
                        name = user.getDoctor().getFullName();
                    } else {
                        name = user.getUserId();
                    }
                }

                if (name == null || name.isBlank()) {
                    name = user.getUserId();
                }

                model.addAttribute("currentProfileName", name);
                model.addAttribute(
                        "currentProfileInitial",
                        name.substring(0, 1).toUpperCase()
                );
                model.addAttribute(
                        "currentHasProfilePhoto",
                        user.getProfilePhoto() != null
                                && user.getProfilePhoto().length > 0
                );
            } else {
                model.addAttribute("currentProfileName", userId);
                model.addAttribute(
                        "currentProfileInitial",
                        userId == null || userId.isBlank()
                                ? "U"
                                : userId.substring(0, 1).toUpperCase()
                );
            }

            /*
             * Notification count must never be allowed to break the
             * dashboard/navbar. If the notification table/query is
             * temporarily unavailable, the application continues with 0.
             */
            try {
                model.addAttribute(
                        "unreadNotificationCount",
                        notificationService.unread(userId)
                );
            } catch (Exception ignored) {
                model.addAttribute("unreadNotificationCount", 0L);
            }

        } catch (Exception ignored) {
            /* Keep the shared navbar safe even if user metadata is unavailable. */
        }
    }
}
