package com.saolan.soem.seguridad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.seguridad.dto.UsuarioCreacionDTO;
import com.saolan.soem.seguridad.dto.UsuarioDTO;
import com.saolan.soem.seguridad.dto.UsuarioFiltroDTO;
import com.saolan.soem.seguridad.mapeador.UsuarioMapeador;
import com.saolan.soem.seguridad.mapeador.UsuarioMapeadorImpl;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.repositorio.PersonaRepositorio;
import com.saolan.soem.seguridad.repositorio.RolRepositorio;
import com.saolan.soem.seguridad.repositorio.UsuarioRepositorio;

/**
 * Tests unitarios de {@link UsuarioServicio}: mockea los repositorios y
 * {@link ClaveServicio}, usa {@link UsuarioMapeador} real como {@code @Spy}.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PersonaRepositorio personaRepositorio;

    @Mock
    private RolRepositorio rolRepositorio;

    @Mock
    private ClaveServicio claveServicio;

    @Spy
    private UsuarioMapeador usuarioMapeador = new UsuarioMapeadorImpl();

    @InjectMocks
    private UsuarioServicio usuarioServicio;

    // ---------- crear ----------

    @Test
    void crear_deberiaCrearPersonaYUsuario_cuandoDatosValidos() {
        UsuarioCreacionDTO dto = new UsuarioCreacionDTO("0999999999", "Apellido", "Nombre", null, null, null, null,
                "usuario.nuevo", "clave-plana", false, true);
        when(usuarioRepositorio.buscarPorUsuari("usuario.nuevo")).thenReturn(Optional.empty());
        when(claveServicio.hashear("clave-plana")).thenReturn("hash-resultante");

        UsuarioDTO resultado = usuarioServicio.crear(dto);

        assertEquals("usuario.nuevo", resultado.usuari());
        assertEquals("0999999999", resultado.cedulaRuc());
        verify(personaRepositorio).crear(any(Persona.class));

        ArgumentCaptor<PersUsua> captor = ArgumentCaptor.forClass(PersUsua.class);
        verify(usuarioRepositorio).crear(captor.capture());
        assertEquals("hash-resultante", captor.getValue().getClave());
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoUsuariYaExiste() {
        UsuarioCreacionDTO dto = new UsuarioCreacionDTO("0999999999", "Apellido", "Nombre", null, null, null, null,
                "usuario.repetido", "clave-plana", false, true);
        when(usuarioRepositorio.buscarPorUsuari("usuario.repetido")).thenReturn(Optional.of(nuevoUsuario(5, "usuario.repetido")));

        assertThrows(ExcepcionConflicto.class, () -> usuarioServicio.crear(dto));
        verify(personaRepositorio, never()).crear(any());
    }

    // ---------- modificar ----------

    @Test
    void modificar_deberiaActualizarDatos() {
        PersUsua existente = nuevoUsuario(1, "usuario.viejo");
        UsuarioDTO dto = new UsuarioDTO(1, "0999999999", "Apellido Nuevo", "Nombre Nuevo", null, null, null, null,
                "usuario.nuevo", false, null, true);
        when(usuarioRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(usuarioRepositorio.buscarPorUsuari("usuario.nuevo")).thenReturn(Optional.empty());

        UsuarioDTO resultado = usuarioServicio.modificar(1, dto);

        assertEquals("usuario.nuevo", existente.getUsuari());
        assertEquals("usuario.nuevo", resultado.usuari());
    }

    @Test
    void modificar_deberiaLanzarNoEncontrado_cuandoElUsuarioNoExiste() {
        UsuarioDTO dto = new UsuarioDTO(99, "0999999999", "Apellido", "Nombre", null, null, null, null,
                "usuario.x", false, null, true);
        when(usuarioRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> usuarioServicio.modificar(99, dto));
        verify(usuarioRepositorio, never()).buscarPorUsuari(any());
    }

    @Test
    void modificar_deberiaLanzarConflicto_cuandoElUsuariPerteneceAOtroUsuario() {
        PersUsua existente = nuevoUsuario(1, "usuario.viejo");
        UsuarioDTO dto = new UsuarioDTO(1, "0999999999", "Apellido", "Nombre", null, null, null, null,
                "usuario.ocupado", false, null, true);
        when(usuarioRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(usuarioRepositorio.buscarPorUsuari("usuario.ocupado")).thenReturn(Optional.of(nuevoUsuario(2, "usuario.ocupado")));

        assertThrows(ExcepcionConflicto.class, () -> usuarioServicio.modificar(1, dto));
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_deberiaMarcarEstadoFalse() {
        PersUsua existente = nuevoUsuario(1, "usuario.test");
        when(usuarioRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));

        usuarioServicio.eliminar(1);

        assertFalse(existente.getEstado());
    }

    @Test
    void eliminar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(usuarioRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> usuarioServicio.eliminar(99));
    }

    // ---------- consultas ----------

    @Test
    void buscarPorId_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(usuarioRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> usuarioServicio.buscarPorId(99));
    }

    @Test
    void buscar_deberiaDelegarEnRepositorioYMapear() {
        UsuarioFiltroDTO filtro = new UsuarioFiltroDTO(null, null, null, null);
        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(1, 20, "apelli", "asc");
        when(usuarioRepositorio.buscarPaginado(filtro, parametrosBusqueda)).thenReturn(List.of(nuevoUsuario(1, "usuario.test")));

        List<UsuarioDTO> resultado = usuarioServicio.buscar(filtro, parametrosBusqueda);

        assertEquals(1, resultado.size());
        assertEquals("usuario.test", resultado.get(0).usuari());
    }

    @Test
    void contar_deberiaDelegarEnRepositorio() {
        UsuarioFiltroDTO filtro = new UsuarioFiltroDTO(null, null, null, null);
        when(usuarioRepositorio.contar(filtro)).thenReturn(3L);

        assertEquals(3L, usuarioServicio.contar(filtro));
    }

    @Test
    void combo_deberiaDelegarEnRepositorio() {
        when(usuarioRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "usuario.test")));

        List<ComboDTO> resultado = usuarioServicio.combo();

        assertEquals(1, resultado.size());
    }

    // ---------- reemplazarRoles ----------

    @Test
    void reemplazarRoles_deberiaReemplazarEnBloque() {
        PersUsua existente = nuevoUsuario(1, "usuario.test");
        when(usuarioRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(rolRepositorio.buscarPorId(5)).thenReturn(Optional.of(nuevoRol(5)));
        when(rolRepositorio.buscarPorId(6)).thenReturn(Optional.of(nuevoRol(6)));

        usuarioServicio.reemplazarRoles(1, List.of(5, 6));

        assertEquals(2, existente.getRoles().size());
    }

    @Test
    void reemplazarRoles_deberiaLanzarValidacion_cuandoUnRolNoExiste() {
        PersUsua existente = nuevoUsuario(1, "usuario.test");
        when(usuarioRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(rolRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> usuarioServicio.reemplazarRoles(1, List.of(99)));
        assertEquals("rolIds", excepcion.getErrores().get(0).campo());
    }

    // ---------- helpers ----------

    private PersUsua nuevoUsuario(Integer personaId, String usuari) {
        Persona persona = new Persona();
        persona.setPersonaId(personaId);
        persona.setCedulaRuc("0999999999");
        persona.setApelli("Apellido Test");
        persona.setNombre("Nombre Test");
        persona.setEstado(Boolean.TRUE);

        PersUsua usuario = new PersUsua();
        usuario.setPersona(persona);
        usuario.setUsuari(usuari);
        usuario.setClave("hash-existente");
        usuario.setCaducaClav(Boolean.FALSE);
        usuario.setEstado(Boolean.TRUE);
        asignarPersonaId(usuario, personaId);
        return usuario;
    }

    private Rol nuevoRol(Integer rolId) {
        Rol rol = new Rol();
        rol.setRolId(rolId);
        rol.setDescri("Rol Test " + rolId);
        rol.setEstado(Boolean.TRUE);
        return rol;
    }

    /**
     * {@code PersUsua.personaId} no tiene setter propio (se sincroniza vía
     * {@code @MapsId} solo en un runtime JPA real); en un test unitario puro
     * hay que fijarlo por reflexión para que {@code UsuarioServicio} (que
     * navega por {@code getPersonaId()}) tenga un valor coherente.
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
