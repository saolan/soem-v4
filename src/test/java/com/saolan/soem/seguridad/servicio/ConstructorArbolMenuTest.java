package com.saolan.soem.seguridad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.saolan.soem.seguridad.dto.MenuNodoDTO;
import com.saolan.soem.seguridad.dto.MenuPlanoDTO;

/**
 * Tests unitarios de {@link ConstructorArbolMenu}: jerarquia, orden, poda de
 * grupos vacios y descarte de nodos con ancestro ausente o en ciclo.
 */
class ConstructorArbolMenuTest {

    @Test
    void construir_deberiaArmarLaJerarquiaOrdenadaPorId() {
        List<MenuPlanoDTO> filas = List.of(
                pagina(1150, 1050, "/seguridad/menus"),
                grupo(1050, 1000),
                pagina(1100, 1050, "/seguridad/roles"),
                grupo(1000, null),
                grupo(3300, null),
                grupo(3350, 3300),
                pagina(3600, 3350, "/inventario/productos"));

        List<MenuNodoDTO> arbol = ConstructorArbolMenu.construir(filas, true);

        assertEquals(List.of(1000, 3300), ids(arbol));
        MenuNodoDTO seguridad = arbol.get(0).hijos().get(0);
        assertEquals(1050, seguridad.menuId());
        assertEquals(List.of(1100, 1150), ids(seguridad.hijos()));
        assertTrue(seguridad.hijos().get(0).hijos().isEmpty());
    }

    @Test
    void construir_deberiaPodarGruposSinHijos_cuandoSeSolicita() {
        List<MenuPlanoDTO> filas = List.of(
                grupo(1000, null),
                grupo(1050, 1000),
                grupo(2000, null),
                pagina(2100, 2000, "/configuracion/bodegas"));

        assertEquals(List.of(2000), ids(ConstructorArbolMenu.construir(filas, true)));
        assertEquals(List.of(1000, 2000), ids(ConstructorArbolMenu.construir(filas, false)));
    }

    @Test
    void construir_deberiaDescartarNodos_cuandoFaltaUnAncestro() {
        List<MenuPlanoDTO> filas = List.of(
                grupo(1000, null),
                pagina(1100, 1000, "/seguridad/roles"),
                pagina(1200, 1050, "/seguridad/permisos"));

        List<MenuNodoDTO> arbol = ConstructorArbolMenu.construir(filas, true);

        assertEquals(1, arbol.size());
        assertEquals(List.of(1100), ids(arbol.get(0).hijos()));
    }

    @Test
    void construir_deberiaDescartarNodosEnCiclo() {
        List<MenuPlanoDTO> filas = List.of(
                grupo(1000, null),
                pagina(1100, 1000, "/seguridad/roles"),
                grupo(5000, 5001),
                grupo(5001, 5000));

        List<MenuNodoDTO> arbol = ConstructorArbolMenu.construir(filas, false);

        assertEquals(List.of(1000), ids(arbol));
    }

    @Test
    void construir_deberiaRetornarVacio_cuandoNoHayFilas() {
        assertTrue(ConstructorArbolMenu.construir(List.of(), true).isEmpty());
    }

    // ---------- helpers ----------

    private MenuPlanoDTO grupo(Integer menuId, Integer menuSupeId) {
        return new MenuPlanoDTO(menuId, menuSupeId, "Grupo " + menuId, null, false, true);
    }

    private MenuPlanoDTO pagina(Integer menuId, Integer menuSupeId, String paginaRuta) {
        return new MenuPlanoDTO(menuId, menuSupeId, "Página " + menuId, paginaRuta, true, true);
    }

    private List<Integer> ids(List<MenuNodoDTO> nodos) {
        return nodos.stream().map(MenuNodoDTO::menuId).toList();
    }
}
