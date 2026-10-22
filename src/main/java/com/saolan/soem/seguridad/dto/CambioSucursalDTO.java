package com.saolan.soem.seguridad.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Sucursal elegida por el usuario despues del login
 * ({@code POST /auth/cambiar-sucursal}). Debe ser una de las sucursales
 * accesibles devueltas en {@link LoginRespuestaDTO#sucursales()}.
 */
public record CambioSucursalDTO(

        @NotNull(message = "La sucursal es obligatoria")
        Integer sucursalId) {
}
