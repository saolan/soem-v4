package com.saolan.soem.configuracion.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.saolan.soem.configuracion.dto.ParametroDTO;
import com.saolan.soem.configuracion.modelo.Parametro;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;

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
 * Test de integración end-to-end de {@code ParametroControlador}: login real
 * y llamadas HTTP a {@code /api/v1/parametros}.
 * <p>
 * <b>Precondición</b>: usuario semilla con los permisos
 * {@code parametros:buscar/crear/modificar/eliminar} (V7, ver el bloque de
 * resincronización de {@code db/seed-usuario-prueba.sql}).
 */
@ExtendWith(ArquillianExtension.class)
class ParametroControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(ParametroControladorIT.class.getPackage());
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
                "Requiere el usuario de prueba '" + USUARIO_PRUEBA + "' ya cargado en la base");
        accessToken = respuestaLogin.readEntity(new GenericType<RespuestaApi<LoginRespuestaDTO>>() { }).datos().accessToken();
    }

    @AfterEach
    void cerrarClienteYLimpiar() {
        try {
            ejecutarEnTransaccion(() -> {
                for (Integer id : idsCreados) {
                    Parametro parametro = entityManager.find(Parametro.class, id);
                    if (parametro != null) {
                        entityManager.remove(parametro);
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
    void combo_conToken_deberiaRetornar200() {
        assertEquals(200, get("api/v1/parametros/combo").getStatus());
    }

    @Test
    void combo_sinToken_deberiaRetornar401() {
        Response respuesta = target("api/v1/parametros/combo").request(MediaType.APPLICATION_JSON).get();

        assertEquals(401, respuesta.getStatus());
    }

    @Test
    void buscar_conToken_deberiaRetornarListadoPaginado() {
        Response respuesta = target("api/v1/parametros")
                .queryParam("pagina", 1)
                .queryParam("tamanio", 5)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
        RespuestaApi<List<ParametroDTO>> cuerpo = respuesta.readEntity(new GenericType<RespuestaApi<List<ParametroDTO>>>() { });
        assertTrue(cuerpo.exito());
        assertNotNull(cuerpo.paginacion());
    }

    @Test
    void crear_modificar_yEliminar_deberiaCompletarElCiclo() {
        Response respuestaCrear = crear(parametroDTO(descriAleatoria()));
        assertEquals(201, respuestaCrear.getStatus());
        Integer id = registrarParaLimpieza(respuestaCrear);

        String descriNueva = descriAleatoria();
        Response respuestaModificar = target("api/v1/parametros/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(parametroDTO(descriNueva), MediaType.APPLICATION_JSON));
        assertEquals(200, respuestaModificar.getStatus());
        assertEquals(descriNueva, respuestaModificar.readEntity(new GenericType<RespuestaApi<ParametroDTO>>() { }).datos().descri());

        Response respuestaEliminar = target("api/v1/parametros/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
        assertEquals(200, respuestaEliminar.getStatus());

        Response respuestaBuscar = get("api/v1/parametros/" + id);
        assertEquals(200, respuestaBuscar.getStatus());
        assertFalse(respuestaBuscar.readEntity(new GenericType<RespuestaApi<ParametroDTO>>() { }).datos().estado());
    }

    @Test
    void crear_sinCodigo_deberiaRetornar400() {
        Response respuesta = crear(new ParametroDTO(null, null, descriAleatoria(), true));

        assertEquals(400, respuesta.getStatus());
    }

    // ---------- helpers ----------

    private WebTarget target(String path) {
        try {
            return cliente.target(baseUrl.toURI()).path(path);
        } catch (URISyntaxException excepcion) {
            throw new IllegalStateException("URL de despliegue inválida: " + baseUrl, excepcion);
        }
    }

    private Response get(String path) {
        return target(path)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();
    }

    private Response crear(ParametroDTO parametroDTO) {
        return target("api/v1/parametros")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(parametroDTO, MediaType.APPLICATION_JSON));
    }

    private Integer registrarParaLimpieza(Response respuestaCrear) {
        Integer id = respuestaCrear.readEntity(new GenericType<RespuestaApi<ParametroDTO>>() { }).datos().parametroId();
        idsCreados.add(id);
        return id;
    }

    private ParametroDTO parametroDTO(String descri) {
        return new ParametroDTO(null, "PT-" + UUID.randomUUID().toString().substring(0, 8), descri, true);
    }

    private String descriAleatoria() {
        return "Parámetro de prueba " + UUID.randomUUID().toString().substring(0, 8);
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
