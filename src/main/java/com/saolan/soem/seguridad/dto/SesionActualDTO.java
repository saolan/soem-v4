package com.saolan.soem.seguridad.dto;

import java.util.List;

/**
 * Identidad de la sesion actual, leida directamente de los claims del JWT ya
 * autenticado por el contenedor ({@code GET /auth/me}), sin volver a
 * consultar la base de datos.
 */
public record SesionActualDTO(
        Integer personaId,
        String usuario,
        Integer sucursalId,
        List<String> permisos,
        boolean claveCaducada) {
}
