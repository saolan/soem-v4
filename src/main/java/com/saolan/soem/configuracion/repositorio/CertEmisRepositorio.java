package com.saolan.soem.configuracion.repositorio;

import java.util.List;
import java.util.Optional;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.configuracion.modelo.CertEmis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Acceso de solo lectura al catalogo externo {@link CertEmis}: combo para
 * consumidores externos y busqueda por id para que {@code SucursalServicio}
 * valide y asocie el emisor elegido.
 */
@ApplicationScoped
public class CertEmisRepositorio {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<CertEmis> buscarPorId(Integer certEmisId) {
        return Optional.ofNullable(entityManager.find(CertEmis.class, certEmisId));
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
}
