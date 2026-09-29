package com.saolan.soem.seguridad.servicio;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.seguridad.dto.PermisoDTO;
import com.saolan.soem.seguridad.dto.PermisoFiltroDTO;
import com.saolan.soem.seguridad.mapeador.PermisoMapeador;
import com.saolan.soem.seguridad.modelo.Permiso;
import com.saolan.soem.seguridad.repositorio.PermisoRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Reglas de negocio del catalogo {@link Permiso}: unicidad de
 * {@code recurso}+{@code accion} (restriccion tambien aplicada en BD por la
 * migracion V2). Sin {@code eliminar}: la tabla no tiene columna
 * {@code estado} (ver Javadoc de {@link Permiso}), y un borrado fisico
 * romperia cualquier {@code rol_perm} que ya lo referencie.
 */
@ApplicationScoped
public class PermisoServicio {

    @Inject
    private PermisoRepositorio permisoRepositorio;

    @Inject
    private PermisoMapeador permisoMapeador;

    public List<PermisoDTO> buscar(PermisoFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return permisoRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(permisoMapeador::aPermisoDTO)
                .toList();
    }

    public long contar(PermisoFiltroDTO filtro) {
        return permisoRepositorio.contar(filtro);
    }

    public PermisoDTO buscarPorId(Integer permisoId) {
        return permisoMapeador.aPermisoDTO(obtenerOFallar(permisoId));
    }

    public List<ComboDTO> combo() {
        return permisoRepositorio.buscarCombo();
    }

    @Transactional
    public PermisoDTO crear(PermisoDTO permisoDTO) {
        validarUnicidad(permisoDTO.recurso(), permisoDTO.accion(), null);

        Permiso permiso = permisoMapeador.aPermiso(permisoDTO);
        permisoRepositorio.crear(permiso);
        return permisoMapeador.aPermisoDTO(permiso);
    }

    @Transactional
    public PermisoDTO modificar(Integer permisoId, PermisoDTO permisoDTO) {
        Permiso permiso = obtenerOFallar(permisoId);
        validarUnicidad(permisoDTO.recurso(), permisoDTO.accion(), permisoId);

        permisoMapeador.actualizarEntidadDesdeDto(permisoDTO, permiso);
        return permisoMapeador.aPermisoDTO(permiso);
    }

    private Permiso obtenerOFallar(Integer permisoId) {
        return permisoRepositorio.buscarPorId(permisoId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró el permiso solicitado"));
    }

    private void validarUnicidad(String recurso, Accion accion, Integer idExcluido) {
        permisoRepositorio.buscarPorRecursoYAccion(recurso, accion, idExcluido)
                .ifPresent(existente -> {
                    throw new ExcepcionConflicto("Ya existe un permiso para este recurso y acción",
                            List.of(new ErrorCampo("recurso", "Ya existe un permiso para este recurso y acción")));
                });
    }
}
