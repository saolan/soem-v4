package com.saolan.soem.configuracion.dto;

/**
 * Filtros opcionales de busqueda para el listado paginado de certificados
 * emisores. Cualquier campo en {@code null} se ignora en el criterio.
 */
public record CertEmisFiltroDTO(
        String descri,
        String certCn,
        String certO,
        Boolean estado) {
}
