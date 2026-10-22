package com.saolan.soem.seguridad.dto;

/**
 * Filtros opcionales de busqueda para el listado paginado de menus.
 * Cualquier campo en {@code null} se ignora en el criterio.
 */
public record MenuFiltroDTO(
        String descri,
        Integer menuSupeId,
        Boolean estado) {
}
