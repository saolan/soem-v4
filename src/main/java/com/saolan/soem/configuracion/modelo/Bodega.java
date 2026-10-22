package com.saolan.soem.configuracion.modelo;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catalogo BODEGA. Agregado propio del modulo configuracion
 * ({@code /api/v1/bodegas}), sin columnas de auditoria y con borrado logico
 * por {@code estado}. Lo referencian la asignacion por sucursal
 * ({@link SucuBode}) y, en otros modulos, ingresos, egresos, kardex, saldos
 * por producto, roles y tomas fisicas: por eso expone su propio combo.
 */
@Entity
@Table(name = "bodega")
public class Bodega implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bodega_id")
    private Integer bodegaId;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    public Integer getBodegaId() {
        return bodegaId;
    }

    public void setBodegaId(Integer bodegaId) {
        this.bodegaId = bodegaId;
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

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Bodega otraBodega)) {
            return false;
        }
        return bodegaId != null && bodegaId.equals(otraBodega.bodegaId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
