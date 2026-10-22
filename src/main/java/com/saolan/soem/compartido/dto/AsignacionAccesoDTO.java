package com.saolan.soem.compartido.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Elemento de un reemplazo en bloque de asignaciones con flag {@code acceso}
 * (ver {@code compartido.servicio.ReemplazoPorDiferencia}). Lo usan los
 * sub-recursos {@code PUT /roles/{id}/permisos|sucursales|menus} y
 * {@code PUT /sucursales/{id}/bodegas}.
 *
 * @param valor  identificador del recurso asignado (permiso, sucursal, menu, bodega)
 * @param acceso si la asignacion queda habilitada
 */
public record AsignacionAccesoDTO(

        @NotNull(message = "Debe indicar el identificador del recurso asignado")
        Integer valor,

        @NotNull(message = "Debe indicar si el acceso queda habilitado")
        Boolean acceso) {
}
