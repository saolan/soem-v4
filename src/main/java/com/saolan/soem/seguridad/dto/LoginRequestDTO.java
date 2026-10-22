package com.saolan.soem.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de acceso. {@code sucursalId} es opcional: si no se indica
 * (o el usuario no tiene acceso a ella), {@code AutenticacionServicio}
 * resuelve una por defecto entre las sucursales accesibles del usuario. El
 * frontend puede enviar la ultima sucursal recordada.
 */
public record LoginRequestDTO(

        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 100, message = "El usuario no puede superar los 100 caracteres")
        String usuario,

        // BCrypt solo procesa hasta 72 bytes.
        @NotBlank(message = "La clave es obligatoria")
        @Size(max = 72, message = "La clave no puede superar los 72 caracteres")
        String clave,

        Integer sucursalId) {
}
