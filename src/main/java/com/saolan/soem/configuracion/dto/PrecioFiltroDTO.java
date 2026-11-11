package com.saolan.soem.configuracion.dto;

/**
 * Filtros opcionales de busqueda para el listado paginado de listas de
 * precio. Cualquier campo en {@code null} se ignora en el criterio.
 */
public record PrecioFiltroDTO(
        String codigo,
        String descri,
        Boolean estado) {
}
