package com.saolan.soem.seguridad.servicio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link ClaveServicio}. El campo {@code costo} se
 * inyecta con {@code @ConfigProperty} (CDI/MicroProfile Config), que no
 * corre en un test unitario puro: se fija por reflexión al mínimo permitido
 * por BCrypt (4) para que los tests sean rápidos.
 */
class ClaveServicioTest {

    private ClaveServicio claveServicio;

    @BeforeEach
    void configurarCosto() throws NoSuchFieldException, IllegalAccessException {
        claveServicio = new ClaveServicio();
        Field campoCosto = ClaveServicio.class.getDeclaredField("costo");
        campoCosto.setAccessible(true);
        campoCosto.set(claveServicio, 4);
    }

    @Test
    void hashear_deberiaProducirUnHashDistintoDelTextoPlano() {
        String hash = claveServicio.hashear("MiClaveSegura123");

        assertNotEquals("MiClaveSegura123", hash);
        assertTrue(hash.startsWith("$2"), "el hash de BCrypt debe tener el prefijo de versión del algoritmo");
    }

    @Test
    void verificar_deberiaSerVerdadero_cuandoLaClaveEsCorrecta() {
        String hash = claveServicio.hashear("MiClaveSegura123");

        assertTrue(claveServicio.verificar("MiClaveSegura123", hash));
    }

    @Test
    void verificar_deberiaSerFalso_cuandoLaClaveEsIncorrecta() {
        String hash = claveServicio.hashear("MiClaveSegura123");

        assertFalse(claveServicio.verificar("OtraClaveDistinta", hash));
    }

    @Test
    void hashear_deberiaProducirHashesDistintos_paraLaMismaClave() {
        String primerHash = claveServicio.hashear("MiClaveSegura123");
        String segundoHash = claveServicio.hashear("MiClaveSegura123");

        assertNotEquals(primerHash, segundoHash, "cada hash debe usar un salt aleatorio distinto");
        assertTrue(claveServicio.verificar("MiClaveSegura123", primerHash));
        assertTrue(claveServicio.verificar("MiClaveSegura123", segundoHash));
    }
}
