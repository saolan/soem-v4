package com.saolan.soem.seguridad.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.seguridad.dto.PermisoFiltroDTO;
import com.saolan.soem.seguridad.modelo.Permiso;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link PermisoRepositorio}. A diferencia de
 * {@code sucursal}/{@code rol}, la tabla {@code permiso} ya trae filas
 * sembradas por la migración {@code V2__permiso_recurso_accion.sql}: los
 * recursos de prueba usan un prefijo aleatorio para no colisionar con esos
 * datos, y las aserciones sobre listados usan {@code anyMatch}/{@code noneMatch}
 * en vez de tamaños absolutos.
 */
@ExtendWith(ArquillianExtension.class)
class PermisoRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(PermisoRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private PermisoRepositorio permisoRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            for (Integer id : idsCreados) {
                Permiso permiso = entityManager.find(Permiso.class, id);
                if (permiso != null) {
                    entityManager.remove(permiso);
                }
            }
        });
    }

    @Test
    void crear_yBuscarPorId_deberianFuncionarComoRoundTrip() {
        Permiso permiso = crear(nuevoPermiso(recursoAleatorio(), Accion.BUSCAR));

        assertTrue(permisoRepositorio.buscarPorId(permiso.getPermisoId()).isPresent());
    }

    @Test
    void buscarPorRecursoYAccion_deberiaEncontrarYExcluirIdIndicado() {
        String recurso = recursoAleatorio();
        Permiso permiso = crear(nuevoPermiso(recurso, Accion.BUSCAR));

        assertTrue(permisoRepositorio.buscarPorRecursoYAccion(recurso, Accion.BUSCAR, null).isPresent());
        assertTrue(permisoRepositorio.buscarPorRecursoYAccion(recurso, Accion.BUSCAR, permiso.getPermisoId()).isEmpty());
    }

    @Test
    void buscarPaginado_deberiaFiltrarPorRecurso() {
        String recurso = recursoAleatorio();
        crear(nuevoPermiso(recurso, Accion.BUSCAR));

        List<Permiso> resultado = permisoRepositorio.buscarPaginado(new PermisoFiltroDTO(null, recurso, null),
                new ParametrosBusqueda(1, 20, "recurso", "asc"));

        assertEquals(1, resultado.size());
    }

    @Test
    void contar_deberiaContarSoloLoQueCoincideConElFiltro() {
        String recurso = recursoAleatorio();
        crear(nuevoPermiso(recurso, Accion.BUSCAR));
        crear(nuevoPermiso(recurso, Accion.CREAR));

        assertEquals(2L, permisoRepositorio.contar(new PermisoFiltroDTO(null, recurso, null)));
    }

    @Test
    void buscarCombo_deberiaIncluirElPermisoCreado() {
        Permiso permiso = crear(nuevoPermiso(recursoAleatorio(), Accion.BUSCAR));

        assertTrue(permisoRepositorio.buscarCombo().stream().anyMatch(c -> c.valor().equals(permiso.getPermisoId())));
    }

    private Permiso crear(Permiso permiso) {
        ejecutarEnTransaccion(() -> permisoRepositorio.crear(permiso));
        idsCreados.add(permiso.getPermisoId());
        return permiso;
    }

    private Permiso nuevoPermiso(String recurso, Accion accion) {
        Permiso permiso = new Permiso();
        permiso.setDescri("Permiso de prueba");
        permiso.setRecurso(recurso);
        permiso.setAccion(accion);
        return permiso;
    }

    private String recursoAleatorio() {
        return "recurso-test-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void ejecutarEnTransaccion(Runnable accion) {
        try {
            userTransaction.begin();
            accion.run();
            userTransaction.commit();
        } catch (Exception excepcion) {
            try {
                userTransaction.rollback();
            } catch (Exception excepcionRollback) {
                excepcion.addSuppressed(excepcionRollback);
            }
            throw new IllegalStateException("Error ejecutando operación transaccional en el test", excepcion);
        }
    }
}
