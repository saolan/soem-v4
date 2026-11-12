package com.saolan.soem.configuracion.modelo;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * {@code alcanc} ({@link AlcancePrecio}) decide, solo al crear la lista, si
 * se generan los precios en cero de todos los productos activos; despues es
 * inmutable.
 */
@Entity
@Table(name = "precio")
public class Precio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "precio_id")
    private Integer precioId;

    @Column(name = "codigo", nullable = false, length = 32)
    private String codigo;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcanc", nullable = false, length = 12, updatable = false)
    private AlcancePrecio alcanc;

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

    public AlcancePrecio getAlcanc() {
        return alcanc;
    }

    public void setAlcanc(AlcancePrecio alcanc) {
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
