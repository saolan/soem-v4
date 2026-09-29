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
 * Asignacion de un {@link Rol} a un {@link PersUsua} (N:M con PK propia).
 * Composicion: vive y muere junto con su {@code PersUsua} duena (ver
 * {@code PersUsua#getRoles()}); el reemplazo en bloque de los roles de un
 * usuario se hace siempre dentro de la transaccion de {@code UsuarioServicio}.
 */
@Entity
@Table(name = "rol_pers_usua")
public class RolPersUsua implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_pers_usua_id")
    private Integer rolPersUsuaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private PersUsua usuario;

    public Integer getRolPersUsuaId() {
        return rolPersUsuaId;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public PersUsua getUsuario() {
        return usuario;
    }

    public void setUsuario(PersUsua usuario) {
        this.usuario = usuario;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof RolPersUsua otraAsignacion)) {
            return false;
        }
        return rolPersUsuaId != null && rolPersUsuaId.equals(otraAsignacion.rolPersUsuaId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
