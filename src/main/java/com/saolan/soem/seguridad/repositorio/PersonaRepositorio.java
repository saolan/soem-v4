package com.saolan.soem.seguridad.repositorio;

import java.util.Optional;

import com.saolan.soem.seguridad.modelo.Persona;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Acceso a datos de {@link Persona}, acotado a lo que {@code UsuarioServicio}
 * necesita para dar de alta y validar un usuario. Sin CRUD ni combo propios
 * (ver Javadoc de {@link Persona}).
 */
@ApplicationScoped
public class PersonaRepositorio {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Persona> buscarPorId(Integer personaId) {
        return Optional.ofNullable(entityManager.find(Persona.class, personaId));
    }

    public Optional<Persona> buscarPorCedula(String cedulaRuc) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Persona> query = criteriaBuilder.createQuery(Persona.class);
        Root<Persona> raiz = query.from(Persona.class);

        query.where(criteriaBuilder.equal(raiz.get("cedulaRuc"), cedulaRuc));
        return entityManager.createQuery(query).setMaxResults(1).getResultStream().findFirst();
    }

    public void crear(Persona persona) {
        entityManager.persist(persona);
    }
}
