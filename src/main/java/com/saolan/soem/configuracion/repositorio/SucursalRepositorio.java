package com.saolan.soem.configuracion.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.configuracion.dto.SucuBodeDTO;
import com.saolan.soem.configuracion.dto.SucursalFiltroDTO;
import com.saolan.soem.configuracion.modelo.Bodega;
import com.saolan.soem.configuracion.modelo.SucuBode;
import com.saolan.soem.configuracion.modelo.Sucursal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link Sucursal} mediante Criteria API: busqueda
 * paginada/filtrada, combo de solo lectura y las consultas auxiliares que
 * necesita {@code SucursalServicio} para sus reglas de negocio (unicidad de
 * codigo/ruc).
 */
@ApplicationScoped
public class SucursalRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS =
            Set.of("sucursalId", "codigo", "descri", "ruc", "ciudad", "matriz", "estado");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "descri";

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Sucursal> buscarPorId(Integer sucursalId) {
        return Optional.ofNullable(entityManager.find(Sucursal.class, sucursalId));
    }

    public void crear(Sucursal sucursal) {
        entityManager.persist(sucursal);
    }

    public Optional<Sucursal> buscarPorCodigo(String codigo, Integer idExcluido) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Sucursal> query = criteriaBuilder.createQuery(Sucursal.class);
        Root<Sucursal> raiz = query.from(Sucursal.class);

        List<Predicate> predicados = new ArrayList<>();
        predicados.add(criteriaBuilder.equal(raiz.get("codigo"), codigo));
        agregarExclusionId(criteriaBuilder, raiz, idExcluido, predicados);

        query.where(predicados.toArray(new Predicate[0]));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    public Optional<Sucursal> buscarPorRuc(String ruc, Integer idExcluido) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Sucursal> query = criteriaBuilder.createQuery(Sucursal.class);
        Root<Sucursal> raiz = query.from(Sucursal.class);

        List<Predicate> predicados = new ArrayList<>();
        predicados.add(criteriaBuilder.equal(raiz.get("ruc"), ruc));
        agregarExclusionId(criteriaBuilder, raiz, idExcluido, predicados);

        query.where(predicados.toArray(new Predicate[0]));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

	public List<Sucursal> buscarPaginado(SucursalFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Sucursal> query = criteriaBuilder.createQuery(Sucursal.class);
        Root<Sucursal> raiz = query.from(Sucursal.class);

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

    public long contar(SucursalFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Sucursal> raiz = query.from(Sucursal.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<Sucursal> raiz = query.from(Sucursal.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("sucursalId"), raiz.get("descri")));
        query.where(criteriaBuilder.isTrue(raiz.get("estado")));
        query.orderBy(criteriaBuilder.asc(raiz.get("descri")));

        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Bodegas asignadas a la sucursal ({@code SUCU_BODE}) proyectadas a
     * {@link SucuBodeDTO} en una sola consulta, ordenadas por descripcion.
     */
    public List<SucuBodeDTO> buscarBodegas(Integer sucursalId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SucuBodeDTO> query = criteriaBuilder.createQuery(SucuBodeDTO.class);
        Root<SucuBode> raiz = query.from(SucuBode.class);
        Join<SucuBode, Bodega> bodega = raiz.join("bodega");

        query.select(criteriaBuilder.construct(SucuBodeDTO.class,
                bodega.get("bodegaId"), bodega.get("descri"), raiz.get("acceso")));
        query.where(criteriaBuilder.equal(raiz.get("sucursal").get("sucursalId"), sucursalId));
        query.orderBy(criteriaBuilder.asc(bodega.get("descri")));

        return entityManager.createQuery(query).getResultList();
    }

    private void agregarExclusionId(CriteriaBuilder criteriaBuilder, Root<Sucursal> raiz, Integer idExcluido,
            List<Predicate> predicados) {
        if (idExcluido != null) {
            predicados.add(criteriaBuilder.notEqual(raiz.get("sucursalId"), idExcluido));
        }
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<Sucursal> raiz,
            SucursalFiltroDTO filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro == null) {
            return predicados;
        }
        if (filtro.codigo() != null && !filtro.codigo().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("codigo")),
                    "%" + filtro.codigo().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.descri() != null && !filtro.descri().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("descri")),
                    "%" + filtro.descri().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.ruc() != null && !filtro.ruc().isBlank()) {
            predicados.add(criteriaBuilder.like(raiz.get("ruc"), "%" + filtro.ruc() + "%"));
        }
        if (filtro.ciudad() != null && !filtro.ciudad().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("ciudad")),
                    "%" + filtro.ciudad().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.matriz() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("matriz"), filtro.matriz()));
        }
        if (filtro.estado() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("estado"), filtro.estado()));
        }
        return predicados;
    }
}
