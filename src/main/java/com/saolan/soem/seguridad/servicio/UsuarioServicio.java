package com.saolan.soem.seguridad.servicio;

import java.util.ArrayList;
import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.seguridad.dto.UsuarioCreacionDTO;
import com.saolan.soem.seguridad.dto.UsuarioDTO;
import com.saolan.soem.seguridad.dto.UsuarioFiltroDTO;
import com.saolan.soem.seguridad.mapeador.UsuarioMapeador;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolPersUsua;
import com.saolan.soem.seguridad.repositorio.PersonaRepositorio;
import com.saolan.soem.seguridad.repositorio.RolRepositorio;
import com.saolan.soem.seguridad.repositorio.UsuarioRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Reglas de negocio del agregado {@link PersUsua} (expuesto como "usuario"):
 * alta conjunta de {@link Persona}+{@link PersUsua}, unicidad de
 * {@code usuari} (restriccion tambien aplicada en BD por la migracion V3) y
 * la composicion {@code roles}, expuesta como sub-recurso independiente
 * ({@code /usuarios/{id}/roles}) pero administrada siempre dentro de la
 * transaccion de este mismo Service.
 */
@ApplicationScoped
public class UsuarioServicio {

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    @Inject
    private PersonaRepositorio personaRepositorio;

    @Inject
    private RolRepositorio rolRepositorio;

    @Inject
    private UsuarioMapeador usuarioMapeador;

    @Inject
    private ClaveServicio claveServicio;

    public List<UsuarioDTO> buscar(UsuarioFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return usuarioRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(usuarioMapeador::aUsuarioDTO)
                .toList();
    }

    public long contar(UsuarioFiltroDTO filtro) {
        return usuarioRepositorio.contar(filtro);
    }

    public UsuarioDTO buscarPorId(Integer personaId) {
        return usuarioMapeador.aUsuarioDTO(obtenerOFallar(personaId));
    }

    public List<ComboDTO> combo() {
        return usuarioRepositorio.buscarCombo();
    }

    @Transactional
    public UsuarioDTO crear(UsuarioCreacionDTO usuarioCreacionDTO) {
        validarUsuariUnico(usuarioCreacionDTO.usuari(), null);

        Persona persona = new Persona();
        persona.setCedulaRuc(usuarioCreacionDTO.cedulaRuc());
        persona.setApelli(usuarioCreacionDTO.apelli());
        persona.setNombre(usuarioCreacionDTO.nombre());
        persona.setDirecc(usuarioCreacionDTO.direcc());
        persona.setTelefo(usuarioCreacionDTO.telefo());
        persona.setCorreo(usuarioCreacionDTO.correo());
        persona.setFechaNaci(usuarioCreacionDTO.fechaNaci());
        persona.setEstado(Boolean.TRUE);
        personaRepositorio.crear(persona);

        PersUsua persUsua = new PersUsua();
        persUsua.setPersona(persona);
        persUsua.setUsuari(usuarioCreacionDTO.usuari());
        persUsua.setClave(claveServicio.hashear(usuarioCreacionDTO.clave()));
        persUsua.setCaducaClav(usuarioCreacionDTO.caducaClav());
        persUsua.setEstado(usuarioCreacionDTO.estado());
        usuarioRepositorio.crear(persUsua);

        return usuarioMapeador.aUsuarioDTO(persUsua);
    }

    @Transactional
    public UsuarioDTO modificar(Integer personaId, UsuarioDTO usuarioDTO) {
        PersUsua persUsua = obtenerOFallar(personaId);
        validarUsuariUnico(usuarioDTO.usuari(), personaId);

        usuarioMapeador.actualizarEntidadDesdeDto(usuarioDTO, persUsua);

        return usuarioMapeador.aUsuarioDTO(persUsua);
    }

    @Transactional
    public void eliminar(Integer personaId) {
        PersUsua persUsua = obtenerOFallar(personaId);
        persUsua.setEstado(Boolean.FALSE);
    }

    /**
     * Sub-recurso {@code /usuarios/{id}/roles}: reemplaza en bloque los
     * roles asignados a un usuario.
     */
    @Transactional
    public void reemplazarRoles(Integer personaId, List<Integer> rolIds) {
        PersUsua persUsua = obtenerOFallar(personaId);

        persUsua.getRoles().clear();
        for (Integer rolId : rolIds) {
            Rol rol = rolRepositorio.buscarPorId(rolId)
                    .orElseThrow(() -> excepcionDeCampo("rolIds", "El rol " + rolId + " no existe"));

            RolPersUsua asignacion = new RolPersUsua();
            asignacion.setRol(rol);
            asignacion.setUsuario(persUsua);
            persUsua.getRoles().add(asignacion);
        }
    }

    private PersUsua obtenerOFallar(Integer personaId) {
        return usuarioRepositorio.buscarPorId(personaId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró el usuario solicitado"));
    }

    private void validarUsuariUnico(String usuari, Integer idExcluido) {
        List<ErrorCampo> errores = new ArrayList<>();
        usuarioRepositorio.buscarPorUsuari(usuari)
                .filter(existente -> !existente.getPersonaId().equals(idExcluido))
                .ifPresent(existente -> errores.add(new ErrorCampo("usuari", "Ya existe un usuario con este nombre de acceso")));
        if (!errores.isEmpty()) {
            throw new ExcepcionConflicto("El usuario entra en conflicto con datos existentes", errores);
        }
    }

    private ExcepcionValidacion excepcionDeCampo(String campo, String mensaje) {
        return new ExcepcionValidacion(mensaje, List.of(new ErrorCampo(campo, mensaje)));
    }
}
