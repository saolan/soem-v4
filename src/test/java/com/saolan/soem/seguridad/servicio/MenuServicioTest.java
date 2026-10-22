package com.saolan.soem.seguridad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.seguridad.dto.MenuDTO;
import com.saolan.soem.seguridad.dto.MenuNodoDTO;
import com.saolan.soem.seguridad.dto.MenuPlanoDTO;
import com.saolan.soem.seguridad.mapeador.MenuMapeador;
import com.saolan.soem.seguridad.mapeador.MenuMapeadorImpl;
import com.saolan.soem.seguridad.modelo.Menu;
import com.saolan.soem.seguridad.repositorio.MenuRepositorio;

/**
 * Tests unitarios de {@link MenuServicio}: mockea {@link MenuRepositorio} y
 * usa {@link MenuMapeador} real como {@code @Spy}. Arbol base de los casos:
 * 1000 (grupo raiz) &rarr; 1050 (grupo) &rarr; 1100 (pagina), y 2000 (grupo
 * raiz) &rarr; 2050 (grupo).
 */
@ExtendWith(MockitoExtension.class)
class MenuServicioTest {

    private static final List<MenuPlanoDTO> ARBOL_BASE = List.of(
            plano(1000, null, false),
            plano(1050, 1000, false),
            plano(1100, 1050, true),
            plano(2000, null, false),
            plano(2050, 2000, false));

    @Mock
    private MenuRepositorio menuRepositorio;

    @Spy
    private MenuMapeador menuMapeador = new MenuMapeadorImpl();

    @InjectMocks
    private MenuServicio menuServicio;

    @Test
    void crear_deberiaCrearPaginaBajoUnGrupo() {
        when(menuRepositorio.buscarPorPaginaRuta("/seguridad/menus", null)).thenReturn(Optional.empty());
        when(menuRepositorio.buscarPorId(1050)).thenReturn(Optional.of(nuevoMenu(1050, false)));
        when(menuRepositorio.buscarTodosPlanos()).thenReturn(ARBOL_BASE);

        MenuDTO creado = menuServicio.crear(new MenuDTO(null, 1050, "Menú", " /seguridad/menus ", true, true));

        assertEquals(1050, creado.menuSupeId());
        assertEquals("/seguridad/menus", creado.paginaRuta());
        verify(menuRepositorio).crear(any(Menu.class));
    }

    @Test
    void crear_deberiaCrearGrupoRaizSinRuta() {
        when(menuRepositorio.buscarTodosPlanos()).thenReturn(ARBOL_BASE);

        MenuDTO creado = menuServicio.crear(new MenuDTO(null, null, "Reportes", null, false, true));

        assertNull(creado.menuSupeId());
        assertNull(creado.paginaRuta());
    }

    @Test
    void crear_deberiaLanzarValidacion_cuandoLaPaginaNoTieneRuta() {
        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> menuServicio.crear(new MenuDTO(null, 1050, "Menú", null, true, true)));

        assertEquals("paginaRuta", excepcion.getErrores().get(0).campo());
    }

    @Test
    void crear_deberiaLanzarValidacion_cuandoElGrupoTieneRuta() {
        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> menuServicio.crear(new MenuDTO(null, null, "Grupo", "/grupo", false, true)));

        assertEquals("paginaRuta", excepcion.getErrores().get(0).campo());
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoLaRutaYaExiste() {
        when(menuRepositorio.buscarPorPaginaRuta("/seguridad/roles", null))
                .thenReturn(Optional.of(nuevoMenu(1100, true)));

        assertThrows(ExcepcionConflicto.class,
                () -> menuServicio.crear(new MenuDTO(null, 1050, "Rol", "/seguridad/roles", true, true)));
    }

    @Test
    void crear_deberiaLanzarValidacion_cuandoElSuperiorEsUnaPagina() {
        when(menuRepositorio.buscarPorPaginaRuta("/nueva", null)).thenReturn(Optional.empty());
        when(menuRepositorio.buscarPorId(1100)).thenReturn(Optional.of(nuevoMenu(1100, true)));

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> menuServicio.crear(new MenuDTO(null, 1100, "Nueva", "/nueva", true, true)));

        assertEquals("menuSupeId", excepcion.getErrores().get(0).campo());
    }

    @Test
    void crear_deberiaLanzarValidacion_cuandoSuperaLaProfundidadMaxima() {
        List<MenuPlanoDTO> arbol = List.of(plano(1000, null, false), plano(1050, 1000, false), plano(1060, 1050, false));
        when(menuRepositorio.buscarPorPaginaRuta("/nueva", null)).thenReturn(Optional.empty());
        when(menuRepositorio.buscarPorId(1060)).thenReturn(Optional.of(nuevoMenu(1060, false)));
        when(menuRepositorio.buscarTodosPlanos()).thenReturn(arbol);

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> menuServicio.crear(new MenuDTO(null, 1060, "Nueva", "/nueva", true, true)));

        assertEquals("menuSupeId", excepcion.getErrores().get(0).campo());
    }

    @Test
    void modificar_deberiaLanzarValidacion_cuandoSeUbicaDentroDeUnSubmenuPropio() {
        when(menuRepositorio.buscarPorId(1000)).thenReturn(Optional.of(nuevoMenu(1000, false)));
        when(menuRepositorio.buscarPorId(1050)).thenReturn(Optional.of(nuevoMenu(1050, false)));
        when(menuRepositorio.buscarTodosPlanos()).thenReturn(ARBOL_BASE);

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> menuServicio.modificar(1000, new MenuDTO(1000, 1050, "Configuración", null, false, true)));

        assertEquals("menuSupeId", excepcion.getErrores().get(0).campo());
    }

    @Test
    void modificar_deberiaLanzarValidacion_cuandoUnGrupoConHijosPasaAPagina() {
        when(menuRepositorio.buscarPorId(1050)).thenReturn(Optional.of(nuevoMenu(1050, false)));
        when(menuRepositorio.buscarPorPaginaRuta("/seguridad", 1050)).thenReturn(Optional.empty());
        when(menuRepositorio.buscarTodosPlanos()).thenReturn(ARBOL_BASE);

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> menuServicio.modificar(1050, new MenuDTO(1050, null, "Seguridad", "/seguridad", true, true)));

        assertEquals("detall", excepcion.getErrores().get(0).campo());
    }

    @Test
    void modificar_deberiaLanzarValidacion_cuandoElSubarbolMovidoSuperaLaProfundidad() {
        when(menuRepositorio.buscarPorId(1050)).thenReturn(Optional.of(nuevoMenu(1050, false)));
        when(menuRepositorio.buscarPorId(2050)).thenReturn(Optional.of(nuevoMenu(2050, false)));
        when(menuRepositorio.buscarTodosPlanos()).thenReturn(ARBOL_BASE);

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> menuServicio.modificar(1050, new MenuDTO(1050, 2050, "Seguridad", null, false, true)));

        assertEquals("menuSupeId", excepcion.getErrores().get(0).campo());
    }

    @Test
    void modificar_deberiaMoverElMenuARaiz() {
        Menu menu = nuevoMenu(1050, false);
        menu.setMenuSupe(nuevoMenu(1000, false));
        when(menuRepositorio.buscarPorId(1050)).thenReturn(Optional.of(menu));
        when(menuRepositorio.buscarTodosPlanos()).thenReturn(ARBOL_BASE);

        MenuDTO modificado = menuServicio.modificar(1050, new MenuDTO(1050, null, "Seguridad", null, false, true));

        assertNull(modificado.menuSupeId());
        assertEquals("Seguridad", modificado.descri());
    }

    @Test
    void modificar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(menuRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class,
                () -> menuServicio.modificar(99, new MenuDTO(99, null, "X", null, false, true)));
    }

    @Test
    void eliminar_deberiaMarcarEstadoFalse() {
        Menu menu = nuevoMenu(1100, true);
        when(menuRepositorio.buscarPorId(1100)).thenReturn(Optional.of(menu));

        menuServicio.eliminar(1100);

        assertFalse(menu.getEstado());
    }

    @Test
    void menuDeSesion_deberiaRetornarVacio_cuandoNoHaySucursalActiva() {
        assertTrue(menuServicio.menuDeSesion(1, null).isEmpty());
        verifyNoInteractions(menuRepositorio);
    }

    @Test
    void menuDeSesion_deberiaArmarElArbolSinGruposVacios() {
        when(menuRepositorio.buscarAccesiblesDePersona(1, 5)).thenReturn(ARBOL_BASE);

        List<MenuNodoDTO> menu = menuServicio.menuDeSesion(1, 5);

        assertEquals(1, menu.size());
        assertEquals(1000, menu.get(0).menuId());
    }

    @Test
    void arbol_deberiaIncluirGruposVacios() {
        when(menuRepositorio.buscarTodosPlanos()).thenReturn(ARBOL_BASE);

        assertEquals(2, menuServicio.arbol().size());
    }

    // ---------- helpers ----------

    private static MenuPlanoDTO plano(Integer menuId, Integer menuSupeId, boolean detall) {
        return new MenuPlanoDTO(menuId, menuSupeId, "Menú " + menuId, detall ? "/ruta-" + menuId : null, detall, true);
    }

    private Menu nuevoMenu(Integer menuId, boolean detall) {
        Menu menu = new Menu();
        menu.setMenuId(menuId);
        menu.setDescri("Menú " + menuId);
        menu.setDetall(detall);
        menu.setEstado(Boolean.TRUE);
        return menu;
    }
}
