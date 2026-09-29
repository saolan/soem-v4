package com.saolan.soem.seguridad.repositorio;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.saolan.soem.seguridad.modelo.Permiso;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolPerm;
import com.saolan.soem.seguridad.modelo.RolPersUsua;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

/**
 * Consultas sobre la habilitacion de permisos por rol ({@code rol_perm}). El
 * reemplazo en bloque de los permisos de un rol se administra directamente
 * sobre la coleccion {@code Rol#getPermisos()} desde {@code RolServicio}
 * (composicion con {@code orphanRemoval}); esta clase existe para la
 * consulta de resolucion de permisos usada al emitir un JWT.
 */
@ApplicationScoped
public class RolPermRepositorio {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Resuelve el conjunto de permisos vigentes de una persona, formateados
     * como {@code "recurso:accion"} (mismo formato que exige
     * {@code FiltroAutorizacion}): persona activa &rarr; roles activos
     * asignados &rarr; permisos habilitados de esos roles.
     */
    public Set<String> resolverPermisosDePersona(Integer personaId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Permiso> query = criteriaBuilder.createQuery(Permiso.class);
        Root<RolPersUsua> raiz = query.from(RolPersUsua.class);
        Join<RolPersUsua, Rol> rol = raiz.join("rol");
        Join<Rol, RolPerm> rolPerm = rol.join("permisos");
        Join<RolPerm, Permiso> permiso = rolPerm.join("permiso");

        query.select(permiso).distinct(true);
        query.where(
                criteriaBuilder.equal(raiz.get("usuario").get("personaId"), personaId),
                criteriaBuilder.isTrue(raiz.get("usuario").get("estado")),
                criteriaBuilder.isTrue(rol.get("estado")),
                criteriaBuilder.isTrue(rolPerm.get("acceso")));

        return entityManager.createQuery(query).getResultList().stream()
                .map(p -> p.getRecurso() + ":" + p.getAccion().name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
