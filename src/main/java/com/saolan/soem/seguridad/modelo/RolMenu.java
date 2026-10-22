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
 * Habilitacion de un {@link Menu} para un {@link Rol} (N:M con PK propia y
 * flag {@code acceso}, unica por {@code (rol_id, menu_id)}). Composicion:
 * vive y muere junto con su {@code Rol} dueno (ver {@code Rol#getMenus()});
 * se reemplaza en bloque desde {@code RolServicio}.
 */
@Entity
@Table(name = "rol_menu")
public class RolMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_menu_id")
    private Integer rolMenuId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(name = "acceso", nullable = false)
    private Boolean acceso;

    public Integer getRolMenuId() {
        return rolMenuId;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
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
        if (!(otro instanceof RolMenu otraAsignacion)) {
            return false;
        }
        return rolMenuId != null && rolMenuId.equals(otraAsignacion.rolMenuId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
