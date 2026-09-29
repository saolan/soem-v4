package com.saolan.soem.seguridad.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;
import com.saolan.soem.seguridad.dto.PermisoDTO;
import com.saolan.soem.seguridad.modelo.Permiso;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
 * Test de integración end-to-end de {@code PermisoControlador}: login real
 * (usuario semilla con permisos {@code permisos:*}) y CRUD (sin
 * {@code DELETE}, ese endpoint no existe) vía HTTP.
 */
@ExtendWith(ArquillianExtension.class)
class PermisoControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(PermisoControladorIT.class.getPackage());
        return war;
    }

    @ArquillianResource
    private URL baseUrl;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();
    private Client cliente;
    private String accessToken;

    @BeforeEach
    void iniciarSesion() {
        cliente = ClientBuilder.newClient();
        Response respuestaLogin = target("api/v1/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(new LoginRequestDTO(USUARIO_PRUEBA, CLAVE_PRUEBA, null), MediaType.APPLICATION_JSON));
        assumeTrue(respuestaLogin.getStatus() == 200,
                "Requiere el usuario de prueba '" + USUARIO_PRUEBA + "' con permisos de permisos ya cargado en la base");
        accessToken = respuestaLogin.readEntity(new GenericType<RespuestaApi<LoginRespuestaDTO>>() { }).datos().accessToken();
    }

    @AfterEach
    void limpiar() {
        try {
            ejecutarEnTransaccion(() -> {
                for (Integer id : idsCreados) {
                    Permiso permiso = entityManager.find(Permiso.class, id);
                    if (permiso != null) {
                        entityManager.remove(permiso);
                    }
                }
            });
        } finally {
            if (cliente != null) {
                cliente.close();
            }
        }
    }

    @Test
    void buscar_conToken_deberiaRetornarPaginado() {
        Response respuesta = target("api/v1/permisos")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
    }

    @Test
    void combo_conToken_deberiaRetornar200() {
        Response respuesta = target("api/v1/permisos/combo")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
    }

    @Test
    void crear_deberiaRetornar201() {
        Response respuesta = crearPermiso(recursoAleatorio(), Accion.BUSCAR);

        assertEquals(201, respuesta.getStatus());
        registrarParaLimpieza(respuesta);
    }

    @Test
    void crear_conRecursoYAccionDuplicados_deberiaRetornar409() {
        String recurso = recursoAleatorio();
        Response respuestaPrimera = crearPermiso(recurso, Accion.BUSCAR);
        assertEquals(201, respuestaPrimera.getStatus());
        registrarParaLimpieza(respuestaPrimera);

        Response respuestaSegunda = crearPermiso(recurso, Accion.BUSCAR);

        assertEquals(409, respuestaSegunda.getStatus());
    }

    @Test
    void modificar_deberiaActualizarDatos() {
        Integer id = registrarParaLimpieza(crearPermiso(recursoAleatorio(), Accion.BUSCAR));
        String recursoNuevo = recursoAleatorio();

        Response respuestaModificar = target("api/v1/permisos/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(new PermisoDTO(id, "Permiso modificado", recursoNuevo, Accion.MODIFICAR, null), MediaType.APPLICATION_JSON));

        assertEquals(200, respuestaModificar.getStatus());
        RespuestaApi<PermisoDTO> cuerpo = respuestaModificar.readEntity(new GenericType<RespuestaApi<PermisoDTO>>() { });
        assertEquals(recursoNuevo, cuerpo.datos().recurso());
        assertEquals(Accion.MODIFICAR, cuerpo.datos().accion());
    }

    @Test
    void modificar_deberiaRetornar404_cuandoElPermisoNoExiste() {
        Response respuesta = target("api/v1/permisos/999999999")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(new PermisoDTO(999999999, "x", recursoAleatorio(), Accion.BUSCAR, null), MediaType.APPLICATION_JSON));

        assertEquals(404, respuesta.getStatus());
    }

    // ---------- helpers ----------

    private Response crearPermiso(String recurso, Accion accion) {
        PermisoDTO dto = new PermisoDTO(null, "Permiso de prueba", recurso, accion, null);
        return target("api/v1/permisos")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(dto, MediaType.APPLICATION_JSON));
    }

    private Integer registrarParaLimpieza(Response respuestaCrear) {
        Integer id = respuestaCrear.readEntity(new GenericType<RespuestaApi<PermisoDTO>>() { }).datos().permisoId();
        idsCreados.add(id);
        return id;
    }

    private String recursoAleatorio() {
        return "recurso-test-" + UUID.randomUUID().toString().substring(0, 8);
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
