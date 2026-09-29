package com.saolan.soem.configuracion.modelo;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Configuracion del certificado de firma electronica de una sucursal. Tabla de
 * especializacion 1:1: su clave primaria es a la vez clave foranea hacia
 * {@code SUCURSAL} (sin IDENTITY propia). Vive y muere junto con su
 * {@link Sucursal} (composicion, sin ESTADO ni auditoria propios).
 * <p>
 * {@code clave} es un campo sensible (contrasena del archivo del certificado):
 * nunca debe salir en el DTO de respuesta.
 */
@Entity
@Table(name = "sucu_cert_emis")
public class SucuCertEmis implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private Integer sucursalId;

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "sucursal_id")
	private Sucursal sucursal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cert_emis_id", nullable = false)
	private CertEmis certEmis;

	@Column(name = "ruta", nullable = false, length = 100)
	private String ruta;

	@Column(name = "descri", nullable = false, length = 100)
	private String descri;

	@Column(name = "clave", nullable = false, length = 32)
	private String clave;

	public Integer getSucursalId() {
		return sucursalId;
	}

	public Sucursal getSucursal() {
		return sucursal;
	}

	public void setSucursal(Sucursal sucursal) {
		this.sucursal = sucursal;
	}

	public CertEmis getCertEmis() {
		return certEmis;
	}

	public void setCertEmis(CertEmis certEmis) {
		this.certEmis = certEmis;
	}

	public String getRuta() {
		return ruta;
	}

	public void setRuta(String ruta) {
		this.ruta = ruta;
	}

	public String getDescri() {
		return descri;
	}

	public void setDescri(String descri) {
		this.descri = descri;
	}

	public String getClave() {
		return clave;
	}

	public void setClave(String clave) {
		this.clave = clave;
	}

	@Override
	public boolean equals(Object otro) {
		if (this == otro) {
			return true;
		}
		if (!(otro instanceof SucuCertEmis otroCertificado)) {
			return false;
		}
		return sucursalId != null && sucursalId.equals(otroCertificado.sucursalId);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
