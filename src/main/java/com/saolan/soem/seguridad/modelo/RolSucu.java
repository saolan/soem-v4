package com.saolan.soem.seguridad.modelo;

import java.io.Serializable;

import com.saolan.soem.configuracion.modelo.Sucursal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Habilitacion de una {@link Sucursal} para un {@link Rol} (N:M con PK
 * propia y flag {@code acceso}): determina a que sucursales tiene acceso un
 * rol (y, por lo tanto, cualquier usuario que lo tenga asignado). Relacion
 * unidireccional hacia {@code configuracion.modelo.Sucursal} (sin coleccion
 * inversa en esa entidad, para no modificar el modulo {@code configuracion}).
 * Composicion: vive y muere junto con su {@link Rol} dueno.
 */
@Entity
@Table(name = "rol_sucu")
public class RolSucu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_sucu_id")
    private Integer rolSucuId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "acceso", nullable = false)
    private Boolean acceso;

    public Integer getRolSucuId() {
        return rolSucuId;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Boolean getAcceso() {
        return acceso;
    }

    public void setAcceso(Boolean acceso) {
        this.acceso = acceso;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof RolSucu otraAsignacion)) {
            return false;
        }
        return rolSucuId != null && rolSucuId.equals(otraAsignacion.rolSucuId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
