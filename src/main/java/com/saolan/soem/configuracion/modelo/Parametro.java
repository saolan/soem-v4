package com.saolan.soem.configuracion.modelo;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catalogo PARAMETRO: valores genericos de seleccion (Masculino, Femenino,
 * Si, No, etc.). Agregado propio del modulo configuracion
 * ({@code /api/v1/parametros}), sin columnas de auditoria y con borrado
 * logico por {@code estado}. Lo referencian {@code PERS_PARA} y
 * {@code SUCU_PARA}, que lo consumen como combo.
 */
@Entity
@Table(name = "parametro")
public class Parametro implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parametro_id")
    private Integer parametroId;

    @Column(name = "codigo", nullable = false, length = 32)
    private String codigo;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    public Integer getParametroId() {
        return parametroId;
    }

    public void setParametroId(Integer parametroId) {
        this.parametroId = parametroId;
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
        if (!(otro instanceof Parametro otroParametro)) {
            return false;
        }
        return parametroId != null && parametroId.equals(otroParametro.parametroId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
