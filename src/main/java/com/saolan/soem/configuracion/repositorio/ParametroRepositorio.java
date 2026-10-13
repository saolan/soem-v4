package com.saolan.soem.configuracion.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.configuracion.dto.ParametroFiltroDTO;
import com.saolan.soem.configuracion.modelo.Parametro;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link Parametro} mediante Criteria API: busqueda
 * paginada/filtrada y combo de solo lectura (consumido por
 * {@code PERS_PARA} y {@code SUCU_PARA}).
 */
@ApplicationScoped
public class ParametroRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS =
            Set.of("parametroId", "codigo", "descri", "estado");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "descri";

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Parametro> buscarPorId(Integer parametroId) {
        return Optional.ofNullable(entityManager.find(Parametro.class, parametroId));
    }

    public void crear(Parametro parametro) {
        entityManager.persist(parametro);
    }

    public List<Parametro> buscarPaginado(ParametroFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Parametro> query = criteriaBuilder.createQuery(Parametro.class);
        Root<Parametro> raiz = query.from(Parametro.class);

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

    public long contar(ParametroFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<Parametro> raiz = query.from(Parametro.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<Parametro> raiz = query.from(Parametro.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("parametroId"), raiz.get("descri")));
        query.where(criteriaBuilder.isTrue(raiz.get("estado")));
        query.orderBy(criteriaBuilder.asc(raiz.get("descri")));

        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<Parametro> raiz,
            ParametroFiltroDTO filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro == null) {
            return predicados;
        }
        agregarContiene(criteriaBuilder, raiz, "codigo", filtro.codigo(), predicados);
        agregarContiene(criteriaBuilder, raiz, "descri", filtro.descri(), predicados);
        if (filtro.estado() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("estado"), filtro.estado()));
        }
        return predicados;
    }

    private void agregarContiene(CriteriaBuilder criteriaBuilder, Root<Parametro> raiz, String campo, String valor,
            List<Predicate> predicados) {
        if (valor != null && !valor.isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get(campo)),
                    "%" + valor.toLowerCase(Locale.ROOT) + "%"));
        }
    }
}
