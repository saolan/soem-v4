package com.saolan.soem.configuracion.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.configuracion.dto.ParametroDTO;
import com.saolan.soem.configuracion.dto.ParametroFiltroDTO;
import com.saolan.soem.configuracion.mapeador.ParametroMapeador;
import com.saolan.soem.configuracion.mapeador.ParametroMapeadorImpl;
import com.saolan.soem.configuracion.modelo.Parametro;
import com.saolan.soem.configuracion.repositorio.ParametroRepositorio;

/**
 * Tests unitarios de {@link ParametroServicio}: mockea {@link ParametroRepositorio}
 * y usa el mapper MapStruct real ({@link ParametroMapeadorImpl}).
 */
@ExtendWith(MockitoExtension.class)
class ParametroServicioTest {

    @Mock
    private ParametroRepositorio parametroRepositorio;

    @Spy
    private ParametroMapeador parametroMapeador = new ParametroMapeadorImpl();

    @InjectMocks
    private ParametroServicio parametroServicio;

    // ---------- consultas ----------

    @Test
    void combo_deberiaDelegarEnElRepositorio() {
        when(parametroRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "Masculino")));

        List<ComboDTO> resultado = parametroServicio.combo();

        assertEquals(1, resultado.size());
        assertEquals("Masculino", resultado.get(0).etiqueta());
    }

    @Test
    void buscar_deberiaMapearResultadosADto() {
        ParametroFiltroDTO filtro = new ParametroFiltroDTO(null, null, true);
        ParametrosBusqueda parametros = new ParametrosBusqueda(1, 20, "descri", "asc");
        when(parametroRepositorio.buscarPaginado(filtro, parametros)).thenReturn(List.of(parametro(1, "M", "Masculino", true)));

        List<ParametroDTO> resultado = parametroServicio.buscar(filtro, parametros);

        assertEquals(1, resultado.size());
        assertEquals("Masculino", resultado.get(0).descri());
    }

    @Test
    void buscarPorId_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(parametroRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> parametroServicio.buscarPorId(99));
    }

    // ---------- crear ----------

    @Test
    void crear_deberiaGuardarYDevolverDto() {
        ParametroDTO resultado = parametroServicio.crear(parametroDTO("M", "Masculino", true));

        assertEquals("M", resultado.codigo());
        assertEquals("Masculino", resultado.descri());
        verify(parametroRepositorio).crear(any(Parametro.class));
    }

    // ---------- modificar ----------

    @Test
    void modificar_deberiaActualizarDatos_conservandoElId() {
        when(parametroRepositorio.buscarPorId(1)).thenReturn(Optional.of(parametro(1, "M", "Masculino", true)));

        ParametroDTO resultado = parametroServicio.modificar(1, parametroDTO("F", "Femenino", true));

        assertEquals(1, resultado.parametroId());
        assertEquals("F", resultado.codigo());
        assertEquals("Femenino", resultado.descri());
    }

    @Test
    void modificar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(parametroRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class,
                () -> parametroServicio.modificar(99, parametroDTO("M", "Masculino", true)));
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_deberiaMarcarEstadoFalse() {
        Parametro existente = parametro(1, "M", "Masculino", true);
        when(parametroRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));

        parametroServicio.eliminar(1);

        assertFalse(existente.getEstado());
    }

    @Test
    void eliminar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(parametroRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> parametroServicio.eliminar(99));
    }

    private ParametroDTO parametroDTO(String codigo, String descri, boolean estado) {
        return new ParametroDTO(null, codigo, descri, estado);
    }

    private Parametro parametro(Integer id, String codigo, String descri, boolean estado) {
        Parametro parametro = new Parametro();
        parametro.setParametroId(id);
        parametro.setCodigo(codigo);
        parametro.setDescri(descri);
        parametro.setEstado(estado);
        return parametro;
    }
}
