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
import com.saolan.soem.seguridad.dto.MenuPlanoDTO;
import com.saolan.soem.seguridad.modelo.Menu;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolMenu;
import com.saolan.soem.seguridad.modelo.RolPersUsua;
import com.saolan.soem.seguridad.modelo.RolSucu;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * Test de integración de
 * {@link MenuRepositorio#buscarAccesiblesDePersona(Integer, Integer)}: arma
 * el grafo real Persona→PersUsua→RolPersUsua→Rol→(RolMenu→Menu, RolSucu→Sucursal)
 * vía {@link EntityManager} y verifica el filtro por sucursal activa, por
 * acceso y por estado. Misma precondición que {@code RolSucuRepositorioIT}.
 */
@ExtendWith(ArquillianExtension.class)
class MenuRepositorioIT {

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(MenuRepositorioIT.class.getPackage());
        return war;
    }

    @Inject
    private MenuRepositorio menuRepositorio;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsRolMenu = new ArrayList<>();
    private final List<Integer> idsRolSucu = new ArrayList<>();
    private final List<Integer> idsRolPersUsua = new ArrayList<>();
    private final List<Integer> idsMenu = new ArrayList<>();
    private final List<Integer> idsSucursal = new ArrayList<>();
    private final List<Integer> idsRol = new ArrayList<>();
    private final List<Integer> idsPersona = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ejecutarEnTransaccion(() -> {
            eliminarPorId(RolMenu.class, idsRolMenu);
            eliminarPorId(RolSucu.class, idsRolSucu);
            eliminarPorId(RolPersUsua.class, idsRolPersUsua);
            eliminarPorId(Menu.class, idsMenu);
            eliminarPorId(Sucursal.class, idsSucursal);
            eliminarPorId(Rol.class, idsRol);
            eliminarPorId(PersUsua.class, idsPersona);
            eliminarPorId(Persona.class, idsPersona);
        });
    }

    @Test
    void buscarAccesiblesDePersona_deberiaIncluirElMenuDeLaSucursalActiva() {
        Contexto contexto = crearUsuarioConRolYSucursal();
        Menu menu = agregarMenuAlRol(contexto.rol(), true, true);

        List<MenuPlanoDTO> resultado = menuRepositorio.buscarAccesiblesDePersona(contexto.personaId(), contexto.sucursalId());

        assertTrue(contiene(resultado, menu));
    }

    @Test
    void buscarAccesiblesDePersona_noDeberiaIncluirMenus_enOtraSucursal() {
        Contexto contexto = crearUsuarioConRolYSucursal();
        Menu menu = agregarMenuAlRol(contexto.rol(), true, true);
        Sucursal otraSucursal = nuevaSucursal();
        ejecutarEnTransaccion(() -> entityManager.persist(otraSucursal));
        idsSucursal.add(otraSucursal.getSucursalId());

        List<MenuPlanoDTO> resultado = menuRepositorio.buscarAccesiblesDePersona(contexto.personaId(),
                otraSucursal.getSucursalId());

        assertFalse(contiene(resultado, menu));
    }

    @Test
    void buscarAccesiblesDePersona_noDeberiaIncluirConAccesoFalso() {
        Contexto contexto = crearUsuarioConRolYSucursal();
        Menu menu = agregarMenuAlRol(contexto.rol(), false, true);

        List<MenuPlanoDTO> resultado = menuRepositorio.buscarAccesiblesDePersona(contexto.personaId(), contexto.sucursalId());

        assertFalse(contiene(resultado, menu));
    }

    @Test
    void buscarAccesiblesDePersona_noDeberiaIncluirMenusInactivos() {
        Contexto contexto = crearUsuarioConRolYSucursal();
        Menu menu = agregarMenuAlRol(contexto.rol(), true, false);

        List<MenuPlanoDTO> resultado = menuRepositorio.buscarAccesiblesDePersona(contexto.personaId(), contexto.sucursalId());

        assertFalse(contiene(resultado, menu));
    }

    @Test
    void buscarTodosPlanos_deberiaProyectarElMenuSuperior() {
        Menu grupo = nuevoGrupo();
        Menu pagina = nuevoMenu(grupo, true);
        ejecutarEnTransaccion(() -> {
            entityManager.persist(grupo);
            entityManager.persist(pagina);
        });
        idsMenu.add(pagina.getMenuId());
        idsMenu.add(grupo.getMenuId());

        MenuPlanoDTO plano = menuRepositorio.buscarTodosPlanos().stream()
                .filter(fila -> fila.menuId().equals(pagina.getMenuId()))
                .findFirst()
                .orElseThrow();

        assertEquals(grupo.getMenuId(), plano.menuSupeId());
    }

    // ---------- helpers ----------

    private Contexto crearUsuarioConRolYSucursal() {
        Persona persona = nuevaPersona();
        PersUsua usuario = nuevoUsuario(persona);
        Rol rol = nuevoRol();
        Sucursal sucursal = nuevaSucursal();

        RolPersUsua asignacionRol = new RolPersUsua();
        asignacionRol.setRol(rol);
        asignacionRol.setUsuario(usuario);

        RolSucu rolSucu = new RolSucu();
        rolSucu.setRol(rol);
        rolSucu.setSucursal(sucursal);
        rolSucu.setAcceso(Boolean.TRUE);

        ejecutarEnTransaccion(() -> {
            entityManager.persist(persona);
            entityManager.persist(usuario);
            entityManager.persist(rol);
            entityManager.persist(sucursal);
            entityManager.persist(asignacionRol);
            entityManager.persist(rolSucu);
        });
        idsPersona.add(persona.getPersonaId());
        idsRol.add(rol.getRolId());
        idsSucursal.add(sucursal.getSucursalId());
        idsRolPersUsua.add(asignacionRol.getRolPersUsuaId());
        idsRolSucu.add(rolSucu.getRolSucuId());

        return new Contexto(persona.getPersonaId(), sucursal.getSucursalId(), rol);
    }

    private Menu agregarMenuAlRol(Rol rol, boolean acceso, boolean menuActivo) {
        Menu menu = nuevoMenu(null, menuActivo);
        RolMenu rolMenu = new RolMenu();
        rolMenu.setRol(rol);
        rolMenu.setMenu(menu);
        rolMenu.setAcceso(acceso);

        ejecutarEnTransaccion(() -> {
            entityManager.persist(menu);
            entityManager.persist(rolMenu);
        });
        idsMenu.add(menu.getMenuId());
        idsRolMenu.add(rolMenu.getRolMenuId());
        return menu;
    }

    private boolean contiene(List<MenuPlanoDTO> resultado, Menu menu) {
        return resultado.stream().anyMatch(fila -> fila.menuId().equals(menu.getMenuId()));
    }

    /** Página con ruta aleatoria: la ruta es única en la tabla. */
    private Menu nuevoMenu(Menu menuSupe, boolean estado) {
        Menu menu = new Menu();
        menu.setMenuSupe(menuSupe);
        menu.setDescri("Página de prueba");
        menu.setDetall(Boolean.TRUE);
        menu.setPaginaRuta("/prueba/" + UUID.randomUUID());
        menu.setEstado(estado);
        return menu;
    }

    private Menu nuevoGrupo() {
        Menu menu = new Menu();
        menu.setDescri("Grupo de prueba");
        menu.setDetall(Boolean.FALSE);
        menu.setEstado(Boolean.TRUE);
        return menu;
    }

    private Persona nuevaPersona() {
        Persona persona = new Persona();
        persona.setCedulaRuc(rucAleatorio());
        persona.setApelli("Apellido de Prueba");
        persona.setNombre("Nombre de Prueba");
        persona.setEstado(Boolean.TRUE);
        return persona;
    }

    private PersUsua nuevoUsuario(Persona persona) {
        PersUsua usuario = new PersUsua();
        usuario.setPersona(persona);
        usuario.setUsuari("usuario.test." + UUID.randomUUID().toString().substring(0, 8));
        usuario.setClave("hash-de-prueba");
        usuario.setCaducaClav(Boolean.FALSE);
        usuario.setEstado(Boolean.TRUE);
        return usuario;
    }

    private Rol nuevoRol() {
        Rol rol = new Rol();
        rol.setDescri("Rol de prueba " + UUID.randomUUID().toString().substring(0, 8));
        rol.setEstado(Boolean.TRUE);
        return rol;
    }

    private Sucursal nuevaSucursal() {
        Sucursal sucursal = new Sucursal();
        sucursal.setCodigo("T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        sucursal.setRuc(rucAleatorio());
        sucursal.setMatriz(false);
        sucursal.setEstado(Boolean.TRUE);
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

    private record Contexto(Integer personaId, Integer sucursalId, Rol rol) {
    }
}
