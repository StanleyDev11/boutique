package com.example.boutique.security;

import org.springframework.security.authentication.AccountStatusException;

/**
 * Levée au login lorsqu'un compte (ex: admin.boutika) tente de se connecter
 * pendant la période d'essai : ce compte ne sera disponible qu'après activation
 * de la licence.
 */
public class LicenseAwaitingActivationException extends AccountStatusException {
    public LicenseAwaitingActivationException(String msg) {
        super(msg);
    }
}
