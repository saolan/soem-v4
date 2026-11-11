package com.saolan.soem.configuracion.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.configuracion.dto.PrecioDTO;
import com.saolan.soem.configuracion.dto.PrecioFiltroDTO;
import com.saolan.soem.configuracion.mapeador.PrecioMapeador;
import com.saolan.soem.configuracion.mapeador.PrecioMapeadorImpl;
import com.saolan.soem.configuracion.modelo.Precio;
import com.saolan.soem.configuracion.repositorio.PrecioRepositorio;

/**
 * Tests unitarios de {@link PrecioServicio}: mockea {@link PrecioRepositorio}
 * y usa el mapper MapStruct real ({@link PrecioMapeadorImpl}), igual que
 * {@code BodegaServicioTest}.
 */
@ExtendWith(MockitoExtension.class)
class PrecioServicioTest {

    @Mock
    private PrecioRepositorio precioRepositorio;

    @Spy
    private PrecioMapeador precioMapeador = new PrecioMapeadorImpl();

    @InjectMocks
    private PrecioServicio precioServicio;

    // ---------- consultas ----------

    @Test
    void buscar_deberiaDelegarEnRepositorioYMapear() {
        PrecioFiltroDTO filtro = new PrecioFiltroDTO(null, null, null);
        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(1, 20, "descri", "asc");
        when(precioRepositorio.buscarPaginado(filtro, parametrosBusqueda))
                .thenReturn(List.of(nuevoPrecio(1, "Und", "Unidad", true)));

        List<PrecioDTO> resultado = precioServicio.buscar(filtro, parametrosBusqueda);

        assertEquals(1, resultado.size());
        assertEquals("Unidad", resultado.get(0).descri());
    }

    @Test
    void combo_deberiaDelegarEnRepositorio() {
        when(precioRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "Unidad")));

        assertEquals(1, precioServicio.combo().size());
    }

    @Test
    void buscarPorId_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(precioRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> precioServicio.buscarPorId(99));
    }

    // ---------- crear ----------

    @Test
    void crear_conAlcanceNinguno_noDeberiaGenerarPrecios() {
        PrecioDTO creado = precioServicio.crear(nuevoDTO(" DOC ", " Docena ", Precio.ALCANCE_NINGUNO, true));

        assertEquals("DOC", creado.codigo());
        assertEquals("Docena", creado.descri());
        verify(precioRepositorio).crear(any(Precio.class));
        verify(precioRepositorio, never()).habilitarEnSucursalesActivas(any());
        verify(precioRepositorio, never()).crearPreciosEnCeroDeProductosActivos(any());
    }

    @Test
    void crear_conAlcanceTodos_deberiaHabilitarSucursalesYGenerarPrecios() {
        doAnswer(invocacion -> {
            invocacion.<Precio>getArgument(0).setPrecioId(7);
            return null;
        }).when(precioRepositorio).crear(any(Precio.class));
        when(precioRepositorio.habilitarEnSucursalesActivas(7)).thenReturn(2);
        when(precioRepositorio.crearPreciosEnCeroDeProductosActivos(7)).thenReturn(50);

        PrecioDTO creado = precioServicio.crear(nuevoDTO("MAY", "Mayorista", Precio.ALCANCE_TODOS, true));

        assertEquals(7, creado.precioId());
        verify(precioRepositorio).habilitarEnSucursalesActivas(7);
        verify(precioRepositorio).crearPreciosEnCeroDeProductosActivos(7);
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoCodigoYDescriExisten() {
        when(precioRepositorio.buscarPorCodigo("Und", null)).thenReturn(Optional.of(nuevoPrecio(1, "Und", "Unidad", true)));
        when(precioRepositorio.buscarPorDescri("Unidad", null)).thenReturn(Optional.of(nuevoPrecio(1, "Und", "Unidad", true)));

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class,
                () -> precioServicio.crear(nuevoDTO("Und", "Unidad", Precio.ALCANCE_TODOS, true)));

        assertEquals(List.of("codigo", "descri"), excepcion.getErrores().stream().map(e -> e.campo()).toList());
        verify(precioRepositorio, never()).crear(any(Precio.class));
        verify(precioRepositorio, never()).crearPreciosEnCeroDeProductosActivos(anyInt());
    }

    // ---------- modificar ----------

    @Test
    void modificar_deberiaActualizarDatos_yConservarElAlcance() {
        Precio precio = nuevoPrecio(1, "Und", "Unidad", true);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));

        PrecioDTO modificado = precioServicio.modificar(1,
                new PrecioDTO(1, "UN", "Unidad suelta", Precio.ALCANCE_TODOS, new BigDecimal("2"), true));

        assertEquals("Unidad suelta", modificado.descri());
        assertEquals(0, new BigDecimal("2").compareTo(modificado.factor()));
        assertEquals(Precio.ALCANCE_NINGUNO, precio.getAlcanc());
        verify(precioRepositorio, never()).contarSucursalesAsignadas(1);
        verify(precioRepositorio, never()).crearPreciosEnCeroDeProductosActivos(any());
    }

    @Test
    void modificar_deberiaLanzarConflicto_cuandoSeDesactivaConSucursalesAsignadas() {
        Precio precio = nuevoPrecio(1, "Und", "Unidad", true);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));
        when(precioRepositorio.contarSucursalesAsignadas(1)).thenReturn(2L);

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class,
                () -> precioServicio.modificar(1, nuevoDTO("Und", "Unidad", Precio.ALCANCE_NINGUNO, false)));

        assertEquals("estado", excepcion.getErrores().get(0).campo());
        assertTrue(precio.getEstado());
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_deberiaMarcarEstadoFalse_cuandoNoTieneSucursales() {
        Precio precio = nuevoPrecio(1, "Und", "Unidad", true);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));
        when(precioRepositorio.contarSucursalesAsignadas(1)).thenReturn(0L);

        precioServicio.eliminar(1);

        assertFalse(precio.getEstado());
    }

    @Test
    void eliminar_deberiaLanzarConflicto_cuandoTieneSucursalesAsignadas() {
        Precio precio = nuevoPrecio(1, "Und", "Unidad", true);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));
        when(precioRepositorio.contarSucursalesAsignadas(1)).thenReturn(1L);

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class, () -> precioServicio.eliminar(1));

        assertTrue(excepcion.getErrores().get(0).detalle().contains("1 sucursal;"));
        assertTrue(precio.getEstado());
    }

    // ---------- helpers ----------

    private PrecioDTO nuevoDTO(String codigo, String descri, int alcanc, boolean estado) {
        return new PrecioDTO(null, codigo, descri, alcanc, BigDecimal.ONE, estado);
    }

    private Precio nuevoPrecio(Integer precioId, String codigo, String descri, boolean estado) {
        Precio precio = new Precio();
        precio.setPrecioId(precioId);
        precio.setCodigo(codigo);
        precio.setDescri(descri);
        precio.setAlcanc(Precio.ALCANCE_NINGUNO);
        precio.setFactor(BigDecimal.ONE);
        precio.setEstado(estado);
        return precio;
    }
}
