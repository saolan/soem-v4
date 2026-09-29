package com.saolan.soem.compartido.modelo;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Campos de auditoria comunes a las tablas transaccionales del esquema.
 * <p>
 * {@code usuariRegi}/{@code fechaHoraRegi} los fija la aplicacion al insertar
 * y son inmutables despues. {@code usuariModi}/{@code fechaHoraModi} los
 * gobierna exclusivamente el trigger {@code trg_marca_modificacion()} en cada
 * UPDATE: la aplicacion nunca los escribe ({@code insertable = false},
 * {@code updatable = false}); tras modificar un registro, el Service debe
 * refrescar la entidad para leer los valores que dejo el trigger.
 */
@MappedSuperclass
public abstract class EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "usuari_regi", nullable = false, updatable = false)
    private String usuariRegi;

    @Column(name = "fecha_hora_regi", nullable = false, updatable = false)
    private OffsetDateTime fechaHoraRegi;

    @Column(name = "usuari_modi", insertable = false, updatable = false)
    private String usuariModi;

    @Column(name = "fecha_hora_modi", insertable = false, updatable = false)
    private OffsetDateTime fechaHoraModi;

    public String getUsuariRegi() {
        return usuariRegi;
    }

    public void setUsuariRegi(String usuariRegi) {
        this.usuariRegi = usuariRegi;
    }

    public OffsetDateTime getFechaHoraRegi() {
        return fechaHoraRegi;
    }

    public void setFechaHoraRegi(OffsetDateTime fechaHoraRegi) {
        this.fechaHoraRegi = fechaHoraRegi;
    }

    public String getUsuariModi() {
        return usuariModi;
    }

    public OffsetDateTime getFechaHoraModi() {
        return fechaHoraModi;
    }
}
