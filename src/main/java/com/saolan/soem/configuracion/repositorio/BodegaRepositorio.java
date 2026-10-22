package com.saolan.soem.configuracion.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.configuracion.dto.BodegaFiltroDTO;
import com.saolan.soem.configuracion.modelo.Bodega;
import com.saolan.soem.configuracion.modelo.SucuBode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link Bodega} mediante Criteria API: busqueda
 * paginada/filtrada, combo de solo lectura y las consultas auxiliares que
 * necesita {@code BodegaServicio} para sus reglas de negocio (unicidad de
 * {@code descri} y uso de la bodega en {@code SUCU_BODE}).
 */
@ApplicationScoped
public class BodegaRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS = Set.of("bodegaId", "descri", "estado");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "descri";

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Bodega> buscarPorId(Integer bodegaId) {
        return Optional.ofNullable(entityManager.find(Bodega.class, bodegaId));
    }

    public void crear(Bodega bodega) {
        entityManager.persist(bodega);
    }

    /**
     * Busca otra bodega con la misma descripcion (sin distinguir mayusculas),
     * excluyendo opcionalmente el registro que se esta modificando.
     */
    public Optional<Bodega> buscarPorDescri(String descri, Integer idExcluido) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Bodega> query = criteriaBuilder.createQuery(Bodega.class);
        Root<Bodega> raiz = query.from(Bodega.class);

        List<Predicate> predicados = new ArrayList<>();
        predicados.add(criteriaBuilder.equal(criteriaBuilder.lower(raiz.get("descri")),
                descri.toLowerCase(Locale.ROOT)));
        if (idExcluido != null) {
            predicados.add(criteriaBuilder.notEqual(raiz.get("bodegaId"), idExcluido));
        }

        query.where(predicados.toArray(new Predicate[0]));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    /**
     * Cuenta las sucursales que tienen la bodega habilitada
     * ({@code SUCU_BODE.acceso = true}); se usa para impedir su desactivacion.
     */
    public long contarSucursalesAsignadas(Integer bodegaId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<SucuBode> raiz = query.from(SucuBode.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(
                criteriaBuilder.equal(raiz.get("bodega").get("bodegaId"), bodegaId),
                criteriaBuilder.isTrue(raiz.get("acceso")));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<Bodega> buscarPaginado(BodegaFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Bodega> query = criteriaBuilder.createQuery(Bodega.class);
        Root<Bodega> raiz = query.from(Bodega.class);

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

    public long contar(BodegaFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Bodega> raiz = query.from(Bodega.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<Bodega> raiz = query.from(Bodega.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("bodegaId"), raiz.get("descri")));
        query.where(criteriaBuilder.isTrue(raiz.get("estado")));
        query.orderBy(criteriaBuilder.asc(raiz.get("descri")));

        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<Bodega> raiz,
            BodegaFiltroDTO filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro == null) {
            return predicados;
        }
        if (filtro.descri() != null && !filtro.descri().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("descri")),
                    "%" + filtro.descri().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.estado() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("estado"), filtro.estado()));
        }
        return predicados;
    }
}
