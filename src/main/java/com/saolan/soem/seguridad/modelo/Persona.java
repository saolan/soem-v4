package com.saolan.soem.seguridad.modelo;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persona: tabla base compartida por varias especializaciones del sistema
 * (usuario, cliente, proveedor, cobrador, vendedor, cajero). Este modulo solo
 * mapea las columnas que ya trae el DDL y las usa exclusivamente como soporte
 * de {@link PersUsua} (el usuario del sistema); no expone un CRUD ni un combo
 * propios de Persona, para no duplicar trabajo cuando exista un futuro modulo
 * dedicado a terceros.
 */
@Entity
@Table(name = "persona")
public class Persona implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "persona_id")
	private Integer personaId;

	@Column(name = "cedula_ruc", nullable = false, length = 13)
	private String cedulaRuc;

	@Column(name = "apelli", nullable = false, length = 100)
	private String apelli;

	@Column(name = "nombre", length = 100)
	private String nombre;

	@Column(name = "direcc", length = 100)
	private String direcc;

	@Column(name = "telefo", length = 50)
	private String telefo;

	@Column(name = "correo", length = 100)
	private String correo;

	@Column(name = "fecha_naci")
	private LocalDate fechaNaci;

	@Column(name = "estado", nullable = false)
	private Boolean estado;

	public Integer getPersonaId() {
		return personaId;
	}

	public void setPersonaId(Integer personaId) {
		this.personaId = personaId;
	}

	public String getCedulaRuc() {
		return cedulaRuc;
	}

	public void setCedulaRuc(String cedulaRuc) {
		this.cedulaRuc = cedulaRuc;
	}

	public String getApelli() {
		return apelli;
	}

	public void setApelli(String apelli) {
		this.apelli = apelli;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDirecc() {
		return direcc;
	}

	public void setDirecc(String direcc) {
		this.direcc = direcc;
	}

	public String getTelefo() {
		return telefo;
	}

	public void setTelefo(String telefo) {
		this.telefo = telefo;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public LocalDate getFechaNaci() {
		return fechaNaci;
	}

	public void setFechaNaci(LocalDate fechaNaci) {
		this.fechaNaci = fechaNaci;
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
		if (!(otro instanceof Persona otraPersona)) {
			return false;
		}
		return personaId != null && personaId.equals(otraPersona.personaId);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
