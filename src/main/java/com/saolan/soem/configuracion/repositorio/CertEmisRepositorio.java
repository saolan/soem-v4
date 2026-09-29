package com.saolan.soem.configuracion.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.configuracion.dto.CertEmisFiltroDTO;
import com.saolan.soem.configuracion.modelo.CertEmis;
import com.saolan.soem.configuracion.modelo.SucuCertEmis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link CertEmis} mediante Criteria API: busqueda
 * paginada/filtrada, combo de solo lectura (consumido por el agregado
 * Sucursal) y las consultas auxiliares que necesita {@code CertEmisServicio}
 * para sus reglas de negocio (unicidad de {@code descri} y uso del emisor en
 * {@code SUCU_CERT_EMIS}).
 */
@ApplicationScoped
public class CertEmisRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS =
            Set.of("certEmisId", "descri", "certCn", "certO", "certC", "estado");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "descri";

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<CertEmis> buscarPorId(Integer certEmisId) {
        return Optional.ofNullable(entityManager.find(CertEmis.class, certEmisId));
    }

    public void crear(CertEmis certEmis) {
        entityManager.persist(certEmis);
    }

    /**
     * Busca otro emisor con la misma descripcion (sin distinguir mayusculas),
     * excluyendo opcionalmente el registro que se esta modificando.
     */
    public Optional<CertEmis> buscarPorDescri(String descri, Integer idExcluido) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CertEmis> query = criteriaBuilder.createQuery(CertEmis.class);
        Root<CertEmis> raiz = query.from(CertEmis.class);

        List<Predicate> predicados = new ArrayList<>();
        predicados.add(criteriaBuilder.equal(criteriaBuilder.lower(raiz.get("descri")),
                descri.toLowerCase(Locale.ROOT)));
        if (idExcluido != null) {
            predicados.add(criteriaBuilder.notEqual(raiz.get("certEmisId"), idExcluido));
        }

        query.where(predicados.toArray(new Predicate[0]));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    /**
     * Cuenta las sucursales que tienen asignado el emisor en
     * {@code SUCU_CERT_EMIS}; se usa para impedir su desactivacion.
     */
    public long contarSucursalesAsignadas(Integer certEmisId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<SucuCertEmis> raiz = query.from(SucuCertEmis.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(criteriaBuilder.equal(raiz.get("certEmis").get("certEmisId"), certEmisId));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<CertEmis> buscarPaginado(CertEmisFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CertEmis> query = criteriaBuilder.createQuery(CertEmis.class);
        Root<CertEmis> raiz = query.from(CertEmis.class);

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

    public long contar(CertEmisFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<CertEmis> raiz = query.from(CertEmis.class);

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<CertEmis> raiz = query.from(CertEmis.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("certEmisId"), raiz.get("descri")));
        query.where(criteriaBuilder.isTrue(raiz.get("estado")));
        query.orderBy(criteriaBuilder.asc(raiz.get("descri")));

        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<CertEmis> raiz,
            CertEmisFiltroDTO filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro == null) {
            return predicados;
        }
        agregarContiene(criteriaBuilder, raiz, "descri", filtro.descri(), predicados);
        agregarContiene(criteriaBuilder, raiz, "certCn", filtro.certCn(), predicados);
        agregarContiene(criteriaBuilder, raiz, "certO", filtro.certO(), predicados);
        if (filtro.estado() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("estado"), filtro.estado()));
        }
        return predicados;
    }

    private void agregarContiene(CriteriaBuilder criteriaBuilder, Root<CertEmis> raiz, String campo, String valor,
            List<Predicate> predicados) {
        if (valor != null && !valor.isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get(campo)),
                    "%" + valor.toLowerCase(Locale.ROOT) + "%"));
        }
    }
}
