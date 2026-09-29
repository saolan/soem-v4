package com.saolan.soem.configuracion.dto;

/**
 * Filtros opcionales de busqueda para el listado paginado de sucursales.
 * Cualquier campo en {@code null} se ignora en el criterio.
 */
public record SucursalFiltroDTO(
        String codigo,
        String descri,
        String ruc,
        String ciudad,
        Boolean matriz,
        Boolean estado) {
}
