package com.saolan.soem.configuracion.dto;

/**
 * Lista de precio asignada a una sucursal, tal como la lee la pantalla de
 * sucursal-precio ({@code GET /sucursales/{id}/precios}). El reemplazo
 * ({@code PUT}) recibe {@code AsignacionAccesoDTO} con {@code valor} =
 * {@code precioId}.
 *
 * @param descri descripcion de la lista de precio
 * @param acceso si la asignacion esta habilitada
 */
public record SucuPrecDTO(
        Integer precioId,
        String descri,
        Boolean acceso) {
}
