package com.example.boutique.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String userHome = System.getProperty("user.home");
        Path uploadDir = Paths.get(userHome, "boutique-uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/login", "/css/**", "/js/**", "/loo.jpg", "/favicon.ico", "/uploads/**").permitAll() // Pages et ressources publiques
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
                .requestMatchers("/.well-known/**").permitAll() // Ignore requests from browser dev tools
                .anyRequest().authenticated() // Toutes les autres pages nécessitent une connexion
            )
            .sessionManagement(session -> session
                .invalidSessionUrl("/login") // Redirect to plain login page on invalid session (e.g., after server restart)
                .maximumSessions(1) // Allow only one session per user
                .maxSessionsPreventsLogin(false) // If a new session is created, the old one is invalidated
            )
            .formLogin(form -> form
                .loginPage("/login") // URL de notre page de connexion
                .successHandler(customAuthenticationSuccessHandler) // Utilise notre gestionnaire personnalisé
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new OrRequestMatcher(
                    new AntPathRequestMatcher("/logout", "GET"),
                    new AntPathRequestMatcher("/logout", "POST")
                ))
                .logoutSuccessUrl("/login?logout") // Page après la déconnexion
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
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
