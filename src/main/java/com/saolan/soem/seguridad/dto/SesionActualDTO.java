package com.saolan.soem.seguridad.dto;

import java.util.List;

/**
 * Identidad de la sesion actual, leida directamente de los claims del JWT ya
 * autenticado por el contenedor ({@code GET /auth/me}), sin volver a
 * consultar la base de datos.
 *
 * @param requiereSeleccionSucursal el token no tiene sucursal activa: el
 *                                  usuario debe elegirla con
 *                                  {@code POST /auth/cambiar-sucursal}
 */
public record SesionActualDTO(
        Integer personaId,
        String usuario,
        Integer sucursalId,
        List<String> permisos,
        boolean claveCaducada,
        boolean requiereSeleccionSucursal) {
}
