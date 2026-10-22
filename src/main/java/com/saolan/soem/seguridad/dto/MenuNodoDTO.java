package com.saolan.soem.seguridad.dto;

import java.util.List;

/**
 * Nodo del arbol de menu, ordenado por {@code menuId} en cada nivel. Lo
 * consumen el menu lateral de la sesion ({@code GET /auth/menu}) y la
 * pantalla de asignacion de menus a roles ({@code GET /menus/arbol}).
 *
 * @param paginaRuta ruta del frontend; {@code null} en los grupos
 * @param detall     {@code true} = pagina navegable, {@code false} = grupo
 * @param hijos      submenus; lista vacia en las paginas
 */
public record MenuNodoDTO(
        Integer menuId,
        String descri,
        String paginaRuta,
        Boolean detall,
        Boolean estado,
        List<MenuNodoDTO> hijos) {
}
