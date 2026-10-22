package com.saolan.soem.seguridad.dto;

/**
 * Proyeccion plana de una fila de {@code menu} (sin cargar la entidad), base
 * para armar el arbol en memoria.
 *
 * @param menuSupeId menu superior; {@code null} para un menu raiz
 */
public record MenuPlanoDTO(
        Integer menuId,
        Integer menuSupeId,
        String descri,
        String paginaRuta,
        Boolean detall,
        Boolean estado) {
}
