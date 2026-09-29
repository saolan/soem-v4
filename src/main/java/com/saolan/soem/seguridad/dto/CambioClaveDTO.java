package com.saolan.soem.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de clave del usuario autenticado. Requiere la clave actual (no
 * alcanza con estar autenticado con un access token vigente) para evitar
 * que una sesion robada pueda tomar control total de la cuenta.
 */
public record CambioClaveDTO(

        @NotBlank(message = "La clave actual es obligatoria")
        String claveActual,

        @NotBlank(message = "La clave nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La clave nueva debe tener entre 8 y 72 caracteres")
        String claveNueva) {
}
