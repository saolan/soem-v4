package com.saolan.soem.seguridad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.saolan.soem.compartido.excepcion.ExcepcionValidacion;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.configuracion.repositorio.SucursalRepositorio;
import com.saolan.soem.seguridad.dto.AsignacionAccesoDTO;
import com.saolan.soem.seguridad.dto.RolDTO;
import com.saolan.soem.seguridad.dto.RolFiltroDTO;
import com.saolan.soem.seguridad.mapeador.RolMapeador;
import com.saolan.soem.seguridad.mapeador.RolMapeadorImpl;
import com.saolan.soem.seguridad.modelo.Permiso;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.repositorio.PermisoRepositorio;
import com.saolan.soem.seguridad.repositorio.RolRepositorio;

/**
 * Tests unitarios de {@link RolServicio}: mockea los repositorios (incluido
 * {@link SucursalRepositorio} del módulo {@code configuracion}, del que
 * depende {@code reemplazarSucursales}), usa {@link RolMapeador} real como
 * {@code @Spy}.
 */
@ExtendWith(MockitoExtension.class)
class RolServicioTest {

    @Mock
    private RolRepositorio rolRepositorio;

    @Mock
    private PermisoRepositorio permisoRepositorio;

    @Mock
    private SucursalRepositorio sucursalRepositorio;

    @Spy
    private RolMapeador rolMapeador = new RolMapeadorImpl();

    @InjectMocks
    private RolServicio rolServicio;

    @Test
    void buscar_deberiaDelegarEnRepositorioYMapear() {
        RolFiltroDTO filtro = new RolFiltroDTO(null, null);
        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(1, 20, "descri", "asc");
        when(rolRepositorio.buscarPaginado(filtro, parametrosBusqueda)).thenReturn(List.of(nuevoRol(1, "Administrador", true)));

        List<RolDTO> resultado = rolServicio.buscar(filtro, parametrosBusqueda);

        assertEquals(1, resultado.size());
        assertEquals("Administrador", resultado.get(0).descri());
    }

    @Test
    void contar_deberiaDelegarEnRepositorio() {
        RolFiltroDTO filtro = new RolFiltroDTO(null, null);
        when(rolRepositorio.contar(filtro)).thenReturn(4L);

        assertEquals(4L, rolServicio.contar(filtro));
    }

    @Test
    void buscarPorId_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(rolRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> rolServicio.buscarPorId(99));
    }

    @Test
    void combo_deberiaDelegarEnRepositorio() {
        when(rolRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "Administrador")));

        assertEquals(1, rolServicio.combo().size());
    }

    @Test
    void crear_deberiaCrearRol() {
        RolDTO dto = new RolDTO(null, "Nuevo Rol", true);

        RolDTO resultado = rolServicio.crear(dto);

        assertEquals("Nuevo Rol", resultado.descri());
    }

    @Test
    void modificar_deberiaActualizarDatos() {
        Rol existente = nuevoRol(1, "Rol Viejo", true);
        when(rolRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));

        RolDTO resultado = rolServicio.modificar(1, new RolDTO(1, "Rol Nuevo", false));

        assertEquals("Rol Nuevo", existente.getDescri());
        assertFalse(resultado.estado());
    }

    @Test
    void modificar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(rolRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> rolServicio.modificar(99, new RolDTO(99, "X", true)));
    }

    @Test
    void eliminar_deberiaMarcarEstadoFalse() {
        Rol existente = nuevoRol(1, "Rol Test", true);
        when(rolRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));

        rolServicio.eliminar(1);

        assertFalse(existente.getEstado());
    }

    @Test
    void eliminar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(rolRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> rolServicio.eliminar(99));
    }

    @Test
    void reemplazarPermisos_deberiaReemplazarEnBloque() {
        Rol rol = nuevoRol(1, "Rol Test", true);
        when(rolRepositorio.buscarPorId(1)).thenReturn(Optional.of(rol));
        when(permisoRepositorio.buscarPorId(10)).thenReturn(Optional.of(nuevoPermiso(10, "sucursales", Accion.BUSCAR)));
        when(permisoRepositorio.buscarPorId(11)).thenReturn(Optional.of(nuevoPermiso(11, "sucursales", Accion.CREAR)));

        rolServicio.reemplazarPermisos(1, List.of(new AsignacionAccesoDTO(10, true), new AsignacionAccesoDTO(11, false)));

        assertEquals(2, rol.getPermisos().size());
        assertFalse(rol.getPermisos().get(1).getAcceso());
    }

    @Test
    void reemplazarPermisos_deberiaLanzarValidacion_cuandoElPermisoNoExiste() {
        Rol rol = nuevoRol(1, "Rol Test", true);
        when(rolRepositorio.buscarPorId(1)).thenReturn(Optional.of(rol));
        when(permisoRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> rolServicio.reemplazarPermisos(1, List.of(new AsignacionAccesoDTO(99, true))));
        assertEquals("permisos", excepcion.getErrores().get(0).campo());
    }

    @Test
    void reemplazarSucursales_deberiaReemplazarEnBloque() {
        Rol rol = nuevoRol(1, "Rol Test", true);
        when(rolRepositorio.buscarPorId(1)).thenReturn(Optional.of(rol));
        when(sucursalRepositorio.buscarPorId(5)).thenReturn(Optional.of(nuevaSucursal(5)));

        rolServicio.reemplazarSucursales(1, List.of(new AsignacionAccesoDTO(5, true)));

        assertEquals(1, rol.getSucursales().size());
    }

    @Test
    void reemplazarSucursales_deberiaLanzarValidacion_cuandoLaSucursalNoExiste() {
        Rol rol = nuevoRol(1, "Rol Test", true);
        when(rolRepositorio.buscarPorId(1)).thenReturn(Optional.of(rol));
        when(sucursalRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        ExcepcionValidacion excepcion = assertThrows(ExcepcionValidacion.class,
                () -> rolServicio.reemplazarSucursales(1, List.of(new AsignacionAccesoDTO(99, true))));
        assertEquals("sucursales", excepcion.getErrores().get(0).campo());
    }

    // ---------- helpers ----------

    private Rol nuevoRol(Integer rolId, String descri, boolean estado) {
        Rol rol = new Rol();
        rol.setRolId(rolId);
        rol.setDescri(descri);
        rol.setEstado(estado);
        return rol;
    }

    private Permiso nuevoPermiso(Integer permisoId, String recurso, Accion accion) {
        Permiso permiso = new Permiso();
        permiso.setPermisoId(permisoId);
        permiso.setDescri("Permiso Test");
        permiso.setRecurso(recurso);
        permiso.setAccion(accion);
        return permiso;
    }

    private Sucursal nuevaSucursal(Integer sucursalId) {
        Sucursal sucursal = new Sucursal();
        sucursal.setSucursalId(sucursalId);
        sucursal.setDescri("Sucursal Test");
        sucursal.setEstado(Boolean.TRUE);
        return sucursal;
    }
}
