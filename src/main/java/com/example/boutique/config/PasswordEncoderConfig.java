package com.example.boutique.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Déclaré à part de {@link SecurityConfig} pour éviter une dépendance circulaire :
 * les providers d'authentification (super admin, DAO) ont besoin du PasswordEncoder,
 * et SecurityConfig a besoin de ces providers.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
