package com.saolan.soem.seguridad.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.saolan.soem.compartido.excepcion.ExcepcionConflicto;
import com.saolan.soem.compartido.excepcion.ExcepcionNoEncontrado;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.seguridad.dto.PermisoDTO;
import com.saolan.soem.seguridad.dto.PermisoFiltroDTO;
import com.saolan.soem.seguridad.mapeador.PermisoMapeador;
import com.saolan.soem.seguridad.mapeador.PermisoMapeadorImpl;
import com.saolan.soem.seguridad.modelo.Permiso;
import com.saolan.soem.seguridad.repositorio.PermisoRepositorio;

/**
 * Tests unitarios de {@link PermisoServicio}: mockea {@link PermisoRepositorio},
 * usa {@link PermisoMapeador} real como {@code @Spy}.
 */
@ExtendWith(MockitoExtension.class)
class PermisoServicioTest {

    @Mock
    private PermisoRepositorio permisoRepositorio;

    @Spy
    private PermisoMapeador permisoMapeador = new PermisoMapeadorImpl();

    @InjectMocks
    private PermisoServicio permisoServicio;

    @Test
    void buscar_deberiaDelegarEnRepositorioYMapear() {
        PermisoFiltroDTO filtro = new PermisoFiltroDTO(null, null, null);
        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(1, 20, "recurso", "asc");
        when(permisoRepositorio.buscarPaginado(filtro, parametrosBusqueda))
                .thenReturn(List.of(nuevoPermiso(1, "sucursales", Accion.BUSCAR)));

        List<PermisoDTO> resultado = permisoServicio.buscar(filtro, parametrosBusqueda);

        assertEquals(1, resultado.size());
        assertEquals("sucursales", resultado.get(0).recurso());
    }

    @Test
    void contar_deberiaDelegarEnRepositorio() {
        PermisoFiltroDTO filtro = new PermisoFiltroDTO(null, null, null);
        when(permisoRepositorio.contar(filtro)).thenReturn(6L);

        assertEquals(6L, permisoServicio.contar(filtro));
    }

    @Test
    void buscarPorId_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(permisoRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class, () -> permisoServicio.buscarPorId(99));
    }

    @Test
    void combo_deberiaDelegarEnRepositorio() {
        when(permisoRepositorio.buscarCombo()).thenReturn(List.of(new ComboDTO(1, "Buscar sucursales")));

        assertEquals(1, permisoServicio.combo().size());
    }

    @Test
    void crear_deberiaCrearPermiso_cuandoNoHayDuplicado() {
        PermisoDTO dto = new PermisoDTO(null, "Buscar usuarios", "usuarios", Accion.BUSCAR, null);
        when(permisoRepositorio.buscarPorRecursoYAccion("usuarios", Accion.BUSCAR, null)).thenReturn(Optional.empty());

        PermisoDTO resultado = permisoServicio.crear(dto);

        assertEquals("usuarios", resultado.recurso());
        assertEquals(Accion.BUSCAR, resultado.accion());
    }

    @Test
    void crear_deberiaLanzarConflicto_cuandoElRecursoYAccionYaExisten() {
        PermisoDTO dto = new PermisoDTO(null, "Buscar usuarios", "usuarios", Accion.BUSCAR, null);
        when(permisoRepositorio.buscarPorRecursoYAccion("usuarios", Accion.BUSCAR, null))
                .thenReturn(Optional.of(nuevoPermiso(1, "usuarios", Accion.BUSCAR)));

        assertThrows(ExcepcionConflicto.class, () -> permisoServicio.crear(dto));
    }

    @Test
    void modificar_deberiaActualizarDatos() {
        Permiso existente = nuevoPermiso(1, "usuarios", Accion.BUSCAR);
        when(permisoRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(permisoRepositorio.buscarPorRecursoYAccion("usuarios", Accion.MODIFICAR, 1)).thenReturn(Optional.empty());

        PermisoDTO resultado = permisoServicio.modificar(1, new PermisoDTO(1, "Modificar usuarios", "usuarios", Accion.MODIFICAR, null));

        assertEquals(Accion.MODIFICAR, existente.getAccion());
        assertEquals(Accion.MODIFICAR, resultado.accion());
    }

    @Test
    void modificar_deberiaLanzarConflicto_cuandoElRecursoYAccionPertenecenAOtroPermiso() {
        Permiso existente = nuevoPermiso(1, "usuarios", Accion.BUSCAR);
        when(permisoRepositorio.buscarPorId(1)).thenReturn(Optional.of(existente));
        when(permisoRepositorio.buscarPorRecursoYAccion("usuarios", Accion.CREAR, 1))
                .thenReturn(Optional.of(nuevoPermiso(2, "usuarios", Accion.CREAR)));

        assertThrows(ExcepcionConflicto.class,
                () -> permisoServicio.modificar(1, new PermisoDTO(1, "x", "usuarios", Accion.CREAR, null)));
    }

    @Test
    void modificar_deberiaLanzarNoEncontrado_cuandoNoExiste() {
        when(permisoRepositorio.buscarPorId(99)).thenReturn(Optional.empty());

        assertThrows(ExcepcionNoEncontrado.class,
                () -> permisoServicio.modificar(99, new PermisoDTO(99, "x", "usuarios", Accion.BUSCAR, null)));
    }

    private Permiso nuevoPermiso(Integer permisoId, String recurso, Accion accion) {
        Permiso permiso = new Permiso();
        permiso.setPermisoId(permisoId);
        permiso.setDescri("Permiso Test");
        permiso.setRecurso(recurso);
        permiso.setAccion(accion);
        return permiso;
    }
}
