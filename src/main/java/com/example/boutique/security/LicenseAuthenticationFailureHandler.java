package com.example.boutique.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Redirige l'utilisateur selon le motif d'échec de connexion :
 *   - essai expiré         -> page d'expiration / activation (/license/expired)
 *   - compte en attente    -> /login?locked (message "disponible après activation")
 *   - identifiants erronés  -> /login?error
 */
@Component
public class LicenseAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        if (exception instanceof TrialExpiredException) {
            getRedirectStrategy().sendRedirect(request, response, "/license/expired");
            return;
        }
        if (exception instanceof LicenseAwaitingActivationException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?locked");
            return;
        }
        getRedirectStrategy().sendRedirect(request, response, "/login?error");
    }
}
