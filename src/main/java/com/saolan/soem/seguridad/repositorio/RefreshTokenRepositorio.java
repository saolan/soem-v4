package com.saolan.soem.seguridad.repositorio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.saolan.soem.seguridad.modelo.RefreshToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Acceso a datos de {@link RefreshToken}: alta, busqueda por hash (para
 * validar el token que llega en la cookie), revocacion masiva de los
 * tokens vigentes de una persona (deteccion de reuso y cambio de clave, ver
 * {@code AutenticacionServicio}) y purga de tokens expirados.
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

    /**
     * Borra fisicamente los tokens expirados antes de {@code limite}. Como la
     * FK {@code token_padre_id} es RESTRICT, primero desvincula a los hijos
     * de las filas a borrar y luego las elimina.
     *
     * @return cantidad de tokens eliminados
     */
    public int purgarExpiradosAntesDe(OffsetDateTime limite) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaUpdate<RefreshToken> desvincular = criteriaBuilder.createCriteriaUpdate(RefreshToken.class);
        Root<RefreshToken> hijo = desvincular.from(RefreshToken.class);
        Subquery<Integer> idsAPurgar = desvincular.subquery(Integer.class);
        Root<RefreshToken> padre = idsAPurgar.from(RefreshToken.class);
        idsAPurgar.select(padre.get("refreshTokenId"))
                .where(criteriaBuilder.lessThan(padre.get("fechaHoraExpira"), limite));
        desvincular.set(hijo.<RefreshToken>get("tokenPadre"), (RefreshToken) null)
                .where(hijo.get("tokenPadre").get("refreshTokenId").in(idsAPurgar));
        entityManager.createQuery(desvincular).executeUpdate();

        CriteriaDelete<RefreshToken> borrar = criteriaBuilder.createCriteriaDelete(RefreshToken.class);
        Root<RefreshToken> raiz = borrar.from(RefreshToken.class);
        borrar.where(criteriaBuilder.lessThan(raiz.get("fechaHoraExpira"), limite));
        return entityManager.createQuery(borrar).executeUpdate();
    }
}
