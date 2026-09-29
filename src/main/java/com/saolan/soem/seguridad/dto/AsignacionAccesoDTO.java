package com.saolan.soem.seguridad.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Elemento de un reemplazo en bloque de asignaciones rol-recurso. Reutilizado
 * tanto para {@code PUT /roles/{id}/permisos} ({@code valor} = id de
 * {@code Permiso}) como para {@code PUT /roles/{id}/sucursales}
 * ({@code valor} = id de {@code Sucursal}).
 *
 * @param valor  identificador del recurso asignado (permiso o sucursal)
 * @param acceso si la asignacion queda habilitada
 */
public record AsignacionAccesoDTO(

        @NotNull(message = "Debe indicar el identificador del recurso asignado")
        Integer valor,

        @NotNull(message = "Debe indicar si el acceso queda habilitado")
        Boolean acceso) {
}
