package com.saolan.soem.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de un parametro generico de seleccion.
 *
 * @param parametroId identificador; ignorado en el cuerpo de POST/PUT
 */
public record ParametroDTO(

        Integer parametroId,

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 32, message = "El código no puede superar los 32 caracteres")
        String codigo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
