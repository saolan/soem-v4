package com.saolan.soem.seguridad.modelo;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.saolan.soem.configuracion.modelo.Sucursal;

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
 * Refresh token de una sesion (tabla nueva {@code refresh_token}, migracion
 * V4). Nunca se persiste el valor crudo del token: {@code tokenHash} guarda
 * SHA-256(valor); el valor crudo solo viaja en la cookie HttpOnly del
 * cliente. Soporta rotacion ({@code tokenPadre}, cada renovacion invalida el
 * token actual y encadena el nuevo) y deteccion de reuso (un
 * {@code tokenHash} ya {@code revocado} indica un posible robo).
 * <p>
 * No extiende {@code EntidadAuditable}: su semantica propia
 * ({@code fechaHoraEmision}/{@code revocado}/{@code fechaHoraRevocado}) es
 * mas precisa aqui, y al no tener {@code fecha_hora_modi} no queda
 * alcanzada por el trigger {@code trg_marca_modificacion} (correcto: la
 * aplicacion controla estas columnas explicitamente).
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Integer refreshTokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private PersUsua usuario;

    /** Sucursal activa de la sesion al emitir este token, para preservar el contexto entre rotaciones. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_padre_id")
    private RefreshToken tokenPadre;

    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @Column(name = "fecha_hora_emision", nullable = false)
    private OffsetDateTime fechaHoraEmision;

    @Column(name = "fecha_hora_expira", nullable = false)
    private OffsetDateTime fechaHoraExpira;

    @Column(name = "revocado", nullable = false)
    private Boolean revocado;

    @Column(name = "fecha_hora_revocado")
    private OffsetDateTime fechaHoraRevocado;

    @Column(name = "motivo_revocacion", length = 30)
    private String motivoRevocacion;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "agente_usuario", length = 255)
    private String agenteUsuario;

    public Integer getRefreshTokenId() {
        return refreshTokenId;
    }

    public PersUsua getUsuario() {
        return usuario;
    }

    public void setUsuario(PersUsua usuario) {
        this.usuario = usuario;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public RefreshToken getTokenPadre() {
        return tokenPadre;
    }

    public void setTokenPadre(RefreshToken tokenPadre) {
        this.tokenPadre = tokenPadre;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public OffsetDateTime getFechaHoraEmision() {
        return fechaHoraEmision;
    }

    public void setFechaHoraEmision(OffsetDateTime fechaHoraEmision) {
        this.fechaHoraEmision = fechaHoraEmision;
    }

    public OffsetDateTime getFechaHoraExpira() {
        return fechaHoraExpira;
    }

    public void setFechaHoraExpira(OffsetDateTime fechaHoraExpira) {
        this.fechaHoraExpira = fechaHoraExpira;
    }

    public Boolean getRevocado() {
        return revocado;
    }

    public void setRevocado(Boolean revocado) {
        this.revocado = revocado;
    }

    public OffsetDateTime getFechaHoraRevocado() {
        return fechaHoraRevocado;
    }

    public void setFechaHoraRevocado(OffsetDateTime fechaHoraRevocado) {
        this.fechaHoraRevocado = fechaHoraRevocado;
    }

    public String getMotivoRevocacion() {
        return motivoRevocacion;
    }

    public void setMotivoRevocacion(String motivoRevocacion) {
        this.motivoRevocacion = motivoRevocacion;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public String getAgenteUsuario() {
        return agenteUsuario;
    }

    public void setAgenteUsuario(String agenteUsuario) {
        this.agenteUsuario = agenteUsuario;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof RefreshToken otroToken)) {
            return false;
        }
        return refreshTokenId != null && refreshTokenId.equals(otroToken.refreshTokenId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
