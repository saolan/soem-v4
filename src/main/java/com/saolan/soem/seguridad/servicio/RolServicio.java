package com.saolan.soem.seguridad.servicio;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.configuracion.repositorio.SucursalRepositorio;
import com.saolan.soem.seguridad.dto.AsignacionAccesoDTO;
import com.saolan.soem.seguridad.dto.RolDTO;
import com.saolan.soem.seguridad.dto.RolFiltroDTO;
import com.saolan.soem.seguridad.mapeador.RolMapeador;
import com.saolan.soem.seguridad.modelo.Permiso;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolPerm;
import com.saolan.soem.seguridad.modelo.RolSucu;
import com.saolan.soem.seguridad.repositorio.PermisoRepositorio;
import com.saolan.soem.seguridad.repositorio.RolRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Reglas de negocio del agregado {@link Rol}: CRUD basico y las
 * composiciones {@code permisos} ({@link RolPerm}) y {@code sucursales}
 * ({@link RolSucu}), expuestas como sub-recursos independientes pero
 * administradas siempre dentro de la transaccion de este mismo Service, sin
 * Service propio, para no permitir mutarlas sin validar que el permiso o la
 * sucursal referenciados existan.
 */
@ApplicationScoped
public class RolServicio {

    @Inject
    private RolRepositorio rolRepositorio;

    @Inject
    private PermisoRepositorio permisoRepositorio;

    @Inject
    private SucursalRepositorio sucursalRepositorio;

    @Inject
    private RolMapeador rolMapeador;

    public List<RolDTO> buscar(RolFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return rolRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(rolMapeador::aRolDTO)
                .toList();
    }

    public long contar(RolFiltroDTO filtro) {
        return rolRepositorio.contar(filtro);
    }

    public RolDTO buscarPorId(Integer rolId) {
        return rolMapeador.aRolDTO(obtenerOFallar(rolId));
    }

    public List<ComboDTO> combo() {
        return rolRepositorio.buscarCombo();
    }

    @Transactional
    public RolDTO crear(RolDTO rolDTO) {
        Rol rol = rolMapeador.aRol(rolDTO);
        rolRepositorio.crear(rol);
        return rolMapeador.aRolDTO(rol);
    }

    @Transactional
    public RolDTO modificar(Integer rolId, RolDTO rolDTO) {
        Rol rol = obtenerOFallar(rolId);
        rolMapeador.actualizarEntidadDesdeDto(rolDTO, rol);
        return rolMapeador.aRolDTO(rol);
    }

    @Transactional
    public void eliminar(Integer rolId) {
        Rol rol = obtenerOFallar(rolId);
        rol.setEstado(Boolean.FALSE);
    }

    /**
     * Sub-recurso {@code /roles/{id}/permisos}: reemplaza en bloque los
     * permisos habilitados para el rol.
     */
    @Transactional
    public void reemplazarPermisos(Integer rolId, List<AsignacionAccesoDTO> asignaciones) {
        Rol rol = obtenerOFallar(rolId);

        rol.getPermisos().clear();
        for (AsignacionAccesoDTO asignacion : asignaciones) {
            Permiso permiso = permisoRepositorio.buscarPorId(asignacion.valor())
                    .orElseThrow(() -> excepcionDeCampo("permisos", "El permiso " + asignacion.valor() + " no existe"));

            RolPerm rolPerm = new RolPerm();
            rolPerm.setRol(rol);
            rolPerm.setPermiso(permiso);
            rolPerm.setAcceso(asignacion.acceso());
            rol.getPermisos().add(rolPerm);
        }
    }

    /**
     * Sub-recurso {@code /roles/{id}/sucursales}: reemplaza en bloque las
     * sucursales habilitadas para el rol.
     */
    @Transactional
    public void reemplazarSucursales(Integer rolId, List<AsignacionAccesoDTO> asignaciones) {
        Rol rol = obtenerOFallar(rolId);

        rol.getSucursales().clear();
        for (AsignacionAccesoDTO asignacion : asignaciones) {
            Sucursal sucursal = sucursalRepositorio.buscarPorId(asignacion.valor())
                    .orElseThrow(() -> excepcionDeCampo("sucursales", "La sucursal " + asignacion.valor() + " no existe"));

            RolSucu rolSucu = new RolSucu();
            rolSucu.setRol(rol);
            rolSucu.setSucursal(sucursal);
            rolSucu.setAcceso(asignacion.acceso());
            rol.getSucursales().add(rolSucu);
        }
    }

    private Rol obtenerOFallar(Integer rolId) {
        return rolRepositorio.buscarPorId(rolId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró el rol solicitado"));
    }

    private ExcepcionValidacion excepcionDeCampo(String campo, String mensaje) {
        return new ExcepcionValidacion(mensaje, List.of(new ErrorCampo(campo, mensaje)));
    }
}
