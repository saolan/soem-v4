package com.saolan.soem.seguridad.modelo;

import java.io.Serializable;
import java.math.BigDecimal;

import com.saolan.soem.compartido.seguridad.Accion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Permiso puntual del sistema: la combinacion {@code recurso}+{@code accion}
 * (introducida en la migracion V2) es la que {@code FiltroAutorizacion}
 * compara contra el claim {@code permisos} del JWT, con el formato
 * {@code recurso + ":" + accion.name().toLowerCase()}. No tiene columna
 * {@code estado}: la tabla original no la trajo y la "desactivacion" de un
 * permiso para un rol se modela con {@code rol_perm.acceso}, igual que el
 * resto de la familia {@code rol_*}.
 * <p>
 * {@code valor} es un campo heredado del esquema original sin semantica
 * definida para este modelo (ver comentario de columna en V2); se conserva
 * como nullable hasta que se decida su uso o se elimine.
 */
@Entity
@Table(name = "permiso")
public class Permiso implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permiso_id")
    private Integer permisoId;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Column(name = "valor", precision = 6, scale = 2)
    private BigDecimal valor;

    @Column(name = "recurso", nullable = false, length = 100)
    private String recurso;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 20)
    private Accion accion;

    public Integer getPermisoId() {
        return permisoId;
    }

    public void setPermisoId(Integer permisoId) {
        this.permisoId = permisoId;
    }

    public String getDescri() {
        return descri;
    }

    public void setDescri(String descri) {
        this.descri = descri;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }

    public Accion getAccion() {
        return accion;
    }

    public void setAccion(Accion accion) {
        this.accion = accion;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Permiso otroPermiso)) {
            return false;
        }
        return permisoId != null && permisoId.equals(otroPermiso.permisoId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
