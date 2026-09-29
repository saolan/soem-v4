package com.saolan.soem.seguridad.repositorio;

import java.util.List;

import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolPersUsua;
import com.saolan.soem.seguridad.modelo.RolSucu;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

/**
 * Consultas sobre la habilitacion de sucursales por rol ({@code rol_sucu}).
 * El reemplazo en bloque de las sucursales de un rol se administra
 * directamente sobre la coleccion {@code Rol#getSucursales()} desde
 * {@code RolServicio}; esta clase existe para la consulta de resolucion de
 * la sucursal activa usada durante el login.
 */
@ApplicationScoped
public class RolSucuRepositorio {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Sucursales activas a las que una persona tiene acceso a traves de sus
     * roles activos, ordenadas con la matriz primero (criterio de
     * desempate al elegir la sucursal activa por defecto en el login).
     */
    public List<Sucursal> resolverSucursalesAccesibles(Integer personaId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Sucursal> query = criteriaBuilder.createQuery(Sucursal.class);
        Root<RolPersUsua> raiz = query.from(RolPersUsua.class);
        Join<RolPersUsua, Rol> rol = raiz.join("rol");
        Join<Rol, RolSucu> rolSucu = rol.join("sucursales");
        Join<RolSucu, Sucursal> sucursal = rolSucu.join("sucursal");

        query.select(sucursal).distinct(true);
        query.where(
                criteriaBuilder.equal(raiz.get("usuario").get("personaId"), personaId),
                criteriaBuilder.isTrue(raiz.get("usuario").get("estado")),
                criteriaBuilder.isTrue(rol.get("estado")),
                criteriaBuilder.isTrue(rolSucu.get("acceso")),
                criteriaBuilder.isTrue(sucursal.get("estado")));
        query.orderBy(criteriaBuilder.desc(sucursal.get("matriz")), criteriaBuilder.asc(sucursal.get("descri")));

        return entityManager.createQuery(query).getResultList();
    }
}
