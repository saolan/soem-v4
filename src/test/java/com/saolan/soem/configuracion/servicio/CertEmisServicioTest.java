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
import com.saolan.soem.configuracion.dto.CertEmisDTO;
import com.saolan.soem.configuracion.dto.CertEmisFiltroDTO;
import com.saolan.soem.configuracion.mapeador.CertEmisMapeador;
import com.saolan.soem.configuracion.mapeador.CertEmisMapeadorImpl;
import com.saolan.soem.configuracion.modelo.CertEmis;
import com.saolan.soem.configuracion.repositorio.CertEmisRepositorio;

/**
 * Tests unitarios de {@link CertEmisServicio}: mockea {@link CertEmisRepositorio}
 * y usa el mapper MapStruct real ({@link CertEmisMapeadorImpl}), igual que
 * {@code SucursalServicioTest}.
 */
@ExtendWith(MockitoExtension.class)
class CertEmisServicioTest {

    @Mock
    private CertEmisRepositorio certEmisRepositorio;

    @Spy
    private CertEmisMapeador certEmisMapeador = new CertEmisMapeadorImpl();

    @InjectMocks
    private CertEmisServicio certEmisServicio;

    // ---------- consultas ----------

    @Test
    void combo_deberiaDelegarEnElRepositorio() {
        when(certEmisRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "Certificado Test")));

        List<ComboDTO> resultado = certEmisServicio.combo();

        assertEquals(1, resultado.size());
        assertEquals("Certificado Test", resultado.get(0).etiqueta());
    }

    @Test
    void buscar_deberiaMapearResultadosADto() {
        CertEmisFiltroDTO filtro = new CertEmisFiltroDTO(null, null, null, true);
        ParametrosBusqueda parametros = new ParametrosBusqueda(1, 20, "descri", "asc");
        when(certEmisRepositorio.buscarPaginado(filtro, parametros)).thenReturn(List.of(certEmis(1, "Emisor A", true)));

        List<CertEmisDTO> resultado = certEmisServicio.buscar(filtro, parametros);

        assertEquals(1, resultado.size());
        assertEquals("Emisor A", resultado.get(0).descri());
    }

    @Test
    void buscarPorId_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(certEmisRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> certEmisServicio.buscarPorId(99));
    }

    // ---------- crear ----------

    @Test
    void crear_deberiaGuardar_cuandoDescriEsUnica() {
        CertEmisDTO dto = certEmisDTO("Emisor A", true);
        when(certEmisRepositorio.buscarPorDescri("Emisor A", null)).thenReturn(Optional.empty());

        CertEmisDTO resultado = certEmisServicio.crear(dto);

        assertEquals("Emisor A", resultado.descri());
        verify(certEmisRepositorio).crear(any(CertEmis.class));
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoDescriDuplicada() {
        CertEmisDTO dto = certEmisDTO("Emisor A", true);
        when(certEmisRepositorio.buscarPorDescri("Emisor A", null)).thenReturn(Optional.of(new CertEmis()));

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class, () -> certEmisServicio.crear(dto));

        assertEquals("descri", excepcion.getErrores().get(0).campo());
        verify(certEmisRepositorio, never()).crear(any());
    }

    // ---------- modificar ----------

    @Test
    void modificar_deberiaActualizarDatos_sinValidarUso_cuandoSigueActivo() {
        CertEmis existente = certEmis(1, "Emisor A", true);
        when(certEmisRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(certEmisRepositorio.buscarPorDescri("Emisor B", 1)).thenReturn(Optional.empty());

        CertEmisDTO resultado = certEmisServicio.modificar(1, certEmisDTO("Emisor B", true));

        assertEquals("Emisor B", resultado.descri());
        assertEquals(1, resultado.certEmisId());
        verify(certEmisRepositorio, never()).contarSucursalesAsignadas(any());
    }

    @Test
    void modificar_deberiaLanzarConflicto_cuandoDesactivaEmisorEnUso() {
        CertEmis existente = certEmis(1, "Emisor A", true);
        when(certEmisRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(certEmisRepositorio.buscarPorDescri("Emisor A", 1)).thenReturn(Optional.empty());
        when(certEmisRepositorio.contarSucursalesAsignadas(1)).thenReturn(2L);

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class,
                () -> certEmisServicio.modificar(1, certEmisDTO("Emisor A", false)));

        assertEquals("estado", excepcion.getErrores().get(0).campo());
        assertTrue(existente.getEstado());
    }

    @Test
    void modificar_deberiaPermitirDesactivar_cuandoNoEstaEnUso() {
        CertEmis existente = certEmis(1, "Emisor A", true);
        when(certEmisRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(certEmisRepositorio.buscarPorDescri("Emisor A", 1)).thenReturn(Optional.empty());
        when(certEmisRepositorio.contarSucursalesAsignadas(1)).thenReturn(0L);

        CertEmisDTO resultado = certEmisServicio.modificar(1, certEmisDTO("Emisor A", false));

        assertFalse(resultado.estado());
    }

    @Test
    void modificar_deberiaLanzarConflicto_cuandoDescriDuplicada() {
        when(certEmisRepositorio.buscarPorId(1)).thenReturn(Optional.of(certEmis(1, "Emisor A", true)));
        when(certEmisRepositorio.buscarPorDescri("Emisor B", 1)).thenReturn(Optional.of(new CertEmis()));

        assertThrows(ExcepcionConflicto.class, () -> certEmisServicio.modificar(1, certEmisDTO("Emisor B", true)));
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_deberiaMarcarEstadoFalse_cuandoNoEstaEnUso() {
        CertEmis existente = certEmis(1, "Emisor A", true);
        when(certEmisRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(certEmisRepositorio.contarSucursalesAsignadas(1)).thenReturn(0L);

        certEmisServicio.eliminar(1);

        assertFalse(existente.getEstado());
    }

    @Test
    void eliminar_deberiaLanzarConflicto_cuandoEstaAsignadoASucursales() {
        CertEmis existente = certEmis(1, "Emisor A", true);
        when(certEmisRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(certEmisRepositorio.contarSucursalesAsignadas(1)).thenReturn(1L);

        assertThrows(ExcepcionConflicto.class, () -> certEmisServicio.eliminar(1));
        assertTrue(existente.getEstado());
    }

    @Test
    void eliminar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(certEmisRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> certEmisServicio.eliminar(99));
    }

    private CertEmisDTO certEmisDTO(String descri, boolean estado) {
        return new CertEmisDTO(null, descri, "CN Test", "OU Test", "O Test", "EC", "1.2.3.4", estado);
    }

    private CertEmis certEmis(Integer id, String descri, boolean estado) {
        CertEmis certEmis = new CertEmis();
        certEmis.setCertEmisId(id);
        certEmis.setDescri(descri);
        certEmis.setCertCn("CN Test");
        certEmis.setCertOu("OU Test");
        certEmis.setCertO("O Test");
        certEmis.setCertC("EC");
        certEmis.setCertOid("1.2.3.4");
        certEmis.setEstado(estado);
        return certEmis;
    }
}
