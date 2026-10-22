package com.saolan.soem.seguridad.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
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
import com.saolan.soem.seguridad.dto.CambioClaveDTO;
import com.saolan.soem.seguridad.dto.CambioSucursalDTO;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;
import com.saolan.soem.seguridad.dto.SesionActualDTO;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * Test de integración end-to-end de {@code AuthControlador}: login, refresh
 * (rotación y detección de reuso), logout, {@code /me} y
 * {@code /cambiar-clave} contra WildFly real.
 * <p>
 * <b>Precondición</b>: mismo usuario semilla que {@code SucursalControladorIT}
 * (system properties {@code test.sucursales.usuario}/{@code test.sucursales.clave}).
 * El test de {@code cambiarClave} cambia y restaura la clave del usuario
 * semilla en el mismo método (con {@code finally}), para no dejar el resto
 * de los tests de integración (que reutilizan ese mismo usuario) sin poder
 * loguearse.
 */
@ExtendWith(ArquillianExtension.class)
class AuthControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");
    private static final String COOKIE_REFRESH = "refreshToken";

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(AuthControladorIT.class.getPackage());
        return war;
    }

    @ArquillianResource
    private URL baseUrl;

    private Client cliente;
    private String accessToken;

    @BeforeEach
    void iniciarCliente() {
        cliente = ClientBuilder.newClient();
        Response respuestaLogin = login(USUARIO_PRUEBA, CLAVE_PRUEBA);
        assumeTrue(respuestaLogin.getStatus() == 200,
                "Requiere el usuario de prueba '" + USUARIO_PRUEBA + "' ya cargado en la base");
        accessToken = cuerpoLogin(respuestaLogin).accessToken();
    }

    @AfterEach
    void cerrarCliente() {
        if (cliente != null) {
            cliente.close();
        }
    }

    @Test
    void login_deberiaRetornar200_conCredencialesValidas() {
        Response respuesta = login(USUARIO_PRUEBA, CLAVE_PRUEBA);

        assertEquals(200, respuesta.getStatus());
        LoginRespuestaDTO cuerpo = cuerpoLogin(respuesta);
        assertNotNull(cuerpo.accessToken());
        NewCookie cookie = respuesta.getCookies().get(COOKIE_REFRESH);
        assertNotNull(cookie);
        assertEquals(true, cookie.isHttpOnly());
    }

    @Test
    void login_deberiaRetornar401_conCredencialesInvalidas() {
        Response respuesta = login(USUARIO_PRUEBA, "clave-definitivamente-incorrecta");

        assertEquals(401, respuesta.getStatus());
    }

    @Test
    void refresh_deberiaRotarElTokenYRechazarElAnteriorPorReuso() {
        Response respuestaLogin = login(USUARIO_PRUEBA, CLAVE_PRUEBA);
        NewCookie cookieOriginal = respuestaLogin.getCookies().get(COOKIE_REFRESH);

        Response respuestaRefresh = target("api/v1/auth/refresh")
                .request(MediaType.APPLICATION_JSON)
                .cookie(cookieOriginal)
                .post(Entity.entity("", MediaType.APPLICATION_JSON));
        assertEquals(200, respuestaRefresh.getStatus());
        NewCookie cookieRotada = respuestaRefresh.getCookies().get(COOKIE_REFRESH);
        assertNotEquals(cookieOriginal.getValue(), cookieRotada.getValue());

        Response respuestaReuso = target("api/v1/auth/refresh")
                .request(MediaType.APPLICATION_JSON)
                .cookie(cookieOriginal)
                .post(Entity.entity("", MediaType.APPLICATION_JSON));
        assertEquals(401, respuestaReuso.getStatus());
    }

    @Test
    void logout_deberiaRevocarLaCookieYRechazarUnRefreshPosterior() {
        Response respuestaLogin = login(USUARIO_PRUEBA, CLAVE_PRUEBA);
        NewCookie cookie = respuestaLogin.getCookies().get(COOKIE_REFRESH);

        Response respuestaLogout = target("api/v1/auth/logout")
                .request(MediaType.APPLICATION_JSON)
                .cookie(cookie)
                .post(Entity.entity("", MediaType.APPLICATION_JSON));
        assertEquals(200, respuestaLogout.getStatus());

        Response respuestaRefresh = target("api/v1/auth/refresh")
                .request(MediaType.APPLICATION_JSON)
                .cookie(cookie)
                .post(Entity.entity("", MediaType.APPLICATION_JSON));
        assertEquals(401, respuestaRefresh.getStatus());
    }

    @Test
    void me_deberiaRetornar200_conLaIdentidadDeLaSesion() {
        Response respuesta = target("api/v1/auth/me")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
        RespuestaApi<SesionActualDTO> cuerpo = respuesta.readEntity(new GenericType<RespuestaApi<SesionActualDTO>>() { });
        assertEquals(USUARIO_PRUEBA, cuerpo.datos().usuario());
    }

    @Test
    void me_sinToken_deberiaRetornar401() {
        Response respuesta = target("api/v1/auth/me").request(MediaType.APPLICATION_JSON).get();

        assertEquals(401, respuesta.getStatus());
    }

    @Test
    void login_deberiaIncluirLasSucursalesAccesibles() {
        LoginRespuestaDTO cuerpo = cuerpoLogin(login(USUARIO_PRUEBA, CLAVE_PRUEBA));

        assertNotNull(cuerpo.sucursales());
        assertTrue(cuerpo.sucursales().stream().anyMatch(s -> s.valor().equals(cuerpo.sucursalId())));
    }

    @Test
    void cambiarSucursal_deberiaEmitirSesionConLaSucursalElegida() {
        Response respuestaLogin = login(USUARIO_PRUEBA, CLAVE_PRUEBA);
        NewCookie cookie = respuestaLogin.getCookies().get(COOKIE_REFRESH);
        LoginRespuestaDTO cuerpoLogin = cuerpoLogin(respuestaLogin);
        Integer sucursalElegida = cuerpoLogin.sucursales().get(cuerpoLogin.sucursales().size() - 1).valor();

        Response respuesta = target("api/v1/auth/cambiar-sucursal")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cuerpoLogin.accessToken())
                .cookie(cookie)
                .post(Entity.entity(new CambioSucursalDTO(sucursalElegida), MediaType.APPLICATION_JSON));

        assertEquals(200, respuesta.getStatus());
        assertEquals(sucursalElegida, cuerpoLogin(respuesta).sucursalId());
        assertNotEquals(cookie.getValue(), respuesta.getCookies().get(COOKIE_REFRESH).getValue());
    }

    @Test
    void cambiarSucursal_sinToken_deberiaRetornar401() {
        Response respuesta = target("api/v1/auth/cambiar-sucursal")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(new CambioSucursalDTO(1), MediaType.APPLICATION_JSON));

        assertEquals(401, respuesta.getStatus());
    }

    @Test
    void cambiarClave_deberiaActualizarLaClave_yPermitirLoginConLaNueva() {
        String claveNueva = "ClaveTemporal$" + UUID.randomUUID().toString().substring(0, 8);
        try {
            Response respuestaCambio = target("api/v1/auth/cambiar-clave")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .post(Entity.entity(new CambioClaveDTO(CLAVE_PRUEBA, claveNueva), MediaType.APPLICATION_JSON));
            assertEquals(200, respuestaCambio.getStatus());

            assertEquals(200, login(USUARIO_PRUEBA, claveNueva).getStatus());
            assertEquals(401, login(USUARIO_PRUEBA, CLAVE_PRUEBA).getStatus());
        } finally {
            // Revertir: el resto de los tests de integración reutilizan este mismo usuario semilla.
            target("api/v1/auth/cambiar-clave")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .post(Entity.entity(new CambioClaveDTO(claveNueva, CLAVE_PRUEBA), MediaType.APPLICATION_JSON));
        }
    }

    // ---------- helpers ----------

    private Response login(String usuario, String clave) {
        return target("api/v1/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(new LoginRequestDTO(usuario, clave, null), MediaType.APPLICATION_JSON));
    }

    private LoginRespuestaDTO cuerpoLogin(Response respuesta) {
        RespuestaApi<LoginRespuestaDTO> cuerpo = respuesta.readEntity(new GenericType<RespuestaApi<LoginRespuestaDTO>>() { });
        return cuerpo.datos();
    }

    private WebTarget target(String path) {
        try {
            return cliente.target(baseUrl.toURI()).path(path);
        } catch (URISyntaxException excepcion) {
            throw new IllegalStateException("URL de despliegue inválida: " + baseUrl, excepcion);
        }
    }
}
