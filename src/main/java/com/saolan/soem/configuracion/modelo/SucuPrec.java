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
 * Habilitacion de una lista de {@link Precio} para una {@link Sucursal} (N:M
 * con PK propia y flag {@code acceso}). Composicion: vive y muere junto con
 * su sucursal duena (ver {@code Sucursal#getPrecios()}); se reemplaza en
 * bloque desde {@code SucursalServicio}. La unicidad {@code (sucursal_id,
 * precio_id)} no esta declarada en el DDL, por lo que la garantiza el
 * reemplazo por diferencia.
 */
@Entity
@Table(name = "sucu_prec")
public class SucuPrec implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sucu_prec_id")
    private Integer sucuPrecId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "precio_id", nullable = false)
    private Precio precio;

    @Column(name = "acceso", nullable = false)
    private Boolean acceso;

    public Integer getSucuPrecId() {
        return sucuPrecId;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public Precio getPrecio() {
        return precio;
    }

    public void setPrecio(Precio precio) {
        this.precio = precio;
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
        if (!(otro instanceof SucuPrec otraAsignacion)) {
            return false;
        }
        return sucuPrecId != null && sucuPrecId.equals(otraAsignacion.sucuPrecId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
