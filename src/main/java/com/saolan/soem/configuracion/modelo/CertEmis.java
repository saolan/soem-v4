package com.saolan.soem.configuracion.modelo;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catalogo CERT_EMIS: emisores de certificados de firma electronica (datos
 * del sujeto X.509 usados para firmar comprobantes electronicos). Agregado
 * propio del modulo configuracion ({@code /api/v1/certificados-emisor}),
 * sin columnas de auditoria y con borrado logico por {@code estado}. Lo
 * referencia {@link SucuCertEmis}, que pertenece al agregado Sucursal.
 */
@Entity
@Table(name = "cert_emis")
public class CertEmis implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cert_emis_id")
    private Integer certEmisId;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Column(name = "cert_cn", nullable = false, length = 100)
    private String certCn;

    @Column(name = "cert_ou", nullable = false, length = 100)
    private String certOu;

    @Column(name = "cert_o", nullable = false, length = 100)
    private String certO;

    @Column(name = "cert_c", nullable = false, length = 100)
    private String certC;

    @Column(name = "cert_oid", nullable = false, length = 100)
    private String certOid;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    public Integer getCertEmisId() {
        return certEmisId;
    }

    public void setCertEmisId(Integer certEmisId) {
        this.certEmisId = certEmisId;
    }

    public String getDescri() {
        return descri;
    }

    public void setDescri(String descri) {
        this.descri = descri;
    }

    public String getCertCn() {
        return certCn;
    }

    public void setCertCn(String certCn) {
        this.certCn = certCn;
    }

    public String getCertOu() {
        return certOu;
    }

    public void setCertOu(String certOu) {
        this.certOu = certOu;
    }

    public String getCertO() {
        return certO;
    }

    public void setCertO(String certO) {
        this.certO = certO;
    }

    public String getCertC() {
        return certC;
    }

    public void setCertC(String certC) {
        this.certC = certC;
    }

    public String getCertOid() {
        return certOid;
    }

    public void setCertOid(String certOid) {
        this.certOid = certOid;
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
        if (!(otro instanceof CertEmis otroCertEmis)) {
            return false;
        }
        return certEmisId != null && certEmisId.equals(otroCertEmis.certEmisId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
