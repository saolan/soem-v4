package com.saolan.soem.configuracion.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.saolan.soem.configuracion.dto.BodegaDTO;
import com.saolan.soem.configuracion.dto.BodegaFiltroDTO;
import com.saolan.soem.configuracion.mapeador.BodegaMapeador;
import com.saolan.soem.configuracion.mapeador.BodegaMapeadorImpl;
import com.saolan.soem.configuracion.modelo.Bodega;
import com.saolan.soem.configuracion.repositorio.BodegaRepositorio;

/**
 * Tests unitarios de {@link BodegaServicio}: mockea {@link BodegaRepositorio}
 * y usa el mapper MapStruct real ({@link BodegaMapeadorImpl}), igual que
 * {@code CertEmisServicioTest}.
 */
@ExtendWith(MockitoExtension.class)
class BodegaServicioTest {

    @Mock
    private BodegaRepositorio bodegaRepositorio;

    @Spy
    private BodegaMapeador bodegaMapeador = new BodegaMapeadorImpl();

    @InjectMocks
    private BodegaServicio bodegaServicio;

    // ---------- consultas ----------

    @Test
    void buscar_deberiaDelegarEnRepositorioYMapear() {
        BodegaFiltroDTO filtro = new BodegaFiltroDTO(null, null);
        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(1, 20, "descri", "asc");
        when(bodegaRepositorio.buscarPaginado(filtro, parametrosBusqueda)).thenReturn(List.of(nuevaBodega(1, "Bodega No.1", true)));

        List<BodegaDTO> resultado = bodegaServicio.buscar(filtro, parametrosBusqueda);

        assertEquals(1, resultado.size());
        assertEquals("Bodega No.1", resultado.get(0).descri());
    }

    @Test
    void combo_deberiaDelegarEnRepositorio() {
        when(bodegaRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "Bodega No.1")));

        assertEquals(1, bodegaServicio.combo().size());
    }

    @Test
    void buscarPorId_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(bodegaRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> bodegaServicio.buscarPorId(99));
    }

    // ---------- crear / modificar ----------

    @Test
    void crear_deberiaGuardarBodega_cuandoLaDescripcionEsUnica() {
        when(bodegaRepositorio.buscarPorDescri("Bodega Norte", null)).thenReturn(Optional.empty());

        BodegaDTO creada = bodegaServicio.crear(new BodegaDTO(null, "Bodega Norte", true));

        assertEquals("Bodega Norte", creada.descri());
        verify(bodegaRepositorio).crear(any(Bodega.class));
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoLaDescripcionExiste() {
        when(bodegaRepositorio.buscarPorDescri("Bodega No.1", null)).thenReturn(Optional.of(nuevaBodega(1, "Bodega No.1", true)));

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class,
                () -> bodegaServicio.crear(new BodegaDTO(null, "Bodega No.1", true)));
        assertEquals("descri", excepcion.getErrores().get(0).campo());
        verify(bodegaRepositorio, never()).crear(any(Bodega.class));
    }

    @Test
    void modificar_deberiaActualizarDatos() {
        Bodega bodega = nuevaBodega(1, "Bodega No.1", true);
        when(bodegaRepositorio.buscarPorId(1)).thenReturn(Optional.of(bodega));
        when(bodegaRepositorio.buscarPorDescri("Bodega Central", 1)).thenReturn(Optional.empty());

        BodegaDTO modificada = bodegaServicio.modificar(1, new BodegaDTO(1, "Bodega Central", true));

        assertEquals("Bodega Central", modificada.descri());
        verify(bodegaRepositorio, never()).contarSucursalesAsignadas(1);
    }

    @Test
    void modificar_deberiaLanzarConflicto_cuandoSeDesactivaConSucursalesAsignadas() {
        Bodega bodega = nuevaBodega(1, "Bodega No.1", true);
        when(bodegaRepositorio.buscarPorId(1)).thenReturn(Optional.of(bodega));
        when(bodegaRepositorio.buscarPorDescri("Bodega No.1", 1)).thenReturn(Optional.empty());
        when(bodegaRepositorio.contarSucursalesAsignadas(1)).thenReturn(2L);

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class,
                () -> bodegaServicio.modificar(1, new BodegaDTO(1, "Bodega No.1", false)));
        assertEquals("estado", excepcion.getErrores().get(0).campo());
        assertTrue(bodega.getEstado());
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_deberiaMarcarEstadoFalse_cuandoNoTieneSucursales() {
        Bodega bodega = nuevaBodega(1, "Bodega No.1", true);
        when(bodegaRepositorio.buscarPorId(1)).thenReturn(Optional.of(bodega));
        when(bodegaRepositorio.contarSucursalesAsignadas(1)).thenReturn(0L);

        bodegaServicio.eliminar(1);

        assertFalse(bodega.getEstado());
    }

    @Test
    void eliminar_deberiaLanzarConflicto_cuandoTieneSucursalesAsignadas() {
        Bodega bodega = nuevaBodega(1, "Bodega No.1", true);
        when(bodegaRepositorio.buscarPorId(1)).thenReturn(Optional.of(bodega));
        when(bodegaRepositorio.contarSucursalesAsignadas(1)).thenReturn(1L);

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class, () -> bodegaServicio.eliminar(1));
        assertTrue(excepcion.getErrores().get(0).detalle().contains("1 sucursal;"));
        assertTrue(bodega.getEstado());
    }

    // ---------- helpers ----------

    private Bodega nuevaBodega(Integer bodegaId, String descri, boolean estado) {
        Bodega bodega = new Bodega();
        bodega.setBodegaId(bodegaId);
        bodega.setDescri(descri);
        bodega.setEstado(estado);
        return bodega;
    }
}
