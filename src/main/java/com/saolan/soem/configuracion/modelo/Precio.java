package com.saolan.soem.configuracion.modelo;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catalogo PRECIO: listas de precio (Unidad, Docena, Mayorista...) que puede
 * tener cada producto en {@code PROD_PREC}. Agregado propio del modulo
 * configuracion ({@code /api/v1/precios}), sin columnas de auditoria y con
 * borrado logico por {@code estado}. Se habilita por sucursal mediante
 * {@link SucuPrec}.
 * <p>
 * {@code alcanc} decide, solo al crear la lista, si se generan los precios
 * en cero de todos los productos activos ({@link #ALCANCE_TODOS}) o si el
 * usuario los ira creando ({@link #ALCANCE_NINGUNO}); despues es inmutable.
 */
@Entity
@Table(name = "precio")
public class Precio implements Serializable {

    private static final long serialVersionUID = 1L;

    /** No se generan precios al crear la lista. */
    public static final int ALCANCE_NINGUNO = 0;

    /** Se generan precios en cero para todos los productos activos en todas las sucursales activas. */
    public static final int ALCANCE_TODOS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "precio_id")
    private Integer precioId;

    @Column(name = "codigo", nullable = false, length = 32)
    private String codigo;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Column(name = "alcanc", nullable = false, updatable = false)
    private Integer alcanc;

    @Column(name = "factor", nullable = false, precision = 12, scale = 6)
    private BigDecimal factor;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    public Integer getPrecioId() {
        return precioId;
    }

    public void setPrecioId(Integer precioId) {
        this.precioId = precioId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescri() {
        return descri;
    }

    public void setDescri(String descri) {
        this.descri = descri;
    }

    public Integer getAlcanc() {
        return alcanc;
    }

    public void setAlcanc(Integer alcanc) {
        this.alcanc = alcanc;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
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
        if (!(otro instanceof Precio otroPrecio)) {
            return false;
        }
        return precioId != null && precioId.equals(otroPrecio.precioId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
