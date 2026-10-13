package com.saolan.soem.configuracion.servicio;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.configuracion.dto.ParametroDTO;
import com.saolan.soem.configuracion.dto.ParametroFiltroDTO;
import com.saolan.soem.configuracion.mapeador.ParametroMapeador;
import com.saolan.soem.configuracion.modelo.Parametro;
import com.saolan.soem.configuracion.repositorio.ParametroRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Casos de uso del catalogo {@link Parametro}. Por decision de negocio el
 * {@code codigo} no es unico (el DDL no lo restringe) y la baja es logica
 * por {@code estado}.
 */
@ApplicationScoped
public class ParametroServicio {

    @Inject
    private ParametroRepositorio parametroRepositorio;

    @Inject
    private ParametroMapeador parametroMapeador;

    public List<ParametroDTO> buscar(ParametroFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return parametroRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(parametroMapeador::aParametroDTO)
                .toList();
    }

    public long contar(ParametroFiltroDTO filtro) {
        return parametroRepositorio.contar(filtro);
    }

    public ParametroDTO buscarPorId(Integer parametroId) {
        return parametroMapeador.aParametroDTO(obtenerOFallar(parametroId));
    }

    public List<ComboDTO> combo() {
        return parametroRepositorio.buscarCombo();
    }

    @Transactional
    public ParametroDTO crear(ParametroDTO parametroDTO) {
        Parametro parametro = parametroMapeador.aParametro(parametroDTO);
        parametroRepositorio.crear(parametro);
        return parametroMapeador.aParametroDTO(parametro);
    }

    @Transactional
    public ParametroDTO modificar(Integer parametroId, ParametroDTO parametroDTO) {
        Parametro parametro = obtenerOFallar(parametroId);
        parametroMapeador.actualizarEntidadDesdeDto(parametroDTO, parametro);
        return parametroMapeador.aParametroDTO(parametro);
    }

    @Transactional
    public void eliminar(Integer parametroId) {
        obtenerOFallar(parametroId).setEstado(Boolean.FALSE);
    }

    private Parametro obtenerOFallar(Integer parametroId) {
        return parametroRepositorio.buscarPorId(parametroId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró el parámetro solicitado"));
    }
}
