package com.saolan.soem.seguridad.dto;

import com.saolan.soem.compartido.seguridad.Accion;

/**
 * Filtros opcionales de busqueda para el listado paginado de permisos.
 * Cualquier campo en {@code null} se ignora en el criterio.
 */
public record PermisoFiltroDTO(
        String descri,
        String recurso,
        Accion accion) {
}
