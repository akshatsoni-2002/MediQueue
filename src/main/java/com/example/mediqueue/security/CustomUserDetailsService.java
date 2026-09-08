package com.example.mediqueue.security;

import com.example.mediqueue.model.User;
import com.example.mediqueue.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String username)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByUserId(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + username
                        )
                );

        if (!user.isActive()) {
            throw new UsernameNotFoundException(
                    "User account is inactive."
            );
        }

        String roleName =
                "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserId())
                .password(user.getPassword())
                .authorities(
                        new SimpleGrantedAuthority(roleName)
                )
                .disabled(!user.isActive())
                .build();
    }
}