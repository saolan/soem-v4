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
 * Opcion del menu de la aplicacion. La jerarquia se define solo por
 * {@code menuSupe} (autorreferencia; {@code null} en los menus raiz): el
 * nivel se deriva de la profundidad y {@code detall} distingue una pagina
 * navegable ({@code true}, con {@code paginaRuta} del frontend) de un grupo
 * ({@code false}, sin ruta). No extiende {@code EntidadAuditable} porque la
 * tabla no tiene columnas de auditoria; se da de baja logicamente mediante
 * {@code estado}.
 */
@Entity
@Table(name = "menu")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Integer menuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_supe_id")
    private Menu menuSupe;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Column(name = "pagina_ruta", length = 255)
    private String paginaRuta;

    @Column(name = "detall", nullable = false)
    private Boolean detall;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    public Integer getMenuId() {
        return menuId;
    }

    public void setMenuId(Integer menuId) {
        this.menuId = menuId;
    }

    public Menu getMenuSupe() {
        return menuSupe;
    }

    public void setMenuSupe(Menu menuSupe) {
        this.menuSupe = menuSupe;
    }

    public String getDescri() {
        return descri;
    }

    public void setDescri(String descri) {
        this.descri = descri;
    }

    public String getPaginaRuta() {
        return paginaRuta;
    }

    public void setPaginaRuta(String paginaRuta) {
        this.paginaRuta = paginaRuta;
    }

    public Boolean getDetall() {
        return detall;
    }

    public void setDetall(Boolean detall) {
        this.detall = detall;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Menu otroMenu)) {
            return false;
        }
        return menuId != null && menuId.equals(otroMenu.menuId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
