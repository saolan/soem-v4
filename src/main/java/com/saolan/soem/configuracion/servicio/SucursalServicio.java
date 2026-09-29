package com.saolan.soem.configuracion.servicio;

import java.util.ArrayList;
import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.configuracion.dto.SucuCertEmisDTO;
import com.saolan.soem.configuracion.dto.SucursalDTO;
import com.saolan.soem.configuracion.dto.SucursalFiltroDTO;
import com.saolan.soem.configuracion.mapeador.SucuCertEmisMapeador;
import com.saolan.soem.configuracion.mapeador.SucursalMapeador;
import com.saolan.soem.configuracion.modelo.CertEmis;
import com.saolan.soem.configuracion.modelo.SucuCertEmis;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.configuracion.repositorio.CertEmisRepositorio;
import com.saolan.soem.configuracion.repositorio.SucursalRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Reglas de negocio del agregado {@link Sucursal}: unicidad de
 * {@code codigo}/{@code ruc} (restricciones documentadas en el DDL pero no
 * declaradas en la base de datos), singleton de {@code matriz} y la
 * composicion 1:1 con {@link SucuCertEmis}, expuesta como sub-recurso
 * independiente ({@code /sucursales/{id}/certificado-emision}) pero
 * administrada siempre dentro de la transaccion de este mismo Service, sin
 * un Service propio, para no permitir mutarla sin las validaciones de la
 * sucursal duena.
 */
@ApplicationScoped
public class SucursalServicio {

    @Inject
    private SucursalRepositorio sucursalRepositorio;

    @Inject
    private CertEmisRepositorio certEmisRepositorio;

    @Inject
    private SucursalMapeador sucursalMapeador;

    @Inject
    private SucuCertEmisMapeador sucuCertEmisMapeador;

    public List<SucursalDTO> buscar(SucursalFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return sucursalRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(sucursalMapeador::aSucursalDTO)
                .toList();
    }

    public long contar(SucursalFiltroDTO filtro) {
        return sucursalRepositorio.contar(filtro);
    }

    public SucursalDTO buscarPorId(Integer sucursalId) {
        return sucursalMapeador.aSucursalDTO(obtenerOFallar(sucursalId));
    }

    public List<ComboDTO> combo() {
        return sucursalRepositorio.buscarCombo();
    }

    @Transactional
    public SucursalDTO crear(SucursalDTO sucursalDTO) {
        validarUnicidad(sucursalDTO.codigo(), sucursalDTO.ruc(), null);

        Sucursal sucursal = sucursalMapeador.aSucursal(sucursalDTO);

        if (Boolean.TRUE.equals(sucursal.getMatriz())) {
            desmarcarMatrizActual(null);
        }

        sucursalRepositorio.crear(sucursal);
        return sucursalMapeador.aSucursalDTO(sucursal);
    }

    @Transactional
    public SucursalDTO modificar(Integer sucursalId, SucursalDTO sucursalDTO) {
        Sucursal sucursal = obtenerOFallar(sucursalId);
        validarUnicidad(sucursalDTO.codigo(), sucursalDTO.ruc(), sucursalId);

        sucursalMapeador.actualizarEntidadDesdeDto(sucursalDTO, sucursal);

        if (Boolean.TRUE.equals(sucursal.getMatriz())) {
            desmarcarMatrizActual(sucursalId);
        }

        return sucursalMapeador.aSucursalDTO(sucursal);
    }

    @Transactional
    public void eliminar(Integer sucursalId) {
        Sucursal sucursal = obtenerOFallar(sucursalId);
        sucursal.setEstado(Boolean.FALSE);
    }

    private Sucursal obtenerOFallar(Integer sucursalId) {
        return sucursalRepositorio.buscarPorId(sucursalId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró la sucursal solicitada"));
    }

    private void validarUnicidad(String codigo, String ruc, Integer idExcluido) {
        List<ErrorCampo> errores = new ArrayList<>();
        if (sucursalRepositorio.buscarPorCodigo(codigo, idExcluido).isPresent()) {
            errores.add(new ErrorCampo("codigo", "Ya existe una sucursal con este código"));
        }
        if (sucursalRepositorio.buscarPorRuc(ruc, idExcluido).isPresent()) {
            errores.add(new ErrorCampo("ruc", "Ya existe una sucursal con este RUC"));
        }
        if (!errores.isEmpty()) {
            throw new ExcepcionConflicto("La sucursal entra en conflicto con datos existentes", errores);
        }
    }

    private void desmarcarMatrizActual(Integer idExcluido) {
        sucursalRepositorio.buscarMatrizActual(idExcluido)
                .ifPresent(actual -> actual.setMatriz(Boolean.FALSE));
    }

    /**
     * Sub-recurso {@code /sucursales/{id}/certificado-emision}: obtiene la
     * configuracion actual del certificado de firma electronica.
     */
    public SucuCertEmisDTO obtenerCertificadoEmisor(Integer sucursalId) {
        SucuCertEmis certificado = obtenerOFallar(sucursalId).getCertificadoEmisor();
        if (certificado == null) {
            throw new ExcepcionNoEncontrado("La sucursal no tiene un certificado de firma electrónica configurado");
        }
        return sucuCertEmisMapeador.aSucuCertEmisDTO(certificado);
    }

    /**
     * Crea o actualiza la composicion {@code certificadoEmisor} de la
     * sucursal. La {@code clave} solo se sobrescribe cuando viene no vacia;
     * de lo contrario se conserva la almacenada, salvo que se trate de un
     * certificado nuevo, donde la clave es obligatoria.
     */
    @Transactional
    public SucuCertEmisDTO guardarCertificadoEmisor(Integer sucursalId, SucuCertEmisDTO dto) {
        Sucursal sucursal = obtenerOFallar(sucursalId);

        SucuCertEmis certificado = sucursal.getCertificadoEmisor();
        if (certificado == null) {
            certificado = new SucuCertEmis();
            certificado.setSucursal(sucursal);
            sucursal.setCertificadoEmisor(certificado);
        }

        certificado.setRuta(dto.ruta());
        certificado.setDescri(dto.descri());
        if (!esVacia(dto.clave())) {
            certificado.setClave(dto.clave());
        }
        if (esVacia(certificado.getClave())) {
            throw excepcionDeCampo("certificadoEmisor.clave", "La clave del certificado es obligatoria");
        }

        certificado.setCertEmis(resolverCertEmisActivo(dto.certEmisId()));

        return sucuCertEmisMapeador.aSucuCertEmisDTO(certificado);
    }

    /**
     * Sub-recurso {@code /sucursales/{id}/certificado-emision}: quita el
     * certificado de la sucursal (el {@code orphanRemoval} de
     * {@link Sucursal#getCertificadoEmisor()} borra la fila).
     */
    @Transactional
    public void eliminarCertificadoEmisor(Integer sucursalId) {
        Sucursal sucursal = obtenerOFallar(sucursalId);
        if (sucursal.getCertificadoEmisor() == null) {
            throw new ExcepcionNoEncontrado("La sucursal no tiene un certificado de firma electrónica configurado");
        }
        sucursal.setCertificadoEmisor(null);
    }

    private CertEmis resolverCertEmisActivo(Integer certEmisId) {
        CertEmis certEmis = certEmisRepositorio.buscarPorId(certEmisId)
                .orElseThrow(() -> excepcionDeCampo("certificadoEmisor.certEmisId", "El certificado emisor indicado no existe"));
        if (!Boolean.TRUE.equals(certEmis.getEstado())) {
            throw excepcionDeCampo("certificadoEmisor.certEmisId", "El certificado emisor indicado está inactivo");
        }
        return certEmis;
    }

    private ExcepcionValidacion excepcionDeCampo(String campo, String mensaje) {
        return new ExcepcionValidacion(mensaje, List.of(new ErrorCampo(campo, mensaje)));
    }

    private boolean esVacia(String valor) {
        return valor == null || valor.isBlank();
    }
}
