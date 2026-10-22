package com.saolan.soem.seguridad.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.seguridad.dto.MenuDTO;
import com.saolan.soem.seguridad.dto.MenuFiltroDTO;
import com.saolan.soem.seguridad.dto.MenuNodoDTO;
import com.saolan.soem.seguridad.dto.MenuPlanoDTO;
import com.saolan.soem.seguridad.mapeador.MenuMapeador;
import com.saolan.soem.seguridad.modelo.Menu;
import com.saolan.soem.seguridad.repositorio.MenuRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Reglas de negocio del catalogo {@link Menu} y armado del menu de la
 * sesion:
 * <ul>
 * <li>La jerarquia admite como maximo {@link #PROFUNDIDAD_MAXIMA} niveles y
 * no puede tener ciclos (un menu no puede quedar dentro de si mismo ni de un
 * submenu propio).</li>
 * <li>Solo los grupos ({@code detall = false}) tienen submenus; una pagina
 * ({@code detall = true}) exige ruta y un grupo no la tiene.</li>
 * <li>La ruta es unica: el frontend la usa como clave de navegacion.</li>
 * </ul>
 */
@ApplicationScoped
public class MenuServicio {

    /** Niveles del menu lateral: menu, submenu y pagina. */
    static final int PROFUNDIDAD_MAXIMA = 3;

    @Inject
    private MenuRepositorio menuRepositorio;

    @Inject
    private MenuMapeador menuMapeador;

    public List<MenuDTO> buscar(MenuFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return menuRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(menuMapeador::aMenuDTO)
                .toList();
    }

    public long contar(MenuFiltroDTO filtro) {
        return menuRepositorio.contar(filtro);
    }

    public MenuDTO buscarPorId(Integer menuId) {
        return menuMapeador.aMenuDTO(obtenerOFallar(menuId));
    }

    public List<ComboDTO> combo() {
        return menuRepositorio.buscarCombo();
    }

    /** Arbol completo, incluidos los menus inactivos (administracion). */
    public List<MenuNodoDTO> arbol() {
        return ConstructorArbolMenu.construir(menuRepositorio.buscarTodosPlanos(), false);
    }

    /**
     * Menu lateral de la sesion: menus habilitados para la persona en la
     * sucursal activa, sin grupos vacios. Sin sucursal activa no hay menu.
     */
    public List<MenuNodoDTO> menuDeSesion(Integer personaId, Integer sucursalId) {
        if (personaId == null || sucursalId == null) {
            return List.of();
        }
        return ConstructorArbolMenu.construir(menuRepositorio.buscarAccesiblesDePersona(personaId, sucursalId), true);
    }

    @Transactional
    public MenuDTO crear(MenuDTO menuDTO) {
        Menu menu = menuMapeador.aMenu(menuDTO);
        aplicarJerarquiaYRuta(menu, menuDTO, null);
        menuRepositorio.crear(menu);
        return menuMapeador.aMenuDTO(menu);
    }

    @Transactional
    public MenuDTO modificar(Integer menuId, MenuDTO menuDTO) {
        Menu menu = obtenerOFallar(menuId);
        aplicarJerarquiaYRuta(menu, menuDTO, menuId);
        menuMapeador.actualizarEntidadDesdeDto(menuDTO, menu);
        return menuMapeador.aMenuDTO(menu);
    }

    /**
     * Baja logica. Los submenus no se modifican: el menu de la sesion ya
     * descarta todo nodo con un ancestro inactivo.
     */
    @Transactional
    public void eliminar(Integer menuId) {
        Menu menu = obtenerOFallar(menuId);
        menu.setEstado(Boolean.FALSE);
    }

    private Menu obtenerOFallar(Integer menuId) {
        return menuRepositorio.buscarPorId(menuId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró el menú solicitado"));
    }

    /**
     * Valida y asigna el menu superior y la ruta.
     *
     * @param menuIdActual id del menu que se modifica; {@code null} al crear
     */
    private void aplicarJerarquiaYRuta(Menu menu, MenuDTO menuDTO, Integer menuIdActual) {
        boolean esPagina = Boolean.TRUE.equals(menuDTO.detall());
        String paginaRuta = normalizarRuta(menuDTO.paginaRuta());
        validarRuta(esPagina, paginaRuta, menuIdActual);

        Menu menuSupe = null;
        if (menuDTO.menuSupeId() != null) {
            menuSupe = menuRepositorio.buscarPorId(menuDTO.menuSupeId())
                    .orElseThrow(() -> excepcionDeCampo("menuSupeId", "El menú superior no existe"));
            if (Boolean.TRUE.equals(menuSupe.getDetall())) {
                throw excepcionDeCampo("menuSupeId", "Una página no puede contener submenús");
            }
        }
        validarJerarquia(menuDTO.menuSupeId(), menuIdActual, esPagina);

        menu.setMenuSupe(menuSupe);
        menu.setPaginaRuta(paginaRuta);
    }

    private void validarRuta(boolean esPagina, String paginaRuta, Integer menuIdActual) {
        if (esPagina && paginaRuta == null) {
            throw excepcionDeCampo("paginaRuta", "La ruta es obligatoria en una página");
        }
        if (!esPagina && paginaRuta != null) {
            throw excepcionDeCampo("paginaRuta", "Un grupo de menú no tiene ruta");
        }
        if (paginaRuta != null && menuRepositorio.buscarPorPaginaRuta(paginaRuta, menuIdActual).isPresent()) {
            throw new ExcepcionConflicto("El menú entra en conflicto con datos existentes",
                    List.of(new ErrorCampo("paginaRuta", "Ya existe un menú con esta ruta")));
        }
    }

    private void validarJerarquia(Integer menuSupeId, Integer menuIdActual, boolean esPagina) {
        Map<Integer, MenuPlanoDTO> menus = ConstructorArbolMenu.indexarPorId(menuRepositorio.buscarTodosPlanos());

        int alturaPropia = 1;
        if (menuIdActual != null) {
            if (esPagina && tieneHijos(menus, menuIdActual)) {
                throw excepcionDeCampo("detall", "Un menú con submenús no puede ser una página");
            }
            alturaPropia = altura(menus, menuIdActual);
        }

        int profundidadSuperior = 0;
        Set<Integer> visitados = new HashSet<>();
        for (Integer actual = menuSupeId; actual != null; actual = superiorDe(menus, actual)) {
            if (actual.equals(menuIdActual)) {
                throw excepcionDeCampo("menuSupeId", "Un menú no puede ubicarse dentro de sí mismo ni de un submenú propio");
            }
            if (!visitados.add(actual)) {
                // Ciclo preexistente en los datos: la cadena no llega a una raiz.
                throw excepcionDeCampo("menuSupeId", "El menú superior tiene una jerarquía inválida");
            }
            profundidadSuperior++;
        }

        if (profundidadSuperior + alturaPropia > PROFUNDIDAD_MAXIMA) {
            throw excepcionDeCampo("menuSupeId",
                    "El menú no puede tener más de " + PROFUNDIDAD_MAXIMA + " niveles");
        }
    }

    private Integer superiorDe(Map<Integer, MenuPlanoDTO> menus, Integer menuId) {
        MenuPlanoDTO menu = menus.get(menuId);
        return menu == null ? null : menu.menuSupeId();
    }

    private boolean tieneHijos(Map<Integer, MenuPlanoDTO> menus, Integer menuId) {
        return menus.values().stream().anyMatch(menu -> menuId.equals(menu.menuSupeId()));
    }

    /** Niveles del subarbol que cuelga de {@code menuId}, incluido el propio. */
    private int altura(Map<Integer, MenuPlanoDTO> menus, Integer menuId) {
        int alturaMaxima = 1;
        for (MenuPlanoDTO menu : menus.values()) {
            int profundidad = 1;
            Set<Integer> visitados = new HashSet<>();
            for (Integer actual = menu.menuSupeId(); actual != null && visitados.add(actual);
                    actual = superiorDe(menus, actual)) {
                profundidad++;
                if (actual.equals(menuId)) {
                    alturaMaxima = Math.max(alturaMaxima, profundidad);
                    break;
                }
            }
        }
        return alturaMaxima;
    }

    private String normalizarRuta(String paginaRuta) {
        if (paginaRuta == null || paginaRuta.isBlank()) {
            return null;
        }
        return paginaRuta.strip();
    }

    private ExcepcionValidacion excepcionDeCampo(String campo, String mensaje) {
        return new ExcepcionValidacion(mensaje, List.of(new ErrorCampo(campo, mensaje)));
    }
}
