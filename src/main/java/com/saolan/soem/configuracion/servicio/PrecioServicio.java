package com.saolan.soem.configuracion.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ErrorCampo;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.configuracion.dto.PrecioDTO;
import com.saolan.soem.configuracion.dto.PrecioFiltroDTO;
import com.saolan.soem.configuracion.mapeador.PrecioMapeador;
import com.saolan.soem.configuracion.modelo.AlcancePrecio;
import com.saolan.soem.configuracion.modelo.Precio;
import com.saolan.soem.configuracion.repositorio.PrecioRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Reglas de negocio del catalogo {@link Precio}:
 * <ul>
 * <li>{@code codigo} y {@code descri} unicos (sin distinguir mayusculas y
 * sin espacios en los extremos); el DDL no declara esas restricciones.</li>
 * <li>Alta: la lista nace activa ({@code estado = true}, se ignora el
 * enviado). Con {@link AlcancePrecio#TODOS}, en la misma transaccion, se crea
 * el precio en cero de cada producto activo en cada sucursal activa
 * ({@code PROD_PREC}); con {@link AlcancePrecio#NINGUNO} no se crea nada. El
 * alta no habilita la lista en sucursales ({@code SUCU_PREC}).</li>
 * <li>Modificar: cambia {@code codigo}, {@code descri} y {@code factor};
 * {@code alcanc} (inmutable) y {@code estado} se ignoran. Nunca crea
 * precios.</li>
 * <li>{@code estado} solo cambia con {@link #eliminar(Integer)} y
 * {@link #activar(Integer)}, igual que en sucursal y sin restricciones: al
 * eliminar, los roles pierden el acceso a la lista ({@code ROL_PREC.acceso =
 * false}); al activar, lo recuperan en todas sus filas ({@code acceso =
 * true}).</li>
 * </ul>
 */
@ApplicationScoped
public class PrecioServicio {

    private static final Logger LOGGER = Logger.getLogger(PrecioServicio.class.getName());

    @Inject
    private PrecioRepositorio precioRepositorio;

    @Inject
    private PrecioMapeador precioMapeador;

    public List<PrecioDTO> buscar(PrecioFiltroDTO filtro, ParametrosBusqueda parametrosBusqueda) {
        return precioRepositorio.buscarPaginado(filtro, parametrosBusqueda).stream()
                .map(precioMapeador::aPrecioDTO)
                .toList();
    }

    public long contar(PrecioFiltroDTO filtro) {
        return precioRepositorio.contar(filtro);
    }

    public PrecioDTO buscarPorId(Integer precioId) {
        return precioMapeador.aPrecioDTO(obtenerOFallar(precioId));
    }

    public List<ComboDTO> combo() {
        return precioRepositorio.buscarCombo();
    }

    @Transactional
    public PrecioDTO crear(PrecioDTO precioDTO) {
        PrecioDTO normalizado = normalizar(precioDTO);
        validarUnicidad(normalizado.codigo(), normalizado.descri(), null);

        Precio precio = precioMapeador.aPrecio(normalizado);
        precio.setEstado(Boolean.TRUE);
        precioRepositorio.crear(precio);
        if (precio.getAlcanc() == AlcancePrecio.TODOS) {
            int precios = precioRepositorio.crearPreciosEnCeroDeProductosActivos(precio.getPrecioId());
            LOGGER.log(Level.INFO, "Lista de precio {0} con alcance todos: {1} precios creados",
                    new Object[] { precio.getPrecioId(), precios });
        }
        return precioMapeador.aPrecioDTO(precio);
    }

    /** {@code alcanc} y {@code estado} se ignoran (ver {@link PrecioMapeador}). */
    @Transactional
    public PrecioDTO modificar(Integer precioId, PrecioDTO precioDTO) {
        Precio precio = obtenerOFallar(precioId);
        PrecioDTO normalizado = normalizar(precioDTO);
        validarUnicidad(normalizado.codigo(), normalizado.descri(), precioId);

        precioMapeador.actualizarEntidadDesdeDto(normalizado, precio);
        return precioMapeador.aPrecioDTO(precio);
    }

    /** Borrado logico sin restriccion; idempotente si ya estaba inactiva. */
    @Transactional
    public void eliminar(Integer precioId) {
        Precio precio = obtenerOFallar(precioId);
        precio.setEstado(Boolean.FALSE);
        int roles = precioRepositorio.actualizarAccesoEnRoles(precioId, false);
        LOGGER.log(Level.INFO, "Lista de precio {0} eliminada: acceso quitado en {1} asignaciones de rol",
                new Object[] { precioId, roles });
    }

    /** Revierte el borrado logico; idempotente si ya estaba activa. */
    @Transactional
    public PrecioDTO activar(Integer precioId) {
        Precio precio = obtenerOFallar(precioId);
        precio.setEstado(Boolean.TRUE);
        int roles = precioRepositorio.actualizarAccesoEnRoles(precioId, true);
        LOGGER.log(Level.INFO, "Lista de precio {0} activada: acceso restablecido en {1} asignaciones de rol",
                new Object[] { precioId, roles });
        return precioMapeador.aPrecioDTO(precio);
    }

    private Precio obtenerOFallar(Integer precioId) {
        return precioRepositorio.buscarPorId(precioId)
                .orElseThrow(() -> new ExcepcionNoEncontrado("No se encontró la lista de precio solicitada"));
    }

    /** Los codigos y descripciones se comparan y guardan sin espacios en los extremos. */
    private PrecioDTO normalizar(PrecioDTO dto) {
        return new PrecioDTO(dto.precioId(), dto.codigo().strip(), dto.descri().strip(),
                dto.alcanc(), dto.factor(), dto.estado());
    }

    private void validarUnicidad(String codigo, String descri, Integer idExcluido) {
        List<ErrorCampo> errores = new ArrayList<>();
        if (precioRepositorio.buscarPorCodigo(codigo, idExcluido).isPresent()) {
            errores.add(new ErrorCampo("codigo", "Ya existe una lista de precio con este código"));
        }
        if (precioRepositorio.buscarPorDescri(descri, idExcluido).isPresent()) {
            errores.add(new ErrorCampo("descri", "Ya existe una lista de precio con esta descripción"));
        }
        if (!errores.isEmpty()) {
            throw new ExcepcionConflicto("La lista de precio entra en conflicto con datos existentes", errores);
        }
    }
}
