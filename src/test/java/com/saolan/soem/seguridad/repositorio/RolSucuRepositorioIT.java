package com.saolan.soem.seguridad.repositorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolPersUsua;
import com.saolan.soem.seguridad.modelo.RolSucu;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link RolSucuRepositorio#resolverSucursalesAccesibles(Integer)}:
 * arma el grafo real Persona→PersUsua→RolPersUsua→Rol→RolSucu→Sucursal
 * directamente vía {@link EntityManager} y verifica el orden "matriz
 * primero". Misma precondición que {@code SucursalRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class RolSucuRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(RolSucuRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private RolSucuRepositorio rolSucuRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsRolSucu = new ArrayList<>();
    private final List<Integer> idsRolPersUsua = new ArrayList<>();
    private final List<Integer> idsSucursal = new ArrayList<>();
    private final List<Integer> idsRol = new ArrayList<>();
    private final List<Integer> idsPersona = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            eliminarPorId(RolSucu.class, idsRolSucu);
            eliminarPorId(RolPersUsua.class, idsRolPersUsua);
            eliminarPorId(Sucursal.class, idsSucursal);
            eliminarPorId(Rol.class, idsRol);
            eliminarPorId(PersUsua.class, idsPersona);
            eliminarPorId(Persona.class, idsPersona);
        });
    }

    @Test
    void resolverSucursalesAccesibles_deberiaIncluirLaSucursalHabilitada() {
        Contexto contexto = crearUsuarioConRol(true, true);
        Sucursal sucursal = agregarSucursalAlRol(contexto.rol(), false, true, true);

        List<Sucursal> resultado = rolSucuRepositorio.resolverSucursalesAccesibles(contexto.personaId());

        assertTrue(resultado.stream().anyMatch(s -> s.getSucursalId().equals(sucursal.getSucursalId())));
    }

    @Test
    void resolverSucursalesAccesibles_noDeberiaIncluirConAccesoFalso() {
        Contexto contexto = crearUsuarioConRol(true, true);
        Sucursal sucursal = agregarSucursalAlRol(contexto.rol(), false, false, true);

        List<Sucursal> resultado = rolSucuRepositorio.resolverSucursalesAccesibles(contexto.personaId());

        assertFalse(resultado.stream().anyMatch(s -> s.getSucursalId().equals(sucursal.getSucursalId())));
    }

    @Test
    void resolverSucursalesAccesibles_noDeberiaIncluirDeRolInactivo() {
        Contexto contexto = crearUsuarioConRol(true, false);
        Sucursal sucursal = agregarSucursalAlRol(contexto.rol(), false, true, true);

        List<Sucursal> resultado = rolSucuRepositorio.resolverSucursalesAccesibles(contexto.personaId());

        assertFalse(resultado.stream().anyMatch(s -> s.getSucursalId().equals(sucursal.getSucursalId())));
    }

    @Test
    void resolverSucursalesAccesibles_deberiaOrdenarConMatrizPrimero() {
        Contexto contexto = crearUsuarioConRol(true, true);
        agregarSucursalAlRol(contexto.rol(), false, true, true);
        Sucursal matriz = agregarSucursalAlRol(contexto.rol(), true, true, true);

        List<Sucursal> resultado = rolSucuRepositorio.resolverSucursalesAccesibles(contexto.personaId());

        assertEquals(matriz.getSucursalId(), resultado.get(0).getSucursalId());
    }

    @Test
    void resolverSucursalesAccesibles_deberiaSerVacio_cuandoLaPersonaNoTieneRoles() {
        Persona persona = nuevaPersona();
        PersUsua usuario = nuevoUsuario(persona, true);
        ejecutarEnTransaccion(() -> {
            entityManager.persist(persona);
            entityManager.persist(usuario);
        });
        idsPersona.add(persona.getPersonaId());

        assertTrue(rolSucuRepositorio.resolverSucursalesAccesibles(persona.getPersonaId()).isEmpty());
    }

    private Contexto crearUsuarioConRol(boolean usuarioActivo, boolean rolActivo) {
        Persona persona = nuevaPersona();
        PersUsua usuario = nuevoUsuario(persona, usuarioActivo);
        Rol rol = nuevoRol(rolActivo);

        RolPersUsua asignacionRol = new RolPersUsua();
        asignacionRol.setRol(rol);
        asignacionRol.setUsuario(usuario);

        ejecutarEnTransaccion(() -> {
            entityManager.persist(persona);
            entityManager.persist(usuario);
            entityManager.persist(rol);
            entityManager.persist(asignacionRol);
        });
        idsPersona.add(persona.getPersonaId());
        idsRol.add(rol.getRolId());
        idsRolPersUsua.add(asignacionRol.getRolPersUsuaId());

        return new Contexto(persona.getPersonaId(), rol);
    }

    private Sucursal agregarSucursalAlRol(Rol rol, boolean matriz, boolean acceso, boolean sucursalActiva) {
        Sucursal sucursal = nuevaSucursal(matriz, sucursalActiva);
        RolSucu rolSucu = new RolSucu();
        rolSucu.setRol(rol);
        rolSucu.setSucursal(sucursal);
        rolSucu.setAcceso(acceso);

        ejecutarEnTransaccion(() -> {
            entityManager.persist(sucursal);
            entityManager.persist(rolSucu);
        });
        idsSucursal.add(sucursal.getSucursalId());
        idsRolSucu.add(rolSucu.getRolSucuId());
        return sucursal;
    }

    private Persona nuevaPersona() {
        Persona persona = new Persona();
        persona.setCedulaRuc(cedulaAleatoria());
        persona.setApelli("Apellido de Prueba");
        persona.setNombre("Nombre de Prueba");
        persona.setEstado(Boolean.TRUE);
        return persona;
    }

    private PersUsua nuevoUsuario(Persona persona, boolean estado) {
        PersUsua usuario = new PersUsua();
        usuario.setPersona(persona);
        usuario.setUsuari("usuario.test." + UUID.randomUUID().toString().substring(0, 8));
        usuario.setClave("hash-de-prueba");
        usuario.setCaducaClav(Boolean.FALSE);
        usuario.setEstado(estado);
        return usuario;
    }

    private Rol nuevoRol(boolean estado) {
        Rol rol = new Rol();
        rol.setDescri("Rol de prueba " + UUID.randomUUID().toString().substring(0, 8));
        rol.setEstado(estado);
        return rol;
    }

    private Sucursal nuevaSucursal(boolean matriz, boolean estado) {
        String codigo = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigo(codigo);
        sucursal.setRuc(rucAleatorio());
        sucursal.setMatriz(matriz);
        sucursal.setEstado(estado);
        sucursal.setDescri("Sucursal de prueba");
        sucursal.setRazonSoci("Razón Social de Prueba");
        sucursal.setCiudad("Quito");
        sucursal.setDirecc("Av. de Prueba 123");
        sucursal.setDireccEsta("Av. de Prueba 123");
        sucursal.setTelef1("0999999999");
        sucursal.setCorreo("prueba@test.com");
        sucursal.setRepres("Representante de Prueba");
        sucursal.setRepresCedu("0999999999");
        sucursal.setRepresDire("Dirección Representante de Prueba");
        sucursal.setRucCont(sucursal.getRuc());
        return sucursal;
    }

    private String rucAleatorio() {
        long numero = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000_000L;
        return String.format(Locale.ROOT, "%013d", numero);
    }

    private String cedulaAleatoria() {
        return rucAleatorio();
    }

    private <T> void eliminarPorId(Class<T> clase, List<Integer> ids) {
        for (Integer id : ids) {
            T entidad = entityManager.find(clase, id);
            if (entidad != null) {
                entityManager.remove(entidad);
            }
        }
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

    private record Contexto(Integer personaId, Rol rol) {
    }
}
