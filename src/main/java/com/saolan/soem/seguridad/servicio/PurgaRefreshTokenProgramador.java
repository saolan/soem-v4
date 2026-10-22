package com.saolan.soem.seguridad.servicio;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

/**
 * Tarea diaria (03:00, hora del servidor) que borra los refresh tokens
 * expirados fuera del periodo de retencion. El temporizador no es
 * persistente: si el servidor estaba detenido a esa hora, la purga se
 * ejecuta al dia siguiente sin acumular ejecuciones. La transaccion la
 * abre {@link AutenticacionServicio}, no este EJB (AGENTS.md:
 * {@code @Transactional} solo en la capa Service).
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PurgaRefreshTokenProgramador {

    private static final Logger LOGGER = Logger.getLogger(PurgaRefreshTokenProgramador.class.getName());

    @Inject
    private AutenticacionServicio autenticacionServicio;

    @Schedule(hour = "3", minute = "0", persistent = false)
    public void purgarRefreshTokensExpirados() {
        try {
            int eliminados = autenticacionServicio.purgarRefreshTokensExpirados();
            LOGGER.log(Level.INFO, "Purga de refresh tokens expirados completada: {0} eliminados", eliminados);
        } catch (RuntimeException excepcion) {
            // Se registra y no se relanza: un fallo puntual no debe cancelar el temporizador.
            LOGGER.log(Level.SEVERE, "Error al purgar refresh tokens expirados", excepcion);
        }
    }
}
