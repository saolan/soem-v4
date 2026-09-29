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

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.seguridad.dto.UsuarioFiltroDTO;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link UsuarioRepositorio}. La escritura de un
 * usuario requiere dos pasos, igual que {@code UsuarioServicio.crear}:
 * persistir primero la {@link Persona} (genera su id), y recién después
 * asociarla al {@link PersUsua} (usa ese id vía {@code @MapsId}). Misma
 * precondición y patrón transaccional que {@code SucursalRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class UsuarioRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(UsuarioRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private UsuarioRepositorio usuarioRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            for (Integer id : idsCreados) {
                PersUsua usuario = entityManager.find(PersUsua.class, id);
                if (usuario != null) {
                    entityManager.remove(usuario);
                }
                Persona persona = entityManager.find(Persona.class, id);
                if (persona != null) {
                    entityManager.remove(persona);
                }
            }
        });
    }

    @Test
    void crear_yBuscarPorId_deberianFuncionarComoRoundTrip() {
        PersUsua usuario = crear(nuevoUsuario(usuariAleatorio()));

        Optional<PersUsua> encontrado = usuarioRepositorio.buscarPorId(usuario.getPersonaId());

        assertTrue(encontrado.isPresent());
        assertEquals(usuario.getUsuari(), encontrado.get().getUsuari());
    }

    @Test
    void buscarPorUsuari_deberiaEncontrarElUsuario() {
        String usuari = usuariAleatorio();
        crear(nuevoUsuario(usuari));

        assertTrue(usuarioRepositorio.buscarPorUsuari(usuari).isPresent());
    }

    @Test
    void buscarPaginado_deberiaFiltrarPorApellidoDeLaPersonaAsociada() {
        String apellido = "Apellido" + UUID.randomUUID().toString().substring(0, 6);
        PersUsua usuario = nuevoUsuario(usuariAleatorio());
        usuario.getPersona().setApelli(apellido);
        crear(usuario);
        crear(nuevoUsuario(usuariAleatorio()));

        UsuarioFiltroDTO filtro = new UsuarioFiltroDTO(null, apellido, null, null);
        List<PersUsua> resultado = usuarioRepositorio.buscarPaginado(filtro, new ParametrosBusqueda(1, 20, "apelli", "asc"));

        assertEquals(1, resultado.size());
        assertEquals(usuario.getPersonaId(), resultado.get(0).getPersonaId());
    }

    @Test
    void contar_deberiaContarSoloLoQueCoincideConElFiltro() {
        String usuari = usuariAleatorio();
        crear(nuevoUsuario(usuari));

        long total = usuarioRepositorio.contar(new UsuarioFiltroDTO(null, null, usuari, null));

        assertEquals(1L, total);
    }

    @Test
    void buscarCombo_deberiaIncluirSoloUsuariosActivos() {
        PersUsua activo = crear(nuevoUsuario(usuariAleatorio()));
        PersUsua inactivo = nuevoUsuario(usuariAleatorio());
        inactivo.setEstado(Boolean.FALSE);
        crear(inactivo);

        List<ComboDTO> combo = usuarioRepositorio.buscarCombo();

        assertTrue(combo.stream().anyMatch(c -> c.valor().equals(activo.getPersonaId())));
        assertTrue(combo.stream().noneMatch(c -> c.valor().equals(inactivo.getPersonaId())));
    }

    private PersUsua crear(PersUsua usuario) {
        ejecutarEnTransaccion(() -> {
            entityManager.persist(usuario.getPersona());
            usuarioRepositorio.crear(usuario);
        });
        idsCreados.add(usuario.getPersonaId());
        return usuario;
    }

    private PersUsua nuevoUsuario(String usuari) {
        Persona persona = new Persona();
        persona.setCedulaRuc(cedulaAleatoria());
        persona.setApelli("Apellido de Prueba");
        persona.setNombre("Nombre de Prueba");
        persona.setEstado(Boolean.TRUE);

        PersUsua usuario = new PersUsua();
        usuario.setPersona(persona);
        usuario.setUsuari(usuari);
        usuario.setClave("hash-de-prueba");
        usuario.setCaducaClav(Boolean.FALSE);
        usuario.setEstado(Boolean.TRUE);
        return usuario;
    }

    private String usuariAleatorio() {
        return "usuario.test." + UUID.randomUUID().toString().substring(0, 8);
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
