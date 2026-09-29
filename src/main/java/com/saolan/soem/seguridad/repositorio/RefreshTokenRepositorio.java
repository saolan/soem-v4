package com.saolan.soem.seguridad.repositorio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.saolan.soem.seguridad.modelo.RefreshToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link RefreshToken}: alta, busqueda por hash (para
 * validar el token que llega en la cookie) y revocacion masiva de los
 * tokens vigentes de una persona (usada en la deteccion de reuso, ver
 * {@code AutenticacionServicio}).
 */
@ApplicationScoped
public class RefreshTokenRepositorio {

    @PersistenceContext
    private EntityManager entityManager;

    public void crear(RefreshToken refreshToken) {
        entityManager.persist(refreshToken);
    }

    public Optional<RefreshToken> buscarPorHash(String tokenHash) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RefreshToken> query = criteriaBuilder.createQuery(RefreshToken.class);
        Root<RefreshToken> raiz = query.from(RefreshToken.class);

        query.where(criteriaBuilder.equal(raiz.get("tokenHash"), tokenHash));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    /** Tokens vigentes (no revocados, no expirados) de una persona: usados para revocarlos todos ante un reuso detectado. */
    public List<RefreshToken> buscarVigentesDePersona(Integer personaId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RefreshToken> query = criteriaBuilder.createQuery(RefreshToken.class);
        Root<RefreshToken> raiz = query.from(RefreshToken.class);

        Predicate esDeLaPersona = criteriaBuilder.equal(raiz.get("usuario").get("personaId"), personaId);
        Predicate noRevocado = criteriaBuilder.isFalse(raiz.get("revocado"));
        Predicate noExpirado = criteriaBuilder.greaterThan(raiz.get("fechaHoraExpira"), OffsetDateTime.now());

        query.where(esDeLaPersona, noRevocado, noExpirado);
        return entityManager.createQuery(query).getResultList();
    }
}
