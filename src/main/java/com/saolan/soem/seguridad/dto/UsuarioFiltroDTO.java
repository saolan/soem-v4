package com.saolan.soem.seguridad.dto;

/**
 * Filtros opcionales de busqueda para el listado paginado de usuarios.
 * Cualquier campo en {@code null} se ignora en el criterio.
 */
public record UsuarioFiltroDTO(
        String cedulaRuc,
        String apelli,
        String usuari,
        Boolean estado) {
}
