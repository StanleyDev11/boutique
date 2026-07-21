package com.example.boutique.security;

import org.springframework.security.authentication.AccountStatusException;

/**
 * Levée au login lorsque la période d'essai est terminée et que la licence
 * n'est pas activée. Le gestionnaire d'échec redirige alors vers la page
 * d'expiration / d'activation.
 */
public class TrialExpiredException extends AccountStatusException {
    public TrialExpiredException(String msg) {
        super(msg);
    }
}
