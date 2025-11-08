package com.example.boutique.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/loo.jpg").permitAll() // Pages et ressources publiques
                .requestMatchers("/api/**").authenticated() // Autoriser l'accès à l'API pour les utilisateurs connectés
                .requestMatchers("/produits/delete/**").hasRole("ADMIN") // Seul l'admin peut supprimer
                .requestMatchers("/produits/new", "/produits/edit/**", "/produits/save").hasAnyRole("ADMIN", "GESTIONNAIRE") // Qui peut modifier/créer
                .requestMatchers("/stock/**").hasAnyRole("ADMIN", "GESTIONNAIRE")
                .requestMatchers("/personnel/**").hasRole("ADMIN")
                .requestMatchers("/utilisateurs/**").hasRole("ADMIN")
                .requestMatchers("/rapports/ventes-historique").hasAnyRole("ADMIN", "GESTIONNAIRE", "CAISSIER")
                .requestMatchers("/rapports/sales-by-day").hasAnyRole("ADMIN", "GESTIONNAIRE", "CAISSIER")
                .requestMatchers("/rapports/sales-by-category").hasAnyRole("ADMIN", "GESTIONNAIRE", "CAISSIER")
                .requestMatchers("/rapports/**").hasAnyRole("ADMIN", "GESTIONNAIRE")
                .requestMatchers("/dashboard").hasRole("ADMIN")
                .requestMatchers("/produits").hasAnyRole("ADMIN", "GESTIONNAIRE") // Admin et Gestionnaire peuvent voir les produits
                .requestMatchers("/gestion-caisses/**").hasAnyRole("ADMIN", "GESTIONNAIRE")
                .requestMatchers("/ventes/*/annuler").hasAnyRole("ADMIN", "GESTIONNAIRE")
                .anyRequest().authenticated() // Toutes les autres pages nécessitent une connexion
            )
            .formLogin(form -> form
                .loginPage("/login") // URL de notre page de connexion
                .successHandler(customAuthenticationSuccessHandler) // Utilise notre gestionnaire personnalisé
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout") // Page après la déconnexion
                .permitAll()
            )
            ;

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
