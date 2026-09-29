package com.saolan.soem.seguridad.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.seguridad.dto.PermisoFiltroDTO;
import com.saolan.soem.seguridad.modelo.Permiso;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link Permiso} mediante Criteria API: busqueda
 * paginada/filtrada, combo de solo lectura (sin filtro de estado: la tabla
 * no tiene esa columna, ver Javadoc de {@link Permiso}) y la consulta de
 * unicidad {@code recurso}+{@code accion} (restriccion aplicada en BD por la
 * migracion V2, replicada aqui para poder devolver un error de negocio legible
 * en {@code PermisoServicio} en lugar de una violacion de constraint cruda).
 */
@ApplicationScoped
public class PermisoRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS = Set.of("permisoId", "descri", "recurso", "accion");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "recurso";

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Permiso> buscarPorId(Integer permisoId) {
        return Optional.ofNullable(entityManager.find(Permiso.class, permisoId));
    }

    public void crear(Permiso permiso) {
        entityManager.persist(permiso);
    }

    public Optional<Permiso> buscarPorRecursoYAccion(String recurso, Accion accion, Integer idExcluido) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Permiso> query = criteriaBuilder.createQuery(Permiso.class);
        Root<Permiso> raiz = query.from(Permiso.class);

        List<Predicate> predicados = new ArrayList<>();
        predicados.add(criteriaBuilder.equal(raiz.get("recurso"), recurso));
        predicados.add(criteriaBuilder.equal(raiz.get("accion"), accion));
        if (idExcluido != null) {
            predicados.add(criteriaBuilder.notEqual(raiz.get("permisoId"), idExcluido));
        }

        query.where(predicados.toArray(new Predicate[0]));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    public List<Permiso> buscarPaginado(PermisoFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Permiso> query = criteriaBuilder.createQuery(Permiso.class);
        Root<Permiso> raiz = query.from(Permiso.class);

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

    public long contar(PermisoFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Permiso> raiz = query.from(Permiso.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<Permiso> raiz = query.from(Permiso.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("permisoId"), raiz.get("descri")));
        query.orderBy(criteriaBuilder.asc(raiz.get("recurso")), criteriaBuilder.asc(raiz.get("accion")));

        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<Permiso> raiz, PermisoFiltroDTO filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro == null) {
            return predicados;
        }
        if (filtro.descri() != null && !filtro.descri().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("descri")),
                    "%" + filtro.descri().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.recurso() != null && !filtro.recurso().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("recurso")),
                    "%" + filtro.recurso().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.accion() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("accion"), filtro.accion()));
        }
        return predicados;
    }
}
