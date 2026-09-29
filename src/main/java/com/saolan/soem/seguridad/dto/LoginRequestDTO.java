package com.saolan.soem.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de acceso. {@code sucursalId} es opcional: si no se indica
 * (o el usuario no tiene acceso a ella), {@code AutenticacionServicio}
 * resuelve una por defecto entre las sucursales accesibles del usuario.
 */
public record LoginRequestDTO(

        @NotBlank(message = "El usuario es obligatorio")
        String usuario,

        @NotBlank(message = "La clave es obligatoria")
        String clave,

        Integer sucursalId) {
}
