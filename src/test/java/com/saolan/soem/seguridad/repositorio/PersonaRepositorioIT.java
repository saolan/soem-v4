package com.saolan.soem.seguridad.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

import com.saolan.soem.seguridad.modelo.Persona;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link PersonaRepositorio}. Misma precondición y
 * patrón transaccional que {@code SucursalRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class PersonaRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(PersonaRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private PersonaRepositorio personaRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            for (Integer id : idsCreados) {
                Persona persona = entityManager.find(Persona.class, id);
                if (persona != null) {
                    entityManager.remove(persona);
                }
            }
        });
    }

    @Test
    void crear_yBuscarPorId_deberianFuncionarComoRoundTrip() {
        Persona persona = crear(nuevaPersona(cedulaAleatoria()));

        Optional<Persona> encontrada = personaRepositorio.buscarPorId(persona.getPersonaId());

        assertTrue(encontrada.isPresent());
        assertEquals(persona.getCedulaRuc(), encontrada.get().getCedulaRuc());
    }

    @Test
    void buscarPorCedula_deberiaEncontrarLaPersona() {
        String cedula = cedulaAleatoria();
        crear(nuevaPersona(cedula));

        Optional<Persona> encontrada = personaRepositorio.buscarPorCedula(cedula);

        assertTrue(encontrada.isPresent());
        assertEquals(cedula, encontrada.get().getCedulaRuc());
    }

    @Test
    void buscarPorCedula_deberiaSerVacio_cuandoNoExiste() {
        assertTrue(personaRepositorio.buscarPorCedula(cedulaAleatoria()).isEmpty());
    }

    private Persona crear(Persona persona) {
        ejecutarEnTransaccion(() -> personaRepositorio.crear(persona));
        idsCreados.add(persona.getPersonaId());
        return persona;
    }

    private Persona nuevaPersona(String cedulaRuc) {
        Persona persona = new Persona();
        persona.setCedulaRuc(cedulaRuc);
        persona.setApelli("Apellido de Prueba");
        persona.setNombre("Nombre de Prueba");
        persona.setEstado(Boolean.TRUE);
        return persona;
    }

    private String cedulaAleatoria() {
        long numero = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000_000L;
        return String.format(Locale.ROOT, "%013d", numero);
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
