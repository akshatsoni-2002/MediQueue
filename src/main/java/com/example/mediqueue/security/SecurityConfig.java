package com.example.mediqueue.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.AuthenticationManager;

@Configuration
public class SecurityConfig {

    private final PatientOAuth2SuccessHandler patientOAuth2SuccessHandler;

    public SecurityConfig(PatientOAuth2SuccessHandler patientOAuth2SuccessHandler) {
        this.patientOAuth2SuccessHandler = patientOAuth2SuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {

        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/login",
                    "/patient/login",
                    "/patient/signup",
                    "/patient/signup/**",
                    "/about.html",
                    "/contact.html",
                    "/privacy.html",
                    "/terms.html",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()
                .requestMatchers("/admin/users/**").hasRole("ADMIN")
                .requestMatchers("/patient/**").hasRole("PATIENT")
                .requestMatchers("/profile/**").authenticated()
                .requestMatchers("/doctor/queue/**", "/notifications/**")
                    .hasAnyRole("DOCTOR", "STAFF", "ADMIN", "PATIENT")
                .requestMatchers(
                    "/facilities/**",
                    "/admin/departments/**",
                    "/admin/doctors/**",
                    "/admin/patients/**",
                    "/admin/appointments/**",
                    "/staff/**",
                    "/appointments/**",
                    "/queue/**"
                ).hasAnyRole("STAFF", "ADMIN")
                .requestMatchers("/doctor/appointments/**", "/doctor/clinical/**")
                    .hasRole("DOCTOR")
                .requestMatchers("/dashboard").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        // Enable Google patient login only when OAuth2 client credentials
        // are configured. This allows the application to start normally
        // before Google Client ID/Secret are supplied.
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                .loginPage("/patient/login")
                .successHandler(patientOAuth2SuccessHandler)
            );
        }

        return http.build();
    }
}
