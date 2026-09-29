package com.saolan.soem.configuracion.modelo;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Sucursal de la empresa. Raiz del agregado configuracion/sucursal; no
 * extiende {@code EntidadAuditable} porque la tabla no tiene columnas de
 * auditoria. Se da de baja logicamente mediante {@code estado}.
 */
@Entity
@Table(name = "sucursal")
public class Sucursal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sucursal_id")
    private Integer sucursalId;

    @Column(name = "codigo", nullable = false, length = 32)
    private String codigo;

    @Column(name = "matriz", nullable = false)
    private Boolean matriz;

    @Column(name = "descri", nullable = false, length = 100)
    private String descri;

    @Column(name = "ruc", nullable = false, length = 13)
    private String ruc;

    @Column(name = "razon_soci", nullable = false, length = 256)
    private String razonSoci;

    @Column(name = "nombre_come", length = 256)
    private String nombreCome;

    @Column(name = "ciudad", nullable = false, length = 100)
    private String ciudad;

    @Column(name = "direcc", nullable = false, length = 100)
    private String direcc;

    @Column(name = "direcc_esta", nullable = false, length = 100)
    private String direccEsta;

    @Column(name = "telef1", nullable = false, length = 10)
    private String telef1;

    @Column(name = "telef2", length = 10)
    private String telef2;

    @Column(name = "correo", nullable = false, length = 100)
    private String correo;

    @Column(name = "repres", nullable = false, length = 100)
    private String repres;

    @Column(name = "repres_cedu", nullable = false, length = 13)
    private String represCedu;

    @Column(name = "repres_dire", nullable = false, length = 100)
    private String represDire;

    @Column(name = "ruc_cont", nullable = false, length = 13)
    private String rucCont;

    @Column(name = "contri_espe", length = 13)
    private String contriEspe;

    @Column(name = "obliga_cont")
    private Boolean obligaCont;

    @Column(name = "logoti", length = 255)
    private String logoti;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    /** Composicion 1:1 opcional: configuracion del certificado de firma electronica. */
    @OneToOne(mappedBy = "sucursal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, optional = true)
    private SucuCertEmis certificadoEmisor;

    public Integer getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Integer sucursalId) {
        this.sucursalId = sucursalId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Boolean getMatriz() {
        return matriz;
    }

    public void setMatriz(Boolean matriz) {
        this.matriz = matriz;
    }

    public String getDescri() {
        return descri;
    }

    public void setDescri(String descri) {
        this.descri = descri;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSoci() {
        return razonSoci;
    }

    public void setRazonSoci(String razonSoci) {
        this.razonSoci = razonSoci;
    }

    public String getNombreCome() {
        return nombreCome;
    }

    public void setNombreCome(String nombreCome) {
        this.nombreCome = nombreCome;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getDirecc() {
        return direcc;
    }

    public void setDirecc(String direcc) {
        this.direcc = direcc;
    }

    public String getDireccEsta() {
        return direccEsta;
    }

    public void setDireccEsta(String direccEsta) {
        this.direccEsta = direccEsta;
    }

    public String getTelef1() {
        return telef1;
    }

    public void setTelef1(String telef1) {
        this.telef1 = telef1;
    }

    public String getTelef2() {
        return telef2;
    }

    public void setTelef2(String telef2) {
        this.telef2 = telef2;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRepres() {
        return repres;
    }

    public void setRepres(String repres) {
        this.repres = repres;
    }

    public String getRepresCedu() {
        return represCedu;
    }

    public void setRepresCedu(String represCedu) {
        this.represCedu = represCedu;
    }

    public String getRepresDire() {
        return represDire;
    }

    public void setRepresDire(String represDire) {
        this.represDire = represDire;
    }

    public String getRucCont() {
        return rucCont;
    }

    public void setRucCont(String rucCont) {
        this.rucCont = rucCont;
    }

    public String getContriEspe() {
        return contriEspe;
    }

    public void setContriEspe(String contriEspe) {
        this.contriEspe = contriEspe;
    }

    public Boolean getObligaCont() {
        return obligaCont;
    }

    public void setObligaCont(Boolean obligaCont) {
        this.obligaCont = obligaCont;
    }

    public String getLogoti() {
        return logoti;
    }

    public void setLogoti(String logoti) {
        this.logoti = logoti;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public SucuCertEmis getCertificadoEmisor() {
        return certificadoEmisor;
    }

    public void setCertificadoEmisor(SucuCertEmis certificadoEmisor) {
        this.certificadoEmisor = certificadoEmisor;
    }

    /**
     * Igualdad por identificador persistente (Vlad Mihalcea pattern): dos
     * transitorias (sin id) nunca son iguales entre si, y el hashCode es
     * constante para no romper el contrato de {@link java.util.HashSet}
     * cuando el id se asigna recien al persistir.
     */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Sucursal otraSucursal)) {
            return false;
        }
        return sucursalId != null && sucursalId.equals(otraSucursal.sucursalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
