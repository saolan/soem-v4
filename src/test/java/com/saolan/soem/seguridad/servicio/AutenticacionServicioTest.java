package com.saolan.soem.seguridad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.saolan.soem.compartido.excepcion.ExcepcionNoAutenticado;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.seguridad.dto.CambioClaveDTO;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.mapeador.UsuarioMapeador;
import com.saolan.soem.seguridad.mapeador.UsuarioMapeadorImpl;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;
import com.saolan.soem.seguridad.modelo.RefreshToken;
import com.saolan.soem.seguridad.repositorio.RefreshTokenRepositorio;
import com.saolan.soem.seguridad.repositorio.RolPermRepositorio;
import com.saolan.soem.seguridad.repositorio.RolSucuRepositorio;
import com.saolan.soem.seguridad.repositorio.UsuarioRepositorio;

/**
 * Tests unitarios de {@link AutenticacionServicio}: la clase con más lógica
 * de negocio del módulo seguridad (login, rotación de refresh token con
 * detección de reuso, logout, cambio de clave). Mockea todos los
 * repositorios y {@link ClaveServicio}/{@link TokenServicio}; usa
 * {@link UsuarioMapeador} real (vía {@link UsuarioMapeadorImpl}) como
 * {@code @Spy}.
 */
@ExtendWith(MockitoExtension.class)
class AutenticacionServicioTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private RefreshTokenRepositorio refreshTokenRepositorio;

    @Mock
    private RolPermRepositorio rolPermRepositorio;

    @Mock
    private RolSucuRepositorio rolSucuRepositorio;

    @Mock
    private ClaveServicio claveServicio;

    @Mock
    private TokenServicio tokenServicio;

    @Spy
    private UsuarioMapeador usuarioMapeador = new UsuarioMapeadorImpl();

    @InjectMocks
    private AutenticacionServicio autenticacionServicio;

    // ---------- login ----------

    @Test
    void login_deberiaEmitirSesion_cuandoCredencialesSonValidas() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash-almacenado", true);
        Sucursal sucursal = nuevaSucursal(10);
        when(usuarioRepositorio.buscarPorUsuari("usuario.test")).thenReturn(Optional.of(usuario));
        when(claveServicio.verificar("clave-correcta", "hash-almacenado")).thenReturn(true);
        when(rolSucuRepositorio.resolverSucursalesAccesibles(1)).thenReturn(List.of(sucursal));
        when(rolPermRepositorio.resolverPermisosDePersona(1)).thenReturn(Set.of("sucursales:buscar"));
        when(tokenServicio.emitirAccessToken(any(), any(), any())).thenReturn("access-token-123");
        when(tokenServicio.generarRefreshTokenCrudo()).thenReturn("refresh-crudo-123");
        when(tokenServicio.hashearRefreshToken("refresh-crudo-123")).thenReturn("hash-refresh-123");
        when(tokenServicio.duracionAccessTokenMinutos()).thenReturn(15L);
        when(tokenServicio.duracionRefreshTokenDias()).thenReturn(7L);

        ResultadoAutenticacion resultado = autenticacionServicio.login(
                new LoginRequestDTO("usuario.test", "clave-correcta", null), "127.0.0.1", "JUnit");

        assertEquals("refresh-crudo-123", resultado.refreshTokenCrudo());
        assertEquals("access-token-123", resultado.respuesta().accessToken());
        assertEquals(10, resultado.respuesta().sucursalId());
        assertEquals(900L, resultado.respuesta().expiraEnSegundos());
        assertTrue(resultado.respuesta().permisos().contains("sucursales:buscar"));
        assertEquals("usuario.test", resultado.respuesta().usuario().usuari());
        assertFalse(resultado.respuesta().claveCaducada());
        verify(refreshTokenRepositorio).crear(any());
    }

    @Test
    void login_deberiaMarcarClaveCaducada_cuandoCaducaClavVerdaderoYFechaVencida() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash-almacenado", true);
        usuario.setCaducaClav(Boolean.TRUE);
        usuario.setCaducaFechHora(OffsetDateTime.now().minusDays(1));
        Sucursal sucursal = nuevaSucursal(10);
        when(usuarioRepositorio.buscarPorUsuari("usuario.test")).thenReturn(Optional.of(usuario));
        when(claveServicio.verificar("clave-correcta", "hash-almacenado")).thenReturn(true);
        when(rolSucuRepositorio.resolverSucursalesAccesibles(1)).thenReturn(List.of(sucursal));
        when(rolPermRepositorio.resolverPermisosDePersona(1)).thenReturn(Set.of());
        when(tokenServicio.emitirAccessToken(any(), any(), any())).thenReturn("access-token");
        when(tokenServicio.generarRefreshTokenCrudo()).thenReturn("refresh-crudo");
        when(tokenServicio.hashearRefreshToken("refresh-crudo")).thenReturn("hash-refresh");
        when(tokenServicio.duracionAccessTokenMinutos()).thenReturn(15L);
        when(tokenServicio.duracionRefreshTokenDias()).thenReturn(7L);

        ResultadoAutenticacion resultado = autenticacionServicio.login(
                new LoginRequestDTO("usuario.test", "clave-correcta", null), null, null);

        assertTrue(resultado.respuesta().claveCaducada());
    }

    @Test
    void login_deberiaLanzarNoAutenticado_cuandoUsuarioNoExiste() {
        when(usuarioRepositorio.buscarPorUsuari("desconocido")).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoAutenticado.class,
                () -> autenticacionServicio.login(new LoginRequestDTO("desconocido", "clave", null), null, null));
        verify(claveServicio, never()).verificar(any(), any());
    }

    @Test
    void login_deberiaLanzarNoAutenticado_cuandoUsuarioEstaInactivo() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash", false);
        when(usuarioRepositorio.buscarPorUsuari("usuario.test")).thenReturn(Optional.of(usuario));

        assertThrows(ExcepcionNoAutenticado.class,
                () -> autenticacionServicio.login(new LoginRequestDTO("usuario.test", "clave", null), null, null));
        verify(claveServicio, never()).verificar(any(), any());
    }

    @Test
    void login_deberiaLanzarNoAutenticado_cuandoLaClaveEsIncorrecta() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash-almacenado", true);
        when(usuarioRepositorio.buscarPorUsuari("usuario.test")).thenReturn(Optional.of(usuario));
        when(claveServicio.verificar("clave-incorrecta", "hash-almacenado")).thenReturn(false);

        assertThrows(ExcepcionNoAutenticado.class,
                () -> autenticacionServicio.login(new LoginRequestDTO("usuario.test", "clave-incorrecta", null), null, null));
        verify(rolSucuRepositorio, never()).resolverSucursalesAccesibles(any());
    }

    @Test
    void login_deberiaLanzarValidacion_cuandoNoHaySucursalesAccesibles() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash-almacenado", true);
        when(usuarioRepositorio.buscarPorUsuari("usuario.test")).thenReturn(Optional.of(usuario));
        when(claveServicio.verificar("clave-correcta", "hash-almacenado")).thenReturn(true);
        when(rolSucuRepositorio.resolverSucursalesAccesibles(1)).thenReturn(List.of());

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> autenticacionServicio.login(new LoginRequestDTO("usuario.test", "clave-correcta", null), null, null));
        assertEquals("sucursalId", excepcion.getErrores().get(0).campo());
    }

    @Test
    void login_deberiaLanzarValidacion_cuandoLaSucursalSolicitadaNoEsAccesible() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash-almacenado", true);
        when(usuarioRepositorio.buscarPorUsuari("usuario.test")).thenReturn(Optional.of(usuario));
        when(claveServicio.verificar("clave-correcta", "hash-almacenado")).thenReturn(true);
        when(rolSucuRepositorio.resolverSucursalesAccesibles(1)).thenReturn(List.of(nuevaSucursal(10)));

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class, () -> autenticacionServicio.login(
                new LoginRequestDTO("usuario.test", "clave-correcta", 99), null, null));
        assertEquals("sucursalId", excepcion.getErrores().get(0).campo());
    }

    // ---------- refrescar ----------

    @Test
    void refrescar_deberiaRotarElToken_cuandoEsValido() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash", true);
        Sucursal sucursalPrevia = nuevaSucursal(10);
        RefreshToken tokenActual = nuevoRefreshToken(usuario, sucursalPrevia, false, OffsetDateTime.now().plusDays(1));
        when(tokenServicio.hashearRefreshToken("token-crudo")).thenReturn("hash-actual");
        when(refreshTokenRepositorio.buscarPorHash("hash-actual")).thenReturn(Optional.of(tokenActual));
        when(rolSucuRepositorio.resolverSucursalesAccesibles(1)).thenReturn(List.of(sucursalPrevia));
        when(rolPermRepositorio.resolverPermisosDePersona(1)).thenReturn(Set.of("sucursales:buscar"));
        when(tokenServicio.emitirAccessToken(any(), any(), any())).thenReturn("nuevo-access-token");
        when(tokenServicio.generarRefreshTokenCrudo()).thenReturn("nuevo-refresh-crudo");
        when(tokenServicio.hashearRefreshToken("nuevo-refresh-crudo")).thenReturn("hash-nuevo-refresh");
        when(tokenServicio.duracionAccessTokenMinutos()).thenReturn(15L);
        when(tokenServicio.duracionRefreshTokenDias()).thenReturn(7L);

        ResultadoAutenticacion resultado = autenticacionServicio.refrescar("token-crudo", "127.0.0.1", "JUnit");

        assertTrue(tokenActual.getRevocado());
        assertEquals("rotacion", tokenActual.getMotivoRevocacion());
        assertEquals("nuevo-access-token", resultado.respuesta().accessToken());
        assertEquals(10, resultado.respuesta().sucursalId());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepositorio).crear(captor.capture());
        assertEquals(tokenActual, captor.getValue().getTokenPadre());
    }

    @Test
    void refrescar_deberiaLanzarNoAutenticado_cuandoElTokenNoExiste() {
        when(tokenServicio.hashearRefreshToken("token-crudo")).thenReturn("hash-inexistente");
        when(refreshTokenRepositorio.buscarPorHash("hash-inexistente")).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoAutenticado.class, () -> autenticacionServicio.refrescar("token-crudo", null, null));
    }

    @Test
    void refrescar_deberiaRevocarTodosLosVigentes_cuandoElTokenYaEstabaRevocado() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash", true);
        RefreshToken tokenActual = nuevoRefreshToken(usuario, null, true, OffsetDateTime.now().plusDays(1));
        RefreshToken vigente1 = nuevoRefreshToken(usuario, null, false, OffsetDateTime.now().plusDays(1));
        RefreshToken vigente2 = nuevoRefreshToken(usuario, null, false, OffsetDateTime.now().plusDays(1));
        when(tokenServicio.hashearRefreshToken("token-robado")).thenReturn("hash-revocado");
        when(refreshTokenRepositorio.buscarPorHash("hash-revocado")).thenReturn(Optional.of(tokenActual));
        when(refreshTokenRepositorio.buscarVigentesDePersona(1)).thenReturn(List.of(vigente1, vigente2));

        assertThrows(ExcepcionNoAutenticado.class, () -> autenticacionServicio.refrescar("token-robado", null, null));

        assertTrue(vigente1.getRevocado());
        assertEquals("reuso_detectado", vigente1.getMotivoRevocacion());
        assertTrue(vigente2.getRevocado());
        assertEquals("reuso_detectado", vigente2.getMotivoRevocacion());
    }

    @Test
    void refrescar_deberiaLanzarNoAutenticado_cuandoElTokenEstaExpirado() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash", true);
        RefreshToken tokenActual = nuevoRefreshToken(usuario, null, false, OffsetDateTime.now().minusDays(1));
        when(tokenServicio.hashearRefreshToken("token-crudo")).thenReturn("hash-expirado");
        when(refreshTokenRepositorio.buscarPorHash("hash-expirado")).thenReturn(Optional.of(tokenActual));

        assertThrows(ExcepcionNoAutenticado.class, () -> autenticacionServicio.refrescar("token-crudo", null, null));
    }

    @Test
    void refrescar_deberiaLanzarNoAutenticado_cuandoElUsuarioEstaInactivo() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash", false);
        RefreshToken tokenActual = nuevoRefreshToken(usuario, null, false, OffsetDateTime.now().plusDays(1));
        when(tokenServicio.hashearRefreshToken("token-crudo")).thenReturn("hash-actual");
        when(refreshTokenRepositorio.buscarPorHash("hash-actual")).thenReturn(Optional.of(tokenActual));

        assertThrows(ExcepcionNoAutenticado.class, () -> autenticacionServicio.refrescar("token-crudo", null, null));
    }

    @Test
    void refrescar_deberiaReasignarSucursalPorDefecto_cuandoLaPreviaYaNoEsAccesible() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash", true);
        Sucursal sucursalPrevia = nuevaSucursal(10);
        Sucursal sucursalNueva = nuevaSucursal(20);
        RefreshToken tokenActual = nuevoRefreshToken(usuario, sucursalPrevia, false, OffsetDateTime.now().plusDays(1));
        when(tokenServicio.hashearRefreshToken("token-crudo")).thenReturn("hash-actual");
        when(refreshTokenRepositorio.buscarPorHash("hash-actual")).thenReturn(Optional.of(tokenActual));
        when(rolSucuRepositorio.resolverSucursalesAccesibles(1)).thenReturn(List.of(sucursalNueva));
        when(rolPermRepositorio.resolverPermisosDePersona(1)).thenReturn(Set.of());
        when(tokenServicio.emitirAccessToken(any(), any(), any())).thenReturn("access-token");
        when(tokenServicio.generarRefreshTokenCrudo()).thenReturn("refresh-crudo");
        when(tokenServicio.hashearRefreshToken("refresh-crudo")).thenReturn("hash-refresh");
        when(tokenServicio.duracionAccessTokenMinutos()).thenReturn(15L);
        when(tokenServicio.duracionRefreshTokenDias()).thenReturn(7L);

        ResultadoAutenticacion resultado = autenticacionServicio.refrescar("token-crudo", null, null);

        assertEquals(20, resultado.respuesta().sucursalId());
    }

    // ---------- logout ----------

    @Test
    void logout_deberiaRevocarElToken_cuandoExisteYNoEstabaRevocado() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash", true);
        RefreshToken token = nuevoRefreshToken(usuario, null, false, OffsetDateTime.now().plusDays(1));
        when(tokenServicio.hashearRefreshToken("token-crudo")).thenReturn("hash-actual");
        when(refreshTokenRepositorio.buscarPorHash("hash-actual")).thenReturn(Optional.of(token));

        autenticacionServicio.logout("token-crudo");

        assertTrue(token.getRevocado());
        assertEquals("logout", token.getMotivoRevocacion());
    }

    @Test
    void logout_noDeberiaFallar_cuandoElTokenNoExiste() {
        when(tokenServicio.hashearRefreshToken("token-crudo")).thenReturn("hash-inexistente");
        when(refreshTokenRepositorio.buscarPorHash("hash-inexistente")).thenReturn(Optional.empty());

        autenticacionServicio.logout("token-crudo");
    }

    // ---------- cambiarClave ----------

    @Test
    void cambiarClave_deberiaActualizarLaClave_cuandoLaClaveActualEsCorrecta() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash-viejo", true);
        when(usuarioRepositorio.buscarPorId(1)).thenReturn(Optional.of(usuario));
        when(claveServicio.verificar("clave-actual", "hash-viejo")).thenReturn(true);
        when(claveServicio.hashear("clave-nueva-segura")).thenReturn("hash-nuevo");

        autenticacionServicio.cambiarClave(1, new CambioClaveDTO("clave-actual", "clave-nueva-segura"));

        assertEquals("hash-nuevo", usuario.getClave());
        assertFalse(usuario.getCaducaClav());
        assertNull(usuario.getCaducaFechHora());
    }

    @Test
    void cambiarClave_deberiaLanzarValidacion_cuandoLaClaveActualEsIncorrecta() {
        PersUsua usuario = nuevoUsuario(1, "usuario.test", "hash-viejo", true);
        when(usuarioRepositorio.buscarPorId(1)).thenReturn(Optional.of(usuario));
        when(claveServicio.verificar("clave-incorrecta", "hash-viejo")).thenReturn(false);

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class, () -> autenticacionServicio
                .cambiarClave(1, new CambioClaveDTO("clave-incorrecta", "clave-nueva-segura")));
        assertEquals("claveActual", excepcion.getErrores().get(0).campo());
    }

    @Test
    void cambiarClave_deberiaLanzarNoAutenticado_cuandoElUsuarioNoExiste() {
        when(usuarioRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoAutenticado.class,
                () -> autenticacionServicio.cambiarClave(99, new CambioClaveDTO("clave-actual", "clave-nueva-segura")));
    }

    // ---------- helpers ----------

    private Persona nuevaPersona(Integer personaId) {
        Persona persona = new Persona();
        persona.setPersonaId(personaId);
        persona.setCedulaRuc("0999999999");
        persona.setApelli("Apellido Test");
        persona.setNombre("Nombre Test");
        persona.setEstado(Boolean.TRUE);
        return persona;
    }

    private PersUsua nuevoUsuario(Integer personaId, String usuari, String claveHash, boolean estado) {
        PersUsua usuario = new PersUsua();
        usuario.setPersona(nuevaPersona(personaId));
        usuario.setUsuari(usuari);
        usuario.setClave(claveHash);
        usuario.setCaducaClav(Boolean.FALSE);
        usuario.setEstado(estado);
        asignarPersonaId(usuario, personaId);
        return usuario;
    }

    private Sucursal nuevaSucursal(Integer sucursalId) {
        Sucursal sucursal = new Sucursal();
        sucursal.setSucursalId(sucursalId);
        sucursal.setDescri("Sucursal Test");
        sucursal.setEstado(Boolean.TRUE);
        return sucursal;
    }

    private RefreshToken nuevoRefreshToken(PersUsua usuario, Sucursal sucursal, boolean revocado, OffsetDateTime fechaHoraExpira) {
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setSucursal(sucursal);
        token.setRevocado(revocado);
        token.setFechaHoraExpira(fechaHoraExpira);
        token.setFechaHoraEmision(OffsetDateTime.now().minusMinutes(1));
        token.setTokenHash("hash-existente");
        return token;
    }

    /**
     * {@code PersUsua.personaId} no tiene setter propio (se sincroniza vía
     * {@code @MapsId} solo en un runtime JPA real); en un test unitario puro
     * hay que fijarlo por reflexión para que el resto del flujo (que navega
     * por {@code usuario.getPersonaId()}) tenga un valor coherente.
     */
    private void asignarPersonaId(PersUsua usuario, Integer personaId) {
        try {
            Field campo = PersUsua.class.getDeclaredField("personaId");
            campo.setAccessible(true);
            campo.set(usuario, personaId);
        } catch (ReflectiveOperationException excepcion) {
            throw new IllegalStateException(excepcion);
        }
    }
}
