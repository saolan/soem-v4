package com.saolan.soem.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de una bodega.
 *
 * @param bodegaId identificador; ignorado en el cuerpo de POST/PUT
 */
public record BodegaDTO(

        Integer bodegaId,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
