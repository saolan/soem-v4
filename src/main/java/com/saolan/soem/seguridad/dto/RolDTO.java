package com.saolan.soem.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de un rol. Los permisos y sucursales asignados no viajan aqui: se
 * administran como sub-recursos independientes ({@code /roles/{id}/permisos}
 * y {@code /roles/{id}/sucursales}).
 *
 * @param rolId identificador; ignorado en el cuerpo de POST/PUT
 */
public record RolDTO(

        Integer rolId,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
