package com.saolan.soem.seguridad.servicio;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Hashing y verificacion de contrasenas con BCrypt. El costo (numero de
 * rondas, potencia de 2) es configurable via {@code soem.bcrypt.costo} para
 * poder ajustarlo segun el hardware del servidor sin recompilar.
 */
@ApplicationScoped
public class ClaveServicio {

    @ConfigProperty(name = "soem.bcrypt.costo", defaultValue = "12")
    private int costo;

    public String hashear(String claveTextoPlano) {
        return BCrypt.withDefaults().hashToString(costo, claveTextoPlano.toCharArray());
    }

    public boolean verificar(String claveTextoPlano, String hash) {
        return BCrypt.verifyer().verify(claveTextoPlano.toCharArray(), hash).verified;
    }
}
