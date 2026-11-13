package com.saolan.soem.configuracion.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.saolan.soem.configuracion.modelo.AlcancePrecio;
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
        PrecioDTO creado = precioServicio.crear(nuevoDTO(" DOC ", " Docena ", "NINGUNO", true));

        assertEquals("DOC", creado.codigo());
        assertEquals("Docena", creado.descri());
        verify(precioRepositorio).crear(any(Precio.class));
        verify(precioRepositorio, never()).crearPreciosEnCeroDeProductosActivos(any());
    }

    @Test
    void crear_conAlcanceTodos_deberiaGenerarPrecios() {
        doAnswer(invocacion -> {
            invocacion.<Precio>getArgument(0).setPrecioId(7);
            return null;
        }).when(precioRepositorio).crear(any(Precio.class));
        when(precioRepositorio.crearPreciosEnCeroDeProductosActivos(7)).thenReturn(50);

        PrecioDTO creado = precioServicio.crear(nuevoDTO("MAY", "Mayorista", "TODOS", true));

        assertEquals(7, creado.precioId());
        verify(precioRepositorio).crearPreciosEnCeroDeProductosActivos(7);
        verify(precioRepositorio, never()).actualizarAccesoEnRoles(any(), anyBoolean());
    }

    @Test
    void crear_deberiaCrearActiva_aunqueElDtoIndiqueInactiva() {
        PrecioDTO creado = precioServicio.crear(nuevoDTO("MAY", "Mayorista", "NINGUNO", false));

        assertTrue(creado.estado());
        verify(precioRepositorio).crear(argThat(precio -> Boolean.TRUE.equals(precio.getEstado())));
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoCodigoYDescriExisten() {
        when(precioRepositorio.buscarPorCodigo("Und", null)).thenReturn(Optional.of(nuevoPrecio(1, "Und", "Unidad", true)));
        when(precioRepositorio.buscarPorDescri("Unidad", null)).thenReturn(Optional.of(nuevoPrecio(1, "Und", "Unidad", true)));

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class,
                () -> precioServicio.crear(nuevoDTO("Und", "Unidad", "TODOS", true)));

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
                new PrecioDTO(1, "UN", "Unidad suelta", "TODOS", new BigDecimal("2"), true));

        assertEquals("Unidad suelta", modificado.descri());
        assertEquals(0, new BigDecimal("2").compareTo(modificado.factor()));
        assertEquals(AlcancePrecio.NINGUNO, precio.getAlcanc());
        verificarQueModificarNoTocaNada();
    }

    @Test
    void modificar_listaTodos_cambiandoCodigoYDescri_noDeberiaGenerarPrecios() {
        Precio precio = nuevoPrecio(1, "MAY", "Mayorista", AlcancePrecio.TODOS, true);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));

        PrecioDTO modificado = precioServicio.modificar(1, nuevoDTO("MAY2", "Mayorista 2", "NINGUNO", true));

        assertEquals("MAY2", modificado.codigo());
        assertEquals(AlcancePrecio.TODOS, precio.getAlcanc());
        verificarQueModificarNoTocaNada();
    }

    @Test
    void modificar_conEstadoFalse_noDeberiaDesactivar() {
        Precio precio = nuevoPrecio(1, "Und", "Unidad", true);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));

        precioServicio.modificar(1, nuevoDTO("Und", "Unidad", "NINGUNO", false));

        assertTrue(precio.getEstado());
        verificarQueModificarNoTocaNada();
    }

    @Test
    void modificar_conEstadoTrue_noDeberiaReactivar() {
        Precio precio = nuevoPrecio(1, "Und", "Unidad", false);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));

        precioServicio.modificar(1, nuevoDTO("Und", "Unidad", "NINGUNO", true));

        assertFalse(precio.getEstado());
        verificarQueModificarNoTocaNada();
    }

    // ---------- eliminar y activar ----------

    @Test
    void eliminar_deberiaDesactivarYQuitarElAccesoDeLosRoles_sinRestriccion() {
        Precio precio = nuevoPrecio(1, "Und", "Unidad", true);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));
        when(precioRepositorio.actualizarAccesoEnRoles(1, false)).thenReturn(3);

        precioServicio.eliminar(1);

        assertFalse(precio.getEstado());
        verify(precioRepositorio).actualizarAccesoEnRoles(1, false);
    }

    @Test
    void eliminar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(precioRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> precioServicio.eliminar(99));
        verify(precioRepositorio, never()).actualizarAccesoEnRoles(any(), anyBoolean());
    }

    @Test
    void activar_deberiaActivarYRestablecerElAccesoDeLosRoles() {
        Precio precio = nuevoPrecio(1, "MAY", "Mayorista", AlcancePrecio.TODOS, false);
        when(precioRepositorio.buscarPorId(1)).thenReturn(Optional.of(precio));

        PrecioDTO activado = precioServicio.activar(1);

        assertTrue(precio.getEstado());
        assertTrue(activado.estado());
        verify(precioRepositorio).actualizarAccesoEnRoles(1, true);
        verify(precioRepositorio, never()).crearPreciosEnCeroDeProductosActivos(any());
    }

    @Test
    void activar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(precioRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> precioServicio.activar(99));
        verify(precioRepositorio, never()).actualizarAccesoEnRoles(any(), anyBoolean());
    }

    // ---------- helpers ----------

    private PrecioDTO nuevoDTO(String codigo, String descri, String alcanc, boolean estado) {
        return new PrecioDTO(null, codigo, descri, alcanc, BigDecimal.ONE, estado);
    }

    /** Modificar no crea precios ni toca el acceso de los roles. */
    private void verificarQueModificarNoTocaNada() {
        verify(precioRepositorio, never()).crearPreciosEnCeroDeProductosActivos(any());
        verify(precioRepositorio, never()).actualizarAccesoEnRoles(any(), anyBoolean());
    }

    private Precio nuevoPrecio(Integer precioId, String codigo, String descri, boolean estado) {
        return nuevoPrecio(precioId, codigo, descri, AlcancePrecio.NINGUNO, estado);
    }

    private Precio nuevoPrecio(Integer precioId, String codigo, String descri, AlcancePrecio alcanc, boolean estado) {
        Precio precio = new Precio();
        precio.setPrecioId(precioId);
        precio.setCodigo(codigo);
        precio.setDescri(descri);
        precio.setAlcanc(alcanc);
        precio.setFactor(BigDecimal.ONE);
        precio.setEstado(estado);
        return precio;
    }
}
