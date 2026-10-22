package com.saolan.soem.seguridad.servicio;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Hashing y verificacion de contrasenas con BCrypt. El costo (numero de
 * rondas, potencia de 2) es configurable via {@code soem.bcrypt.costo} para
 * poder ajustarlo segun el hardware del servidor sin recompilar.
 */
@ApplicationScoped
public class ClaveServicio {

    /** Limite de BCrypt: los bytes adicionales no se procesan y la libreria los rechaza. */
    private static final int MAXIMO_BYTES_CLAVE = 72;

    @Inject
    @ConfigProperty(name = "soem.bcrypt.costo", defaultValue = "12")
    private int costo;

    /** Hash de un valor aleatorio con el mismo costo: solo sirve para igualar tiempos de respuesta. */
    private String hashFicticio;

    @PostConstruct
    void inicializar() {
        byte[] aleatorio = new byte[16];
        new SecureRandom().nextBytes(aleatorio);
        hashFicticio = hashear(Base64.getEncoder().encodeToString(aleatorio));
    }

    public String hashear(String claveTextoPlano) {
        return BCrypt.withDefaults().hashToString(costo, claveTextoPlano.toCharArray());
    }

    public boolean verificar(String claveTextoPlano, String hash) {
        if (claveTextoPlano.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_CLAVE) {
            return false;
        }
        return BCrypt.verifyer().verify(claveTextoPlano.toCharArray(), hash).verified;
    }

    /**
     * Ejecuta una verificacion BCrypt completa contra un hash ficticio cuando
     * el usuario no existe o esta inactivo, para que el tiempo de respuesta
     * no revele si el usuario existe (enumeracion de usuarios).
     */
    public void simularVerificacion(String claveTextoPlano) {
        verificar(claveTextoPlano, hashFicticio);
    }
}
