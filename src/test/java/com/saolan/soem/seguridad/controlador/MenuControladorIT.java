package com.saolan.soem.seguridad.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.saolan.soem.compartido.dto.AsignacionAccesoDTO;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;
import com.saolan.soem.seguridad.dto.MenuDTO;
import com.saolan.soem.seguridad.dto.RolDTO;
import com.saolan.soem.seguridad.modelo.Menu;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolMenu;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Test de integración end-to-end de {@code MenuControlador}, de
 * {@code PUT /roles/{id}/menus} y de {@code GET /auth/menu}: login real
 * (usuario semilla con permisos {@code seguridad-crud:*}) y llamadas HTTP.
 */
@ExtendWith(ArquillianExtension.class)
class MenuControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(MenuControladorIT.class.getPackage());
        return war;
    }

    @ArquillianResource
    private URL baseUrl;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsMenuCreados = new ArrayList<>();
    private final List<Integer> idsRolCreados = new ArrayList<>();
    private Client cliente;
    private String accessToken;

    @BeforeEach
    void iniciarSesion() {
        cliente = ClientBuilder.newClient();
        Response respuestaLogin = target("api/v1/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(new LoginRequestDTO(USUARIO_PRUEBA, CLAVE_PRUEBA, null), MediaType.APPLICATION_JSON));
        assumeTrue(respuestaLogin.getStatus() == 200,
                "Requiere el usuario de prueba '" + USUARIO_PRUEBA + "' con permisos de seguridad-crud ya cargado en la base");
        accessToken = respuestaLogin.readEntity(new GenericType<RespuestaApi<LoginRespuestaDTO>>() { }).datos().accessToken();
    }

    @AfterEach
    void limpiar() {
        try {
            ejecutarEnTransaccion(() -> {
                eliminarRolMenus(idsRolCreados);
                eliminarPorId(Rol.class, idsRolCreados);
                eliminarPorId(Menu.class, idsMenuCreados.reversed());
            });
        } finally {
            if (cliente != null) {
                cliente.close();
            }
        }
    }

    @Test
    void buscar_conToken_deberiaRetornarPaginado() {
        assertEquals(200, get("api/v1/menus").getStatus());
    }

    @Test
    void combo_conToken_deberiaRetornar200() {
        assertEquals(200, get("api/v1/menus/combo").getStatus());
    }

    @Test
    void arbol_conToken_deberiaRetornar200() {
        assertEquals(200, get("api/v1/menus/arbol").getStatus());
    }

    @Test
    void menuDeSesion_conToken_deberiaRetornar200SinCache() {
        Response respuesta = get("api/v1/auth/menu");

        assertEquals(200, respuesta.getStatus());
        assertNotNull(respuesta.getHeaderString(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void menuDeSesion_sinToken_deberiaRetornar401() {
        Response respuesta = target("api/v1/auth/menu").request(MediaType.APPLICATION_JSON).get();

        assertEquals(401, respuesta.getStatus());
    }

    @Test
    void crear_deberiaRetornar201() {
        Integer idGrupo = crearMenu(null, false);

        assertNotNull(idGrupo);
    }

    @Test
    void crear_deberiaRetornar400_cuandoLaPaginaNoTieneRuta() {
        Response respuesta = post("api/v1/menus", new MenuDTO(null, null, "Página sin ruta", null, true, true));

        assertEquals(400, respuesta.getStatus());
    }

    @Test
    void modificar_deberiaActualizarDatos() {
        Integer idGrupo = crearMenu(null, false);
        String descriNueva = "Grupo modificado";

        Response respuesta = target("api/v1/menus/" + idGrupo)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(new MenuDTO(idGrupo, null, descriNueva, null, false, true), MediaType.APPLICATION_JSON));

        assertEquals(200, respuesta.getStatus());
        assertEquals(descriNueva, respuesta.readEntity(new GenericType<RespuestaApi<MenuDTO>>() { }).datos().descri());
    }

    @Test
    void eliminar_deberiaMarcarEstadoFalse() {
        Integer idGrupo = crearMenu(null, false);

        Response respuestaEliminar = target("api/v1/menus/" + idGrupo)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
        assertEquals(200, respuestaEliminar.getStatus());

        RespuestaApi<MenuDTO> cuerpo = get("api/v1/menus/" + idGrupo)
                .readEntity(new GenericType<RespuestaApi<MenuDTO>>() { });
        assertFalse(cuerpo.datos().estado());
    }

    @Test
    void reemplazarMenus_deberiaAceptarReenviarLosMismosMenus() {
        Integer idGrupo = crearMenu(null, false);
        Integer idPagina = crearMenu(idGrupo, true);
        Response respuestaRol = post("api/v1/roles", new RolDTO(null, "Rol de prueba " + UUID.randomUUID(), true));
        Integer idRol = respuestaRol.readEntity(new GenericType<RespuestaApi<RolDTO>>() { }).datos().rolId();
        idsRolCreados.add(idRol);
        List<AsignacionAccesoDTO> asignaciones = List.of(new AsignacionAccesoDTO(idGrupo, true),
                new AsignacionAccesoDTO(idPagina, true));

        assertEquals(200, putMenusDelRol(idRol, asignaciones).getStatus());
        assertEquals(200, putMenusDelRol(idRol, asignaciones).getStatus());
    }

    // ---------- helpers ----------

    private Integer crearMenu(Integer menuSupeId, boolean esPagina) {
        String ruta = esPagina ? "/prueba/" + UUID.randomUUID() : null;
        Response respuesta = post("api/v1/menus", new MenuDTO(null, menuSupeId, "Menú de prueba", ruta, esPagina, true));
        assertEquals(201, respuesta.getStatus());
        Integer id = respuesta.readEntity(new GenericType<RespuestaApi<MenuDTO>>() { }).datos().menuId();
        idsMenuCreados.add(id);
        return id;
    }

    private Response putMenusDelRol(Integer idRol, List<AsignacionAccesoDTO> asignaciones) {
        return target("api/v1/roles/" + idRol + "/menus")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(asignaciones, MediaType.APPLICATION_JSON));
    }

    private Response get(String path) {
        return target(path)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();
    }

    private Response post(String path, Object cuerpo) {
        return target(path)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(cuerpo, MediaType.APPLICATION_JSON));
    }

    private <T> void eliminarPorId(Class<T> clase, List<Integer> ids) {
        for (Integer id : ids) {
            T entidad = entityManager.find(clase, id);
            if (entidad != null) {
                entityManager.remove(entidad);
                entityManager.flush();
            }
        }
    }

    private void eliminarRolMenus(List<Integer> rolIds) {
        if (rolIds.isEmpty()) {
            return;
        }
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RolMenu> query = criteriaBuilder.createQuery(RolMenu.class);
        Root<RolMenu> raiz = query.from(RolMenu.class);
        query.where(raiz.get("rol").get("rolId").in(rolIds));
        for (RolMenu rolMenu : entityManager.createQuery(query).getResultList()) {
            entityManager.remove(rolMenu);
        }
        entityManager.flush();
    }

    private WebTarget target(String path) {
        try {
            return cliente.target(baseUrl.toURI()).path(path);
        } catch (URISyntaxException excepcion) {
            throw new IllegalStateException("URL de despliegue inválida: " + baseUrl, excepcion);
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
            throw new IllegalStateException("Error limpiando datos de prueba", excepcion);
        }
    }
}
