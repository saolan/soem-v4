package com.saolan.soem.seguridad.modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Rol del sistema. No extiende {@code EntidadAuditable} porque la tabla no
 * tiene columnas de auditoria. Se da de baja logicamente mediante
 * {@code estado}. Es dueno de las composiciones {@code permisos}
 * ({@link RolPerm}), {@code sucursales} ({@link RolSucu}) y {@code menus}
 * ({@link RolMenu}): todas se reemplazan en bloque desde {@code RolServicio}.
 */
@Entity
@Table(name = "rol")
public class Rol implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    private Integer rolId;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    @OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RolPerm> permisos = new ArrayList<>();

    @OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RolSucu> sucursales = new ArrayList<>();

    @OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RolMenu> menus = new ArrayList<>();

    public Integer getRolId() {
        return rolId;
    }

    public void setRolId(Integer rolId) {
        this.rolId = rolId;
    }

    public String getDescri() {
        return descri;
    }

    public void setDescri(String descri) {
        this.descri = descri;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public List<RolPerm> getPermisos() {
        return permisos;
    }

    public List<RolSucu> getSucursales() {
        return sucursales;
    }

    public List<RolMenu> getMenus() {
        return menus;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Rol otroRol)) {
            return false;
        }
        return rolId != null && rolId.equals(otroRol.rolId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
