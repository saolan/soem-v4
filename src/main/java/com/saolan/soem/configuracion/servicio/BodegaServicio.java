package com.saolan.soem.configuracion.servicio;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.configuracion.dto.BodegaDTO;
import com.saolan.soem.configuracion.dto.BodegaFiltroDTO;
import com.saolan.soem.configuracion.mapeador.BodegaMapeador;
import com.saolan.soem.configuracion.modelo.Bodega;
import com.saolan.soem.configuracion.repositorio.BodegaRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Reglas de negocio del catalogo {@link Bodega}:
 * <ul>
 * <li>{@code descri} unica (sin distinguir mayusculas): es la etiqueta del
 * combo que consumen sucursales, roles, inventario, compras y ventas.</li>
 * <li>Una bodega habilitada en alguna sucursal ({@code SUCU_BODE} con
 * {@code acceso}) no puede desactivarse, ni por DELETE ni por un PUT con
 * {@code estado=false}: primero debe quitarse de esas sucursales.</li>
 * </ul>
 */
@ApplicationScoped
public class BodegaServicio {

    @Inject
    private BodegaRepositorio bodegaRepositorio;

    @Inject
    private BodegaMapeador bodegaMapeador;

    public List<BodegaDTO> buscar(BodegaFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return bodegaRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(bodegaMapeador::aBodegaDTO)
                .toList();
    }

    public long contar(BodegaFiltroDTO filtro) {
        return bodegaRepositorio.contar(filtro);
    }

    public BodegaDTO buscarPorId(Integer bodegaId) {
        return bodegaMapeador.aBodegaDTO(obtenerOFallar(bodegaId));
    }

    public List<ComboDTO> combo() {
        return bodegaRepositorio.buscarCombo();
    }

    @Transactional
    public BodegaDTO crear(BodegaDTO bodegaDTO) {
        validarUnicidad(bodegaDTO.descri(), null);

        Bodega bodega = bodegaMapeador.aBodega(bodegaDTO);
        bodegaRepositorio.crear(bodega);
        return bodegaMapeador.aBodegaDTO(bodega);
    }

    @Transactional
    public BodegaDTO modificar(Integer bodegaId, BodegaDTO bodegaDTO) {
        Bodega bodega = obtenerOFallar(bodegaId);
        validarUnicidad(bodegaDTO.descri(), bodegaId);

        if (Boolean.TRUE.equals(bodega.getEstado()) && Boolean.FALSE.equals(bodegaDTO.estado())) {
            validarSinSucursalesAsignadas(bodegaId);
        }

        bodegaMapeador.actualizarEntidadDesdeDto(bodegaDTO, bodega);
        return bodegaMapeador.aBodegaDTO(bodega);
    }

    @Transactional
    public void eliminar(Integer bodegaId) {
        Bodega bodega = obtenerOFallar(bodegaId);
        validarSinSucursalesAsignadas(bodegaId);
        bodega.setEstado(Boolean.FALSE);
    }

    private Bodega obtenerOFallar(Integer bodegaId) {
        return bodegaRepositorio.buscarPorId(bodegaId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró la bodega solicitada"));
    }

    private void validarUnicidad(String descri, Integer idExcluido) {
        if (bodegaRepositorio.buscarPorDescri(descri, idExcluido).isPresent()) {
            throw new ExcepcionConflicto("La bodega entra en conflicto con datos existentes",
                    List.of(new ErrorCampo("descri", "Ya existe una bodega con esta descripción")));
        }
    }

    private void validarSinSucursalesAsignadas(Integer bodegaId) {
        long sucursalesAsignadas = bodegaRepositorio.contarSucursalesAsignadas(bodegaId);
        if (sucursalesAsignadas > 0) {
            String detalle = "La bodega está habilitada en " + sucursalesAsignadas
                    + (sucursalesAsignadas == 1 ? " sucursal" : " sucursales")
                    + "; quítela de ellas antes de desactivarla";
            throw new ExcepcionConflicto("No se puede desactivar la bodega",
                    List.of(new ErrorCampo("estado", detalle)));
        }
    }
}
