package com.saolan.soem.configuracion.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.configuracion.dto.SucuCertEmisDTO;
import com.saolan.soem.configuracion.dto.SucursalDTO;
import com.saolan.soem.configuracion.dto.SucursalFiltroDTO;
import com.saolan.soem.configuracion.mapeador.SucuCertEmisMapeador;
import com.saolan.soem.configuracion.mapeador.SucuCertEmisMapeadorImpl;
import com.saolan.soem.configuracion.mapeador.SucursalMapeador;
import com.saolan.soem.configuracion.mapeador.SucursalMapeadorImpl;
import com.saolan.soem.configuracion.modelo.CertEmis;
import com.saolan.soem.configuracion.modelo.SucuCertEmis;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.configuracion.repositorio.CertEmisRepositorio;
import com.saolan.soem.configuracion.repositorio.SucursalRepositorio;

/**
 * Tests unitarios de {@link SucursalServicio}: mockea {@link SucursalRepositorio}
 * y {@link CertEmisRepositorio} (sin contenedor CDI ni base de datos) y usa los
 * mappers MapStruct reales ({@link SucursalMapeadorImpl}/{@link SucuCertEmisMapeadorImpl},
 * generados por el annotation processor al compilar), ya que su mapeo es
 * determinista y no aporta valor mockearlo.
 */
@ExtendWith(MockitoExtension.class)
class SucursalServicioTest {

    @Mock
    private SucursalRepositorio sucursalRepositorio;

    @Mock
    private CertEmisRepositorio certEmisRepositorio;

    @Spy
    private SucursalMapeador sucursalMapeador = new SucursalMapeadorImpl();

    @Spy
    private SucuCertEmisMapeador sucuCertEmisMapeador = new SucuCertEmisMapeadorImpl();

    @InjectMocks
    private SucursalServicio sucursalServicio;

    // ---------- crear ----------

    @Test
    void crear_deberiaGuardarSucursal_cuandoDatosValidos() {
        SucursalDTO dto = crearSucursalDTO(null, "S001", "1234567890001", false);
        when(sucursalRepositorio.buscarPorCodigo("S001", null)).thenReturn(Optional.empty());
        when(sucursalRepositorio.buscarPorRuc("1234567890001", null)).thenReturn(Optional.empty());

        SucursalDTO resultado = sucursalServicio.crear(dto);

        assertEquals("S001", resultado.codigo());
        assertEquals("1234567890001", resultado.ruc());
        verify(sucursalRepositorio).crear(any(Sucursal.class));
        verify(sucursalRepositorio, never()).buscarMatrizActual(any());
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoCodigoDuplicado() {
        SucursalDTO dto = crearSucursalDTO(null, "S001", "1234567890001", false);
        when(sucursalRepositorio.buscarPorCodigo("S001", null)).thenReturn(Optional.of(new Sucursal()));
        when(sucursalRepositorio.buscarPorRuc("1234567890001", null)).thenReturn(Optional.empty());

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class, () -> sucursalServicio.crear(dto));

        assertEquals(1, excepcion.getErrores().size());
        assertEquals("codigo", excepcion.getErrores().get(0).campo());
        verify(sucursalRepositorio, never()).crear(any());
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoRucDuplicado() {
        SucursalDTO dto = crearSucursalDTO(null, "S001", "1234567890001", false);
        when(sucursalRepositorio.buscarPorCodigo("S001", null)).thenReturn(Optional.empty());
        when(sucursalRepositorio.buscarPorRuc("1234567890001", null)).thenReturn(Optional.of(new Sucursal()));

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class, () -> sucursalServicio.crear(dto));

        assertEquals(1, excepcion.getErrores().size());
        assertEquals("ruc", excepcion.getErrores().get(0).campo());
        verify(sucursalRepositorio, never()).crear(any());
    }

    @Test
    void crear_deberiaLanzarConflictoConDosErrores_cuandoCodigoYRucDuplicados() {
        SucursalDTO dto = crearSucursalDTO(null, "S001", "1234567890001", false);
        when(sucursalRepositorio.buscarPorCodigo("S001", null)).thenReturn(Optional.of(new Sucursal()));
        when(sucursalRepositorio.buscarPorRuc("1234567890001", null)).thenReturn(Optional.of(new Sucursal()));

        ExcepcionConflicto excepcion = assertThrows(ExcepcionConflicto.class, () -> sucursalServicio.crear(dto));

        assertEquals(2, excepcion.getErrores().size());
    }

    @Test
    void crear_deberiaDesmarcarMatrizAnterior_cuandoNuevaEsMatriz() {
        SucursalDTO dto = crearSucursalDTO(null, "S001", "1234567890001", true);
        Sucursal matrizAnterior = crearSucursalEntidad(5, "S000", "9999999999999", true, true);
        when(sucursalRepositorio.buscarPorCodigo("S001", null)).thenReturn(Optional.empty());
        when(sucursalRepositorio.buscarPorRuc("1234567890001", null)).thenReturn(Optional.empty());
        when(sucursalRepositorio.buscarMatrizActual(null)).thenReturn(Optional.of(matrizAnterior));

        sucursalServicio.crear(dto);

        assertFalse(matrizAnterior.getMatriz());
    }

    // ---------- modificar ----------

    @Test
    void modificar_deberiaActualizarDatos() {
        Sucursal existente = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        SucursalDTO dto = crearSucursalDTO(1, "S002", "1234567890002", false);
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(sucursalRepositorio.buscarPorCodigo("S002", 1)).thenReturn(Optional.empty());
        when(sucursalRepositorio.buscarPorRuc("1234567890002", 1)).thenReturn(Optional.empty());

        SucursalDTO resultado = sucursalServicio.modificar(1, dto);

        assertEquals("S002", existente.getCodigo());
        assertEquals("S002", resultado.codigo());
    }

    @Test
    void modificar_deberiaLanzarNoEncontrado_cuandoSucursalNoExiste() {
        SucursalDTO dto = crearSucursalDTO(99, "S099", "1234567890099", false);
        when(sucursalRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> sucursalServicio.modificar(99, dto));
        verify(sucursalRepositorio, never()).buscarPorCodigo(any(), any());
    }

    @Test
    void modificar_deberiaLanzarConflicto_cuandoCodigoPerteneceAOtraSucursal() {
        Sucursal existente = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        SucursalDTO dto = crearSucursalDTO(1, "S002", "1234567890001", false);
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(sucursalRepositorio.buscarPorCodigo("S002", 1)).thenReturn(Optional.of(crearSucursalEntidad(2, "S002", "otro", false, true)));
        when(sucursalRepositorio.buscarPorRuc("1234567890001", 1)).thenReturn(Optional.empty());

        assertThrows(ExcepcionConflicto.class, () -> sucursalServicio.modificar(1, dto));
    }

    @Test
    void modificar_deberiaDesmarcarMatrizAnterior_cuandoPasaASerMatriz() {
        Sucursal existente = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        Sucursal matrizAnterior = crearSucursalEntidad(5, "S000", "9999999999999", true, true);
        SucursalDTO dto = crearSucursalDTO(1, "S001", "1234567890001", true);
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(sucursalRepositorio.buscarPorCodigo("S001", 1)).thenReturn(Optional.empty());
        when(sucursalRepositorio.buscarPorRuc("1234567890001", 1)).thenReturn(Optional.empty());
        when(sucursalRepositorio.buscarMatrizActual(1)).thenReturn(Optional.of(matrizAnterior));

        sucursalServicio.modificar(1, dto);

        assertFalse(matrizAnterior.getMatriz());
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_deberiaMarcarEstadoFalse() {
        Sucursal existente = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));

        sucursalServicio.eliminar(1);

        assertFalse(existente.getEstado());
    }

    @Test
    void eliminar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(sucursalRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> sucursalServicio.eliminar(99));
    }

    // ---------- consultas ----------

    @Test
    void buscarPorId_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(sucursalRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> sucursalServicio.buscarPorId(99));
    }

    @Test
    void buscarPorId_deberiaRetornarDTO_cuandoExiste() {
        Sucursal existente = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));

        SucursalDTO resultado = sucursalServicio.buscarPorId(1);

        assertEquals("S001", resultado.codigo());
    }

    @Test
    void buscar_deberiaDelegarEnRepositorioYMapear() {
        SucursalFiltroDTO filtro = new SucursalFiltroDTO(null, null, null, null, null, null);
        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(1, 20, "descri", "asc");
        when(sucursalRepositorio.buscarPaginado(filtro, parametrosBusqueda))
                .thenReturn(List.of(crearSucursalEntidad(1, "S001", "1234567890001", false, true)));

        List<SucursalDTO> resultado = sucursalServicio.buscar(filtro, parametrosBusqueda);

        assertEquals(1, resultado.size());
        assertEquals("S001", resultado.get(0).codigo());
    }

    @Test
    void contar_deberiaDelegarEnRepositorio() {
        SucursalFiltroDTO filtro = new SucursalFiltroDTO(null, null, null, null, null, null);
        when(sucursalRepositorio.contar(filtro)).thenReturn(5L);

        assertEquals(5L, sucursalServicio.contar(filtro));
    }

    @Test
    void combo_deberiaDelegarEnRepositorio() {
        when(sucursalRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "Sucursal Test")));

        List<ComboDTO> resultado = sucursalServicio.combo();

        assertEquals(1, resultado.size());
        assertEquals("Sucursal Test", resultado.get(0).etiqueta());
    }

    // ---------- certificado emisor ----------

    @Test
    void obtenerCertificadoEmisor_deberiaLanzarNoEncontrado_cuandoSucursalNoTieneCertificado() {
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));

        assertThrows(ExcepcionNoEncontrado.class, () -> sucursalServicio.obtenerCertificadoEmisor(1));
    }

    @Test
    void obtenerCertificadoEmisor_deberiaRetornarDTOSinClave_cuandoTieneCertificado() {
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        SucuCertEmis certificado = crearCertificadoEntidad(sucursal, crearCertEmisEntidad(10, true), "clave-secreta");
        sucursal.setCertificadoEmisor(certificado);
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));

        SucuCertEmisDTO resultado = sucursalServicio.obtenerCertificadoEmisor(1);

        assertEquals("ruta-test", resultado.ruta());
        assertNull(resultado.clave(), "la clave del certificado nunca debe salir en el DTO");
    }

    @Test
    void guardarCertificadoEmisor_deberiaCrearCertificadoNuevo_cuandoSucursalNoTieneUno() {
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        CertEmis certEmisActivo = crearCertEmisEntidad(10, true);
        SucuCertEmisDTO dto = new SucuCertEmisDTO(10, "ruta-nueva", "descripción nueva", "clave-nueva");
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));
        when(certEmisRepositorio.buscarPorId(10)).thenReturn(Optional.of(certEmisActivo));

        SucuCertEmisDTO resultado = sucursalServicio.guardarCertificadoEmisor(1, dto);

        assertEquals("ruta-nueva", resultado.ruta());
        assertEquals(sucursal, sucursal.getCertificadoEmisor().getSucursal());
        assertEquals("clave-nueva", sucursal.getCertificadoEmisor().getClave());
    }

    @Test
    void guardarCertificadoEmisor_deberiaLanzarValidacion_cuandoCertificadoNuevoSinClave() {
        // La validación de clave ocurre antes de resolver el CertEmis: no se
        // stubea certEmisRepositorio porque nunca llega a invocarse.
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        SucuCertEmisDTO dto = new SucuCertEmisDTO(10, "ruta-nueva", "descripción nueva", "");
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> sucursalServicio.guardarCertificadoEmisor(1, dto));

        assertEquals("certificadoEmisor.clave", excepcion.getErrores().get(0).campo());
    }

    @Test
    void guardarCertificadoEmisor_deberiaConservarClaveExistente_cuandoClaveLlegaVacia() {
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        CertEmis certEmisActivo = crearCertEmisEntidad(10, true);
        SucuCertEmis certificadoExistente = crearCertificadoEntidad(sucursal, certEmisActivo, "clave-existente");
        sucursal.setCertificadoEmisor(certificadoExistente);
        SucuCertEmisDTO dto = new SucuCertEmisDTO(10, "ruta-actualizada", "descripción actualizada", "");
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));
        when(certEmisRepositorio.buscarPorId(10)).thenReturn(Optional.of(certEmisActivo));

        sucursalServicio.guardarCertificadoEmisor(1, dto);

        assertEquals("clave-existente", sucursal.getCertificadoEmisor().getClave());
        assertEquals("ruta-actualizada", sucursal.getCertificadoEmisor().getRuta());
    }

    @Test
    void guardarCertificadoEmisor_deberiaLanzarValidacion_cuandoCertEmisNoExiste() {
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        SucuCertEmisDTO dto = new SucuCertEmisDTO(999, "ruta", "descripción", "clave");
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));
        when(certEmisRepositorio.buscarPorId(999)).thenReturn(Optional.empty());

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> sucursalServicio.guardarCertificadoEmisor(1, dto));

        assertEquals("certificadoEmisor.certEmisId", excepcion.getErrores().get(0).campo());
    }

    @Test
    void guardarCertificadoEmisor_deberiaLanzarValidacion_cuandoCertEmisInactivo() {
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        CertEmis certEmisInactivo = crearCertEmisEntidad(10, false);
        SucuCertEmisDTO dto = new SucuCertEmisDTO(10, "ruta", "descripción", "clave");
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));
        when(certEmisRepositorio.buscarPorId(10)).thenReturn(Optional.of(certEmisInactivo));

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> sucursalServicio.guardarCertificadoEmisor(1, dto));

        assertEquals("certificadoEmisor.certEmisId", excepcion.getErrores().get(0).campo());
    }

    @Test
    void eliminarCertificadoEmisor_deberiaQuitarCertificado_cuandoExiste() {
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        sucursal.setCertificadoEmisor(crearCertificadoEntidad(sucursal, crearCertEmisEntidad(10, true), "clave"));
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));

        sucursalServicio.eliminarCertificadoEmisor(1);

        assertNull(sucursal.getCertificadoEmisor());
    }

    @Test
    void eliminarCertificadoEmisor_deberiaLanzarNoEncontrado_cuandoNoTieneCertificado() {
        Sucursal sucursal = crearSucursalEntidad(1, "S001", "1234567890001", false, true);
        when(sucursalRepositorio.buscarPorId(1)).thenReturn(Optional.of(sucursal));

        assertThrows(ExcepcionNoEncontrado.class, () -> sucursalServicio.eliminarCertificadoEmisor(1));
    }

    // ---------- helpers ----------

    private SucursalDTO crearSucursalDTO(Integer sucursalId, String codigo, String ruc, boolean matriz) {
        return new SucursalDTO(sucursalId, codigo, matriz, "Sucursal Test", ruc, "Razón Social Test",
                null, "Quito", "Av. de Prueba 123", "Av. de Prueba 123", "0999999999", null,
                "correo@test.com", "Representante Test", "0999999999", "Dirección Representante Test",
                ruc, null, null, null, true);
    }

    private Sucursal crearSucursalEntidad(Integer sucursalId, String codigo, String ruc, boolean matriz, boolean estado) {
        Sucursal sucursal = new Sucursal();
        sucursal.setSucursalId(sucursalId);
        sucursal.setCodigo(codigo);
        sucursal.setRuc(ruc);
        sucursal.setMatriz(matriz);
        sucursal.setEstado(estado);
        sucursal.setDescri("Sucursal Test");
        sucursal.setRazonSoci("Razón Social Test");
        sucursal.setCiudad("Quito");
        sucursal.setDirecc("Av. de Prueba 123");
        sucursal.setDireccEsta("Av. de Prueba 123");
        sucursal.setTelef1("0999999999");
        sucursal.setCorreo("correo@test.com");
        sucursal.setRepres("Representante Test");
        sucursal.setRepresCedu("0999999999");
        sucursal.setRepresDire("Dirección Representante Test");
        sucursal.setRucCont(ruc);
        return sucursal;
    }

    private CertEmis crearCertEmisEntidad(Integer certEmisId, boolean estado) {
        CertEmis certEmis = new CertEmis();
        certEmis.setCertEmisId(certEmisId);
        certEmis.setEstado(estado);
        certEmis.setDescri("Certificado Test");
        certEmis.setCertCn("CN Test");
        certEmis.setCertOu("OU Test");
        certEmis.setCertO("O Test");
        certEmis.setCertC("EC");
        certEmis.setCertOid("1.2.3.4");
        return certEmis;
    }

    private SucuCertEmis crearCertificadoEntidad(Sucursal sucursal, CertEmis certEmis, String clave) {
        SucuCertEmis certificado = new SucuCertEmis();
        certificado.setSucursal(sucursal);
        certificado.setCertEmis(certEmis);
        certificado.setRuta("ruta-test");
        certificado.setDescri("Certificado de la sucursal");
        certificado.setClave(clave);
        return certificado;
    }
}
