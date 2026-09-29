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

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.seguridad.dto.RolFiltroDTO;
import com.saolan.soem.seguridad.modelo.Rol;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link RolRepositorio}. Misma precondición y
 * patrón transaccional que {@code SucursalRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class RolRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(RolRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private RolRepositorio rolRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            for (Integer id : idsCreados) {
                Rol rol = entityManager.find(Rol.class, id);
                if (rol != null) {
                    entityManager.remove(rol);
                }
            }
        });
    }

    @Test
    void crear_yBuscarPorId_deberianFuncionarComoRoundTrip() {
        Rol rol = crear(nuevoRol(descriAleatoria(), true));

        assertTrue(rolRepositorio.buscarPorId(rol.getRolId()).isPresent());
    }

    @Test
    void buscarPaginado_deberiaFiltrarPorDescripcion() {
        String descri = descriAleatoria();
        crear(nuevoRol(descri, true));
        crear(nuevoRol(descriAleatoria(), true));

        List<Rol> resultado = rolRepositorio.buscarPaginado(new RolFiltroDTO(descri, null),
                new ParametrosBusqueda(1, 20, "descri", "asc"));

        assertEquals(1, resultado.size());
        assertEquals(descri, resultado.get(0).getDescri());
    }

    @Test
    void contar_deberiaContarSoloLoQueCoincideConElFiltro() {
        String descri = descriAleatoria();
        crear(nuevoRol(descri, true));

        assertEquals(1L, rolRepositorio.contar(new RolFiltroDTO(descri, null)));
    }

    @Test
    void buscarCombo_deberiaIncluirSoloRolesActivos() {
        Rol activo = crear(nuevoRol(descriAleatoria(), true));
        Rol inactivo = crear(nuevoRol(descriAleatoria(), false));

        List<ComboDTO> combo = rolRepositorio.buscarCombo();

        assertTrue(combo.stream().anyMatch(c -> c.valor().equals(activo.getRolId())));
        assertTrue(combo.stream().noneMatch(c -> c.valor().equals(inactivo.getRolId())));
    }

    private Rol crear(Rol rol) {
        ejecutarEnTransaccion(() -> rolRepositorio.crear(rol));
        idsCreados.add(rol.getRolId());
        return rol;
    }

    private Rol nuevoRol(String descri, boolean estado) {
        Rol rol = new Rol();
        rol.setDescri(descri);
        rol.setEstado(estado);
        return rol;
    }

    private String descriAleatoria() {
        return "Rol de prueba " + UUID.randomUUID().toString().substring(0, 8);
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
