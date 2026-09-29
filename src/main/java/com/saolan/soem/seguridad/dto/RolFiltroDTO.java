package com.saolan.soem.seguridad.dto;

/**
 * Filtros opcionales de busqueda para el listado paginado de roles.
 * Cualquier campo en {@code null} se ignora en el criterio.
 */
public record RolFiltroDTO(
        String descri,
        Boolean estado) {
}
