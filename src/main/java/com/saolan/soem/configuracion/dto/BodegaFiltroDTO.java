package com.saolan.soem.configuracion.dto;

/**
 * Filtros opcionales de busqueda para el listado paginado de bodegas.
 * Cualquier campo en {@code null} se ignora en el criterio.
 */
public record BodegaFiltroDTO(
        String descri,
        Boolean estado) {
}
