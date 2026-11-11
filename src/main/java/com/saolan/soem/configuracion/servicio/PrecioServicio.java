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
 * <li>Alcance "todos" ({@link Precio#ALCANCE_TODOS}): en la misma
 * transaccion del alta, la lista se habilita en todas las sucursales activas
 * y se crea el precio en cero de cada producto activo en cada una. Con
 * alcance "ninguno" no se genera nada. El alcance es inmutable.</li>
 * <li>Una lista habilitada en alguna sucursal ({@code SUCU_PREC} con
 * {@code acceso}) no puede desactivarse, ni por DELETE ni por un PUT con
 * {@code estado=false}: primero debe quitarse de esas sucursales.</li>
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
        precioRepositorio.crear(precio);
        if (precio.getAlcanc() == Precio.ALCANCE_TODOS) {
            generarAlcanceTodos(precio.getPrecioId());
        }
        return precioMapeador.aPrecioDTO(precio);
    }

    /** {@code alcanc} se ignora: es inmutable (ver {@link PrecioMapeador}). */
    @Transactional
    public PrecioDTO modificar(Integer precioId, PrecioDTO precioDTO) {
        Precio precio = obtenerOFallar(precioId);
        PrecioDTO normalizado = normalizar(precioDTO);
        validarUnicidad(normalizado.codigo(), normalizado.descri(), precioId);

        if (Boolean.TRUE.equals(precio.getEstado()) && Boolean.FALSE.equals(normalizado.estado())) {
            validarSinSucursalesAsignadas(precioId);
        }

        precioMapeador.actualizarEntidadDesdeDto(normalizado, precio);
        return precioMapeador.aPrecioDTO(precio);
    }

    @Transactional
    public void eliminar(Integer precioId) {
        Precio precio = obtenerOFallar(precioId);
        validarSinSucursalesAsignadas(precioId);
        precio.setEstado(Boolean.FALSE);
    }

    private void generarAlcanceTodos(Integer precioId) {
        int sucursales = precioRepositorio.habilitarEnSucursalesActivas(precioId);
        int precios = precioRepositorio.crearPreciosEnCeroDeProductosActivos(precioId);
        LOGGER.log(Level.INFO, "Lista de precio {0} con alcance todos: habilitada en {1} sucursales, {2} precios creados",
                new Object[] { precioId, sucursales, precios });
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

    private void validarSinSucursalesAsignadas(Integer precioId) {
        long sucursalesAsignadas = precioRepositorio.contarSucursalesAsignadas(precioId);
        if (sucursalesAsignadas > 0) {
            String detalle = "La lista de precio está habilitada en " + sucursalesAsignadas
                    + (sucursalesAsignadas == 1 ? " sucursal" : " sucursales")
                    + "; quítela de ellas antes de desactivarla";
            throw new ExcepcionConflicto("No se puede desactivar la lista de precio",
                    List.of(new ErrorCampo("estado", detalle)));
        }
    }
}
