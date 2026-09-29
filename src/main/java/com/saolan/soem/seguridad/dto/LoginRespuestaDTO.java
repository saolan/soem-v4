package com.saolan.soem.seguridad.dto;

import java.util.List;

/**
 * Resultado de un login exitoso. El refresh token nunca viaja aqui (solo en
 * la cookie HttpOnly de la respuesta); {@code accessToken} si, porque el
 * frontend debe guardarlo en memoria segun AGENTS.md.
 *
 * @param claveCaducada si la clave del usuario esta caducada: el login igual
 *                      se completa, pero el frontend debe forzar el cambio
 *                      de clave antes de permitir operar con normalidad
 */
public record LoginRespuestaDTO(
        String accessToken,
        long expiraEnSegundos,
        Integer sucursalId,
        UsuarioDTO usuario,
        List<String> permisos,
        boolean claveCaducada) {
}
