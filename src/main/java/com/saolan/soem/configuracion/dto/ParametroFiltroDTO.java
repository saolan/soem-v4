package com.saolan.soem.configuracion.dto;

/**
 * Filtros opcionales de busqueda para el listado paginado de parametros.
 * Cualquier campo en {@code null} se ignora en el criterio.
 */
public record ParametroFiltroDTO(
        String codigo,
        String descri,
        Boolean estado) {
}
