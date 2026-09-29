package com.saolan.soem.seguridad.modelo;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saolan.soem.compartido.modelo.EntidadAuditable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Usuario del sistema (tabla {@code pers_usua}): especializacion 1:1 de
 * {@link Persona}, con clave primaria compartida (sin identity propio). Es la
 * unica entidad de este agregado con las cuatro columnas de auditoria, por eso
 * es la unica que extiende {@link EntidadAuditable}.
 * <p>
 * {@code clave} es un campo sensible (hash BCrypt de la contrasena): nunca debe
 * salir en un DTO de respuesta ni registrarse en logs.
 * <p>
 * Es duena de la composicion {@code roles} ({@link RolPersUsua}): el reemplazo
 * en bloque de los roles de un usuario se administra siempre dentro de la
 * transaccion de {@code UsuarioServicio}.
 */
@Entity
@Table(name = "pers_usua")
public class PersUsua extends EntidadAuditable implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private Integer personaId;

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "persona_id")
	private Persona persona;

	@Column(name = "usuari", nullable = false, length = 100)
	private String usuari;

	@Column(name = "clave", nullable = false, length = 255)
	private String clave;

	@Column(name = "caduca_clav", nullable = false)
	private Boolean caducaClav;

	@Column(name = "caduca_fech_hora")
	private OffsetDateTime caducaFechHora;

	@Column(name = "estado", nullable = false)
	private Boolean estado;

	@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<RolPersUsua> roles = new ArrayList<>();

	public Integer getPersonaId() {
		return personaId;
	}

	public Persona getPersona() {
		return persona;
	}

	public void setPersona(Persona persona) {
		this.persona = persona;
	}

	public String getUsuari() {
		return usuari;
	}

	public void setUsuari(String usuari) {
		this.usuari = usuari;
	}

	public String getClave() {
		return clave;
	}

	public void setClave(String clave) {
		this.clave = clave;
	}

	public Boolean getCaducaClav() {
		return caducaClav;
	}

	public void setCaducaClav(Boolean caducaClav) {
		this.caducaClav = caducaClav;
	}

	public OffsetDateTime getCaducaFechHora() {
		return caducaFechHora;
	}

	public void setCaducaFechHora(OffsetDateTime caducaFechHora) {
		this.caducaFechHora = caducaFechHora;
	}

	public Boolean getEstado() {
		return estado;
	}

	public void setEstado(Boolean estado) {
		this.estado = estado;
	}

	public List<RolPersUsua> getRoles() {
		return roles;
	}

	@Override
	public boolean equals(Object otro) {
		if (this == otro) {
			return true;
		}
		if (!(otro instanceof PersUsua otroUsuario)) {
			return false;
		}
		return personaId != null && personaId.equals(otroUsuario.personaId);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
