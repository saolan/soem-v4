package com.saolan.soem.seguridad.modelo;

import java.io.Serializable;

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
 * Habilitacion de un {@link Permiso} para un {@link Rol} (N:M con PK propia
 * y flag {@code acceso}). Composicion: vive y muere junto con su
 * {@code Rol} dueno (ver {@code Rol#getPermisos()}); el reemplazo en bloque
 * se hace siempre dentro de la transaccion de {@code RolServicio}.
 */
@Entity
@Table(name = "rol_perm")
public class RolPerm implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_perm_id")
    private Integer rolPermId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permiso_id", nullable = false)
    private Permiso permiso;

    @Column(name = "acceso", nullable = false)
    private Boolean acceso;

    public Integer getRolPermId() {
        return rolPermId;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Permiso getPermiso() {
        return permiso;
    }

    public void setPermiso(Permiso permiso) {
        this.permiso = permiso;
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
        if (!(otro instanceof RolPerm otraAsignacion)) {
            return false;
        }
        return rolPermId != null && rolPermId.equals(otraAsignacion.rolPermId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
