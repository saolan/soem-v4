package com.saolan.soem.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de acceso. La sucursal no se elige aqui: si el usuario tiene
 * acceso a varias, el login responde con
 * {@code requiereSeleccionSucursal = true} y el frontend la pide antes de
 * llamar a {@code POST /auth/cambiar-sucursal} (ver
 * {@link LoginRespuestaDTO}). Un {@code sucursalId} enviado por clientes
 * anteriores se ignora (JSON-B descarta las propiedades desconocidas).
 */
public record LoginRequestDTO(

        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 100, message = "El usuario no puede superar los 100 caracteres")
        String usuario,

        // BCrypt solo procesa hasta 72 bytes.
        @NotBlank(message = "La clave es obligatoria")
        @Size(max = 72, message = "La clave no puede superar los 72 caracteres")
        String clave) {
}
