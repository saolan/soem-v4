package com.saolan.soem.seguridad.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.OffsetDateTime;
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

import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;
import com.saolan.soem.seguridad.modelo.RefreshToken;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link RefreshTokenRepositorio}. Misma
 * precondición que {@code SucursalRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class RefreshTokenRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(RefreshTokenRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private RefreshTokenRepositorio refreshTokenRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsRefreshToken = new ArrayList<>();
    private final List<Integer> idsPersona = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            for (Integer id : idsRefreshToken) {
                RefreshToken token = entityManager.find(RefreshToken.class, id);
                if (token != null) {
                    entityManager.remove(token);
                }
            }
            for (Integer id : idsPersona) {
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
    void crear_yBuscarPorHash_deberianFuncionarComoRoundTrip() {
        PersUsua usuario = crearUsuario();
        String hash = hashAleatorio();
        crear(nuevoToken(usuario, hash, false, OffsetDateTime.now().plusDays(7)));

        Optional<RefreshToken> encontrado = refreshTokenRepositorio.buscarPorHash(hash);

        assertTrue(encontrado.isPresent());
        assertEquals(usuario.getPersonaId(), encontrado.get().getUsuario().getPersonaId());
    }

    @Test
    void buscarVigentesDePersona_deberiaExcluirRevocados() {
        PersUsua usuario = crearUsuario();
        crear(nuevoToken(usuario, hashAleatorio(), true, OffsetDateTime.now().plusDays(7)));

        assertTrue(refreshTokenRepositorio.buscarVigentesDePersona(usuario.getPersonaId()).isEmpty());
    }

    @Test
    void buscarVigentesDePersona_deberiaExcluirExpirados() {
        PersUsua usuario = crearUsuario();
        crear(nuevoToken(usuario, hashAleatorio(), false, OffsetDateTime.now().minusDays(1)));

        assertTrue(refreshTokenRepositorio.buscarVigentesDePersona(usuario.getPersonaId()).isEmpty());
    }

    @Test
    void buscarVigentesDePersona_deberiaIncluirSoloLosVigentes() {
        PersUsua usuario = crearUsuario();
        RefreshToken vigente = crear(nuevoToken(usuario, hashAleatorio(), false, OffsetDateTime.now().plusDays(7)));
        crear(nuevoToken(usuario, hashAleatorio(), true, OffsetDateTime.now().plusDays(7)));
        crear(nuevoToken(usuario, hashAleatorio(), false, OffsetDateTime.now().minusDays(1)));

        List<RefreshToken> resultado = refreshTokenRepositorio.buscarVigentesDePersona(usuario.getPersonaId());

        assertEquals(1, resultado.size());
        assertEquals(vigente.getTokenHash(), resultado.get(0).getTokenHash());
        assertFalse(resultado.get(0).getRevocado());
    }

    private RefreshToken crear(RefreshToken token) {
        ejecutarEnTransaccion(() -> refreshTokenRepositorio.crear(token));
        idsRefreshToken.add(token.getRefreshTokenId());
        return token;
    }

    private PersUsua crearUsuario() {
        Persona persona = new Persona();
        persona.setCedulaRuc(cedulaAleatoria());
        persona.setApelli("Apellido de Prueba");
        persona.setNombre("Nombre de Prueba");
        persona.setEstado(Boolean.TRUE);

        PersUsua usuario = new PersUsua();
        usuario.setPersona(persona);
        usuario.setUsuari("usuario.test." + UUID.randomUUID().toString().substring(0, 8));
        usuario.setClave("hash-de-prueba");
        usuario.setCaducaClav(Boolean.FALSE);
        usuario.setEstado(Boolean.TRUE);

        ejecutarEnTransaccion(() -> {
            entityManager.persist(persona);
            entityManager.persist(usuario);
        });
        idsPersona.add(persona.getPersonaId());
        return usuario;
    }

    private RefreshToken nuevoToken(PersUsua usuario, String hash, boolean revocado, OffsetDateTime fechaHoraExpira) {
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(hash);
        token.setFechaHoraEmision(OffsetDateTime.now().minusMinutes(1));
        token.setFechaHoraExpira(fechaHoraExpira);
        token.setRevocado(revocado);
        return token;
    }

    private String hashAleatorio() {
        return "hash-test-" + UUID.randomUUID();
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
