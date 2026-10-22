package com.saolan.soem.seguridad.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.seguridad.dto.MenuFiltroDTO;
import com.saolan.soem.seguridad.dto.MenuPlanoDTO;
import com.saolan.soem.seguridad.modelo.Menu;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolMenu;
import com.saolan.soem.seguridad.modelo.RolPersUsua;
import com.saolan.soem.seguridad.modelo.RolSucu;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Acceso a datos de {@link Menu} mediante Criteria API: busqueda
 * paginada/filtrada, combo y proyecciones planas para armar el arbol (menu
 * de la sesion y arbol completo de administracion).
 */
@ApplicationScoped
public class MenuRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS = Set.of("menuId", "descri");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "menuId";

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Menu> buscarPorId(Integer menuId) {
        return Optional.ofNullable(entityManager.find(Menu.class, menuId));
    }

    public void crear(Menu menu) {
        entityManager.persist(menu);
    }

    public List<Menu> buscarPaginado(MenuFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Menu> query = criteriaBuilder.createQuery(Menu.class);
        Root<Menu> raiz = query.from(Menu.class);

        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        String campoOrden = CAMPOS_ORDEN_PERMITIDOS.contains(parametrosBusqueda.ordenarPor())
                ? parametrosBusqueda.ordenarPor()
                : CAMPO_ORDEN_PREDETERMINADO;
        Path<?> rutaOrden = raiz.get(campoOrden);
        query.orderBy(parametrosBusqueda.esDescendente()
                ? criteriaBuilder.desc(rutaOrden)
                : criteriaBuilder.asc(rutaOrden));

        return entityManager.createQuery(query)
                .setFirstResult(parametrosBusqueda.primerRegistro())
                .setMaxResults(parametrosBusqueda.tamanio())
                .getResultList();
    }

    public long contar(MenuFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Menu> raiz = query.from(Menu.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<Menu> raiz = query.from(Menu.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("menuId"), raiz.get("descri")));
        query.where(criteriaBuilder.isTrue(raiz.get("estado")));
        query.orderBy(criteriaBuilder.asc(raiz.get("menuId")));

        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Todas las filas de {@code menu}, incluidas las inactivas, como
     * proyeccion plana. La tabla es un catalogo pequeno (cientos de filas):
     * se carga completa para validar jerarquia (ciclos, profundidad) y para
     * el arbol de administracion, en lugar de recorrerla nivel a nivel.
     */
    public List<MenuPlanoDTO> buscarTodosPlanos() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<MenuPlanoDTO> query = criteriaBuilder.createQuery(MenuPlanoDTO.class);
        Root<Menu> raiz = query.from(Menu.class);
        Join<Menu, Menu> menuSupe = raiz.join("menuSupe", JoinType.LEFT);

        query.select(construirProyeccion(criteriaBuilder, raiz, menuSupe));
        query.orderBy(criteriaBuilder.asc(raiz.get("menuId")));

        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Menus activos habilitados para una persona en la sucursal activa: los
     * de {@code rol_menu} con acceso de sus roles activos que, a su vez,
     * tienen acceso a esa sucursal ({@code rol_sucu}). Un mismo menu
     * habilitado por varios roles aparece una sola vez.
     */
    public List<MenuPlanoDTO> buscarAccesiblesDePersona(Integer personaId, Integer sucursalId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<MenuPlanoDTO> query = criteriaBuilder.createQuery(MenuPlanoDTO.class);
        Root<RolPersUsua> raiz = query.from(RolPersUsua.class);
        Join<RolPersUsua, Rol> rol = raiz.join("rol");
        Join<Rol, RolMenu> rolMenu = rol.join("menus");
        Join<RolMenu, Menu> menu = rolMenu.join("menu");
        Join<Menu, Menu> menuSupe = menu.join("menuSupe", JoinType.LEFT);

        Subquery<Integer> rolConSucursal = query.subquery(Integer.class);
        Root<RolSucu> rolSucu = rolConSucursal.from(RolSucu.class);
        Join<RolSucu, Sucursal> sucursal = rolSucu.join("sucursal");
        rolConSucursal.select(rolSucu.get("rolSucuId"));
        rolConSucursal.where(
                criteriaBuilder.equal(rolSucu.get("rol"), rol),
                criteriaBuilder.equal(sucursal.get("sucursalId"), sucursalId),
                criteriaBuilder.isTrue(rolSucu.get("acceso")),
                criteriaBuilder.isTrue(sucursal.get("estado")));

        query.select(construirProyeccion(criteriaBuilder, menu, menuSupe)).distinct(true);
        query.where(
                criteriaBuilder.equal(raiz.get("usuario").get("personaId"), personaId),
                criteriaBuilder.isTrue(raiz.get("usuario").get("estado")),
                criteriaBuilder.isTrue(rol.get("estado")),
                criteriaBuilder.isTrue(rolMenu.get("acceso")),
                criteriaBuilder.isTrue(menu.get("estado")),
                criteriaBuilder.exists(rolConSucursal));
        query.orderBy(criteriaBuilder.asc(menu.get("menuId")));

        return entityManager.createQuery(query).getResultList();
    }

    /** Busca otro menu con la misma ruta, excluyendo opcionalmente un id. */
    public Optional<Menu> buscarPorPaginaRuta(String paginaRuta, Integer idExcluido) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Menu> query = criteriaBuilder.createQuery(Menu.class);
        Root<Menu> raiz = query.from(Menu.class);

        List<Predicate> predicados = new ArrayList<>();
        predicados.add(criteriaBuilder.equal(raiz.get("paginaRuta"), paginaRuta));
        if (idExcluido != null) {
            predicados.add(criteriaBuilder.notEqual(raiz.get("menuId"), idExcluido));
        }
        query.where(predicados.toArray(new Predicate[0]));

        return entityManager.createQuery(query).setMaxResults(1).getResultList().stream().findFirst();
    }

    private CompoundSelection<MenuPlanoDTO> construirProyeccion(
            CriteriaBuilder criteriaBuilder, Path<Menu> menu, Join<Menu, Menu> menuSupe) {
        return criteriaBuilder.construct(MenuPlanoDTO.class,
                menu.get("menuId"),
                menuSupe.get("menuId"),
                menu.get("descri"),
                menu.get("paginaRuta"),
                menu.get("detall"),
                menu.get("estado"));
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<Menu> raiz, MenuFiltroDTO filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro == null) {
            return predicados;
        }
        if (filtro.descri() != null && !filtro.descri().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("descri")),
                    "%" + filtro.descri().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.menuSupeId() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("menuSupe").get("menuId"), filtro.menuSupeId()));
        }
        if (filtro.estado() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("estado"), filtro.estado()));
        }
        return predicados;
    }
}
