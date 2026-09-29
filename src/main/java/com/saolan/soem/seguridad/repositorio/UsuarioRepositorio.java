package com.saolan.soem.seguridad.repositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.seguridad.dto.UsuarioFiltroDTO;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;

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
 * Acceso a datos de {@link PersUsua} (expuesto como "usuario" en la API),
 * combinando sus propios campos con los de la {@link Persona} asociada para
 * filtrar y ordenar por dato personal (apellido, cedula/ruc).
 */
@ApplicationScoped
public class UsuarioRepositorio {

    private static final Set<String> CAMPOS_ORDEN_PERMITIDOS =
            Set.of("personaId", "usuari", "estado", "apelli", "cedulaRuc");
    private static final String CAMPO_ORDEN_PREDETERMINADO = "apelli";

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<PersUsua> buscarPorId(Integer personaId) {
        return Optional.ofNullable(entityManager.find(PersUsua.class, personaId));
    }

    public Optional<PersUsua> buscarPorUsuari(String usuari) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PersUsua> query = criteriaBuilder.createQuery(PersUsua.class);
        Root<PersUsua> raiz = query.from(PersUsua.class);

        query.where(criteriaBuilder.equal(raiz.get("usuari"), usuari));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    public void crear(PersUsua persUsua) {
        entityManager.persist(persUsua);
    }

    public List<PersUsua> buscarPaginado(UsuarioFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PersUsua> query = criteriaBuilder.createQuery(PersUsua.class);
        Root<PersUsua> raiz = query.from(PersUsua.class);
        Join<PersUsua, Persona> persona = raiz.join("persona");

        query.where(construirPredicados(criteriaBuilder, raiz, persona, filtro).toArray(new Predicate[0]));

        String campoOrden = CAMPOS_ORDEN_PERMITIDOS.contains(parametrosBusqueda.ordenarPor())
                ? parametrosBusqueda.ordenarPor()
                : CAMPO_ORDEN_PREDETERMINADO;
        Path<?> rutaOrden = resolverRutaOrden(raiz, persona, campoOrden);
        query.orderBy(parametrosBusqueda.esDescendente()
                ? criteriaBuilder.desc(rutaOrden)
                : criteriaBuilder.asc(rutaOrden));

        return entityManager.createQuery(query)
                .setFirstResult(parametrosBusqueda.primerRegistro())
                .setMaxResults(parametrosBusqueda.tamanio())
                .getResultList();
    }

    public long contar(UsuarioFiltroDTO filtro) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<PersUsua> raiz = query.from(PersUsua.class);
        Join<PersUsua, Persona> persona = raiz.join("persona");

        query.select(criteriaBuilder.count(raiz));
        query.where(construirPredicados(criteriaBuilder, raiz, persona, filtro).toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Combo de usuarios: la etiqueta usa {@code usuari} (nombre de acceso),
     * unico y siempre presente, en lugar del nombre completo de la persona
     * (evita depender de {@code nombre}, que es nullable en el DDL).
     */
    public List<ComboDTO> buscarCombo() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ComboDTO> query = criteriaBuilder.createQuery(ComboDTO.class);
        Root<PersUsua> raiz = query.from(PersUsua.class);

        query.select(criteriaBuilder.construct(ComboDTO.class, raiz.get("personaId"), raiz.get("usuari")));
        query.where(criteriaBuilder.isTrue(raiz.get("estado")));
        query.orderBy(criteriaBuilder.asc(raiz.get("usuari")));

        return entityManager.createQuery(query).getResultList();
    }

    private Path<?> resolverRutaOrden(Root<PersUsua> raiz, Join<PersUsua, Persona> persona, String campoOrden) {
        return switch (campoOrden) {
            case "apelli", "cedulaRuc" -> persona.get(campoOrden);
            default -> raiz.get(campoOrden);
        };
    }

    private List<Predicate> construirPredicados(CriteriaBuilder criteriaBuilder, Root<PersUsua> raiz,
            Join<PersUsua, Persona> persona, UsuarioFiltroDTO filtro) {
        List<Predicate> predicados = new ArrayList<>();
        if (filtro == null) {
            return predicados;
        }
        if (filtro.cedulaRuc() != null && !filtro.cedulaRuc().isBlank()) {
            predicados.add(criteriaBuilder.like(persona.get("cedulaRuc"), "%" + filtro.cedulaRuc() + "%"));
        }
        if (filtro.apelli() != null && !filtro.apelli().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(persona.get("apelli")),
                    "%" + filtro.apelli().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.usuari() != null && !filtro.usuari().isBlank()) {
            predicados.add(criteriaBuilder.like(criteriaBuilder.lower(raiz.get("usuari")),
                    "%" + filtro.usuari().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filtro.estado() != null) {
            predicados.add(criteriaBuilder.equal(raiz.get("estado"), filtro.estado()));
        }
        return predicados;
    }
}
