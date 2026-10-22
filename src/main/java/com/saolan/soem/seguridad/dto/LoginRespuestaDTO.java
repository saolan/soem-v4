package com.saolan.soem.seguridad.dto;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;

/**
 * Resultado de un login, refresh o cambio de sucursal exitoso. El refresh
 * token nunca viaja aqui (solo en la cookie HttpOnly de la respuesta);
 * {@code accessToken} si, porque el frontend debe guardarlo en memoria segun
 * AGENTS.md.
 *
 * @param sucursalId    sucursal activa de la sesion
 * @param sucursales    sucursales accesibles del usuario: si hay mas de una,
 *                      el frontend ofrece elegir y llama a
 *                      {@code POST /auth/cambiar-sucursal}
 * @param claveCaducada si la clave del usuario esta caducada: el token se
 *                      emite sin permisos, de modo que solo puede usarse
 *                      para {@code /auth/cambiar-clave}, {@code /auth/me} y
 *                      {@code /auth/logout}
 */
public record LoginRespuestaDTO(
        String accessToken,
        long expiraEnSegundos,
        Integer sucursalId,
        List<ComboDTO> sucursales,
        UsuarioDTO usuario,
        List<String> permisos,
        boolean claveCaducada) {
}
