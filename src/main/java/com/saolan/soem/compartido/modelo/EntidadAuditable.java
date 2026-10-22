package com.saolan.soem.compartido.modelo;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Campos de auditoria comunes a las tablas transaccionales del esquema.
 * <p>
 * Los cuatro los asigna exclusivamente PostgreSQL (migracion V6): al
 * insertar, {@code DEFAULT soem_usuario_actual()} y {@code now()}; al
 * modificar, el trigger {@code trg_marca_modificacion()}. El usuario es el
 * autenticado en la API, publicado por
 * {@code compartido.seguridad.InterceptorUsuarioAuditoria}. La aplicacion
 * nunca los escribe ({@code insertable = false}, {@code updatable = false}):
 * en una entidad recien persistida o modificada valen lo que se leyo de la
 * BD; si un caso de uso necesita devolverlos, el Service debe hacer
 * {@code flush()} + {@code refresh()} de la entidad.
 */
@MappedSuperclass
public abstract class EntidadAuditable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "usuari_regi", nullable = false, insertable = false, updatable = false)
    private String usuariRegi;

    @Column(name = "fecha_hora_regi", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaHoraRegi;

    @Column(name = "usuari_modi", insertable = false, updatable = false)
    private String usuariModi;

    @Column(name = "fecha_hora_modi", insertable = false, updatable = false)
    private OffsetDateTime fechaHoraModi;

    public String getUsuariRegi() {
        return usuariRegi;
    }

    public OffsetDateTime getFechaHoraRegi() {
        return fechaHoraRegi;
    }

    public String getUsuariModi() {
        return usuariModi;
    }

    public OffsetDateTime getFechaHoraModi() {
        return fechaHoraModi;
    }
}
