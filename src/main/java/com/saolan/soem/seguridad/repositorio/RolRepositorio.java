package com.saolan.soem.seguridad.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.seguridad.dto.RolFiltroDTO;
import com.saolan.soem.seguridad.modelo.Rol;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link Rol} mediante Criteria API: busqueda
 * paginada/filtrada y combo de solo lectura.
 */
@ApplicationScoped
public class RolRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS = Set.of("rolId", "descri", "estado");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "descri";

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Rol> buscarPorId(Integer rolId) {
        return Optional.ofNullable(entityManager.find(Rol.class, rolId));
    }

    public void crear(Rol rol) {
        entityManager.persist(rol);
    }

    public List<Rol> buscarPaginado(RolFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Rol> query = criteriaBuilder.createQuery(Rol.class);
        Root<Rol> raiz = query.from(Rol.class);

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

    public long contar(RolFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Rol> raiz = query.from(Rol.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<Rol> raiz = query.from(Rol.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("rolId"), raiz.get("descri")));
        query.where(criteriaBuilder.isTrue(raiz.get("estado")));
        query.orderBy(criteriaBuilder.asc(raiz.get("descri")));

        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<Rol> raiz, RolFiltroDTO filtro) {
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
