package com.saolan.soem.seguridad.repositorio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Permiso;
import com.saolan.soem.seguridad.modelo.Persona;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolPerm;
import com.saolan.soem.seguridad.modelo.RolPersUsua;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de {@link RolPermRepositorio#resolverPermisosDePersona(Integer)}:
 * arma el grafo real Persona→PersUsua→RolPersUsua→Rol→RolPerm→Permiso
 * directamente vía {@link EntityManager} (no hay repositorio propio para las
 * tablas puente) y verifica el formato {@code recurso:accion} que exige
 * {@code FiltroAutorizacion}. Misma precondición que {@code SucursalRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class RolPermRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(RolPermRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private RolPermRepositorio rolPermRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsRolPerm = new ArrayList<>();
    private final List<Integer> idsRolPersUsua = new ArrayList<>();
    private final List<Integer> idsRol = new ArrayList<>();
    private final List<Integer> idsPermiso = new ArrayList<>();
    private final List<Integer> idsPersona = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            eliminarPorId(RolPerm.class, idsRolPerm);
            eliminarPorId(RolPersUsua.class, idsRolPersUsua);
            eliminarPorId(Permiso.class, idsPermiso);
            eliminarPorId(Rol.class, idsRol);
            eliminarPorId(PersUsua.class, idsPersona);
            eliminarPorId(Persona.class, idsPersona);
        });
    }

    @Test
    void resolverPermisosDePersona_deberiaIncluirElPermisoHabilitado() {
        String recurso = recursoAleatorio();
        Integer personaId = crearGrafo(recurso, Accion.BUSCAR, true, true, true);

        Set<String> resultado = rolPermRepositorio.resolverPermisosDePersona(personaId);

        assertTrue(resultado.contains(recurso + ":buscar"));
    }

    @Test
    void resolverPermisosDePersona_noDeberiaIncluirPermisoConAccesoFalso() {
        String recurso = recursoAleatorio();
        Integer personaId = crearGrafo(recurso, Accion.BUSCAR, true, true, false);

        assertFalse(rolPermRepositorio.resolverPermisosDePersona(personaId).contains(recurso + ":buscar"));
    }

    @Test
    void resolverPermisosDePersona_noDeberiaIncluirPermisoDeRolInactivo() {
        String recurso = recursoAleatorio();
        Integer personaId = crearGrafo(recurso, Accion.BUSCAR, true, false, true);

        assertFalse(rolPermRepositorio.resolverPermisosDePersona(personaId).contains(recurso + ":buscar"));
    }

    @Test
    void resolverPermisosDePersona_deberiaSerVacio_cuandoLaPersonaNoTieneRoles() {
        Integer personaId = crearUsuarioSinRoles();

        assertTrue(rolPermRepositorio.resolverPermisosDePersona(personaId).isEmpty());
    }

    private Integer crearGrafo(String recurso, Accion accion, boolean usuarioActivo, boolean rolActivo, boolean permisoAcceso) {
        Persona persona = nuevaPersona();
        PersUsua usuario = nuevoUsuario(persona, usuarioActivo);
        Rol rol = nuevoRol(rolActivo);
        Permiso permiso = nuevoPermiso(recurso, accion);

        RolPersUsua asignacionRol = new RolPersUsua();
        asignacionRol.setRol(rol);
        asignacionRol.setUsuario(usuario);

        RolPerm asignacionPermiso = new RolPerm();
        asignacionPermiso.setRol(rol);
        asignacionPermiso.setPermiso(permiso);
        asignacionPermiso.setAcceso(permisoAcceso);

        ejecutarEnTransaccion(() -> {
            entityManager.persist(persona);
            entityManager.persist(usuario);
            entityManager.persist(rol);
            entityManager.persist(permiso);
            entityManager.persist(asignacionRol);
            entityManager.persist(asignacionPermiso);
        });

        idsPersona.add(persona.getPersonaId());
        idsRol.add(rol.getRolId());
        idsPermiso.add(permiso.getPermisoId());
        idsRolPersUsua.add(asignacionRol.getRolPersUsuaId());
        idsRolPerm.add(asignacionPermiso.getRolPermId());

        return persona.getPersonaId();
    }

    private Integer crearUsuarioSinRoles() {
        Persona persona = nuevaPersona();
        PersUsua usuario = nuevoUsuario(persona, true);

        ejecutarEnTransaccion(() -> {
            entityManager.persist(persona);
            entityManager.persist(usuario);
        });
        idsPersona.add(persona.getPersonaId());
        return persona.getPersonaId();
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

    private String cedulaAleatoria() {
        long numero = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000_000L;
        return String.format(Locale.ROOT, "%013d", numero);
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
}
