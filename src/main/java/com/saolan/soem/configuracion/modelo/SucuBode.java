package com.saolan.soem.configuracion.modelo;

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
 * Habilitacion de una {@link Bodega} para una {@link Sucursal} (N:M con PK
 * propia y flag {@code acceso}). Composicion: vive y muere junto con su
 * sucursal duena (ver {@code Sucursal#getBodegas()}); se reemplaza en bloque
 * desde {@code SucursalServicio}. La unicidad {@code (sucursal_id,
 * bodega_id)} solo esta propuesta en el DDL, por lo que la garantiza el
 * reemplazo por diferencia.
 */
@Entity
@Table(name = "sucu_bode")
public class SucuBode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sucu_bode_id")
    private Integer sucuBodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bodega_id", nullable = false)
    private Bodega bodega;

    @Column(name = "acceso", nullable = false)
    private Boolean acceso;

    public Integer getSucuBodeId() {
        return sucuBodeId;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public Bodega getBodega() {
        return bodega;
    }

    public void setBodega(Bodega bodega) {
        this.bodega = bodega;
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
        if (!(otro instanceof SucuBode otraAsignacion)) {
            return false;
        }
        return sucuBodeId != null && sucuBodeId.equals(otraAsignacion.sucuBodeId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
