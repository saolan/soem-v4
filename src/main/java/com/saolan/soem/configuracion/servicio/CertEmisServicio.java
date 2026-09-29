package com.saolan.soem.configuracion.servicio;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.configuracion.dto.CertEmisDTO;
import com.saolan.soem.configuracion.dto.CertEmisFiltroDTO;
import com.saolan.soem.configuracion.mapeador.CertEmisMapeador;
import com.saolan.soem.configuracion.modelo.CertEmis;
import com.saolan.soem.configuracion.repositorio.CertEmisRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Reglas de negocio del catalogo {@link CertEmis}:
 * <ul>
 * <li>{@code descri} unica (sin distinguir mayusculas): es la etiqueta del
 * combo que consume la pantalla de sucursal.</li>
 * <li>Un emisor asignado a alguna sucursal ({@code SUCU_CERT_EMIS}) no puede
 * desactivarse, ni por DELETE ni por un PUT con {@code estado=false}: primero
 * deben reasignarse esas sucursales.</li>
 * </ul>
 */
@ApplicationScoped
public class CertEmisServicio {

    @Inject
    private CertEmisRepositorio certEmisRepositorio;

    @Inject
    private CertEmisMapeador certEmisMapeador;

    public List<CertEmisDTO> buscar(CertEmisFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return certEmisRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(certEmisMapeador::aCertEmisDTO)
                .toList();
    }

    public long contar(CertEmisFiltroDTO filtro) {
        return certEmisRepositorio.contar(filtro);
    }

    public CertEmisDTO buscarPorId(Integer certEmisId) {
        return certEmisMapeador.aCertEmisDTO(obtenerOFallar(certEmisId));
    }

    public List<ComboDTO> combo() {
        return certEmisRepositorio.buscarCombo();
    }

    @Transactional
    public CertEmisDTO crear(CertEmisDTO certEmisDTO) {
        validarUnicidad(certEmisDTO.descri(), null);

        CertEmis certEmis = certEmisMapeador.aCertEmis(certEmisDTO);
        certEmisRepositorio.crear(certEmis);
        return certEmisMapeador.aCertEmisDTO(certEmis);
    }

    @Transactional
    public CertEmisDTO modificar(Integer certEmisId, CertEmisDTO certEmisDTO) {
        CertEmis certEmis = obtenerOFallar(certEmisId);
        validarUnicidad(certEmisDTO.descri(), certEmisId);

        if (Boolean.TRUE.equals(certEmis.getEstado()) && Boolean.FALSE.equals(certEmisDTO.estado())) {
            validarSinSucursalesAsignadas(certEmisId);
        }

        certEmisMapeador.actualizarEntidadDesdeDto(certEmisDTO, certEmis);
        return certEmisMapeador.aCertEmisDTO(certEmis);
    }

    @Transactional
    public void eliminar(Integer certEmisId) {
        CertEmis certEmis = obtenerOFallar(certEmisId);
        validarSinSucursalesAsignadas(certEmisId);
        certEmis.setEstado(Boolean.FALSE);
    }

    private CertEmis obtenerOFallar(Integer certEmisId) {
        return certEmisRepositorio.buscarPorId(certEmisId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró el certificado emisor solicitado"));
    }

    private void validarUnicidad(String descri, Integer idExcluido) {
        if (certEmisRepositorio.buscarPorDescri(descri, idExcluido).isPresent()) {
            throw new ExcepcionConflicto("El certificado emisor entra en conflicto con datos existentes",
                    List.of(new ErrorCampo("descri", "Ya existe un certificado emisor con esta descripción")));
        }
    }

    private void validarSinSucursalesAsignadas(Integer certEmisId) {
        long sucursalesAsignadas = certEmisRepositorio.contarSucursalesAsignadas(certEmisId);
        if (sucursalesAsignadas > 0) {
            String detalle = "El certificado emisor está asignado a " + sucursalesAsignadas
                    + (sucursalesAsignadas == 1 ? " sucursal" : " sucursales")
                    + "; reasígnelas antes de desactivarlo";
            throw new ExcepcionConflicto("No se puede desactivar el certificado emisor",
                    List.of(new ErrorCampo("estado", detalle)));
        }
    }
}
