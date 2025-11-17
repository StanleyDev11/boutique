package com.example.boutique.config;

import com.example.boutique.service.ParametreService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

@Component
public class DynamicSessionTimeoutListener implements HttpSessionListener {

    private final ParametreService parametreService;

    public DynamicSessionTimeoutListener(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // Le service retourne des minutes, setMaxInactiveInterval attend des secondes
        int delaiInactiviteMinutes = parametreService.getDelaiInactivite();
        int delaiInactiviteSecondes = delaiInactiviteMinutes * 60;
        
        HttpSession session = se.getSession();
        session.setMaxInactiveInterval(delaiInactiviteSecondes);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // Pas d'action nécessaire à la destruction de la session
    }
}
