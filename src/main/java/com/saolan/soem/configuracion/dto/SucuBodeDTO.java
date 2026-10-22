package com.saolan.soem.configuracion.dto;

/**
 * Bodega asignada a una sucursal, tal como la lee la pantalla de
 * sucursal-bodega ({@code GET /sucursales/{id}/bodegas}). El reemplazo
 * ({@code PUT}) recibe {@code AsignacionAccesoDTO} con {@code valor} =
 * {@code bodegaId}.
 *
 * @param descri descripcion de la bodega
 * @param acceso si la asignacion esta habilitada
 */
public record SucuBodeDTO(
        Integer bodegaId,
        String descri,
        Boolean acceso) {
}
