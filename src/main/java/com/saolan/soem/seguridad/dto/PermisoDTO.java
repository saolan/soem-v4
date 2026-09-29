package com.saolan.soem.seguridad.dto;

import java.math.BigDecimal;

import com.saolan.soem.compartido.seguridad.Accion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Permiso puntual del catalogo: la combinacion {@code recurso}+{@code accion}
 * es la que valida {@code FiltroAutorizacion} contra el claim del JWT.
 *
 * @param permisoId identificador; ignorado en el cuerpo de POST/PUT
 * @param valor      campo heredado del esquema original sin semantica
 *                   definida para este modelo; opcional
 */
public record PermisoDTO(

        Integer permisoId,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        @NotBlank(message = "El recurso es obligatorio")
        @Size(max = 100, message = "El recurso no puede superar los 100 caracteres")
        String recurso,

        @NotNull(message = "La acción es obligatoria")
        Accion accion,

        BigDecimal valor) {
}
