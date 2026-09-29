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
import java.util.Locale;
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

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.configuracion.dto.SucuCertEmisDTO;
import com.saolan.soem.configuracion.dto.SucursalDTO;
import com.saolan.soem.configuracion.modelo.Sucursal;
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
 * Test de integración end-to-end del {@code SucursalControlador}: login real
 * vía {@code AuthControlador} (JWT emitido de verdad, no fabricado) y
 * llamadas HTTP reales a {@code /api/v1/sucursales}.
 * <p>
 * <b>Precondición</b>: la base de datos de pruebas debe tener un usuario con
 * clave conocida, con un rol activo que tenga habilitados los permisos
 * {@code sucursales:buscar/crear/modificar/eliminar} (y, para el test del
 * certificado emisor, al menos un {@code CertEmis} activo cargado). Las
 * credenciales se leen de las system properties {@code test.sucursales.usuario}
 * / {@code test.sucursales.clave} (con un valor por defecto documentado
 * abajo); nunca se hardcodean en el código fuente para un entorno real.
 * Ejemplo: {@code mvn verify -Parq-managed -Dtest.sucursales.usuario=admin.test -Dtest.sucursales.clave=Prueba$2026}.
 */
@ExtendWith(ArquillianExtension.class)
class SucursalControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(SucursalControladorIT.class.getPackage());
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

        LoginRequestDTO credenciales = new LoginRequestDTO(USUARIO_PRUEBA, CLAVE_PRUEBA, null);
        Response respuestaLogin = target("api/v1/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(credenciales, MediaType.APPLICATION_JSON));

        assumeTrue(respuestaLogin.getStatus() == 200,
                "Requiere el usuario de prueba '" + USUARIO_PRUEBA + "' con permisos de sucursales ya cargado en la base");

        RespuestaApi<LoginRespuestaDTO> cuerpo = respuestaLogin.readEntity(new GenericType<RespuestaApi<LoginRespuestaDTO>>() { });
        accessToken = cuerpo.datos().accessToken();
    }

    @AfterEach
    void cerrarClienteYLimpiarSucursalesCreadas() {
        try {
            ejecutarEnTransaccion(() -> {
                for (Integer id : idsCreados) {
                    Sucursal sucursal = entityManager.find(Sucursal.class, id);
                    if (sucursal != null) {
                        entityManager.remove(sucursal);
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
    void buscar_sinToken_deberiaRetornar401() {
        Response respuesta = target("api/v1/sucursales").request(MediaType.APPLICATION_JSON).get();

        assertEquals(401, respuesta.getStatus());
    }

    @Test
    void buscar_conToken_deberiaRetornarListadoPaginado() {
        Response respuesta = target("api/v1/sucursales")
                .queryParam("pagina", 1)
                .queryParam("tamanio", 5)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
        RespuestaApi<List<SucursalDTO>> cuerpo = respuesta.readEntity(new GenericType<RespuestaApi<List<SucursalDTO>>>() { });
        assertTrue(cuerpo.exito());
        assertNotNull(cuerpo.paginacion());
    }

    @Test
    void crear_deberiaRetornar201_yQuedarDisponibleParaBuscarPorId() {
        Response respuestaCrear = crear(sucursalDTO(null, codigoAleatorio(), rucAleatorio(), false));
        assertEquals(201, respuestaCrear.getStatus());
        Integer id = registrarParaLimpieza(respuestaCrear);

        Response respuestaBuscar = target("api/v1/sucursales/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuestaBuscar.getStatus());
    }

    @Test
    void crear_conCodigoDuplicado_deberiaRetornar409() {
        String codigo = codigoAleatorio();
        Response respuestaPrimera = crear(sucursalDTO(null, codigo, rucAleatorio(), false));
        assertEquals(201, respuestaPrimera.getStatus());
        registrarParaLimpieza(respuestaPrimera);

        Response respuestaSegunda = crear(sucursalDTO(null, codigo, rucAleatorio(), false));

        assertEquals(409, respuestaSegunda.getStatus());
    }

    @Test
    void modificar_deberiaActualizarDatos() {
        Response respuestaCrear = crear(sucursalDTO(null, codigoAleatorio(), rucAleatorio(), false));
        Integer id = registrarParaLimpieza(respuestaCrear);
        String codigoNuevo = codigoAleatorio();

        Response respuestaModificar = target("api/v1/sucursales/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(sucursalDTO(id, codigoNuevo, rucAleatorio(), false), MediaType.APPLICATION_JSON));

        assertEquals(200, respuestaModificar.getStatus());
        RespuestaApi<SucursalDTO> cuerpo = respuestaModificar.readEntity(new GenericType<RespuestaApi<SucursalDTO>>() { });
        assertEquals(codigoNuevo, cuerpo.datos().codigo());
    }

    @Test
    void eliminar_deberiaMarcarEstadoFalse_sinBorrarFisicamente() {
        Response respuestaCrear = crear(sucursalDTO(null, codigoAleatorio(), rucAleatorio(), false));
        Integer id = registrarParaLimpieza(respuestaCrear);

        Response respuestaEliminar = target("api/v1/sucursales/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
        assertEquals(200, respuestaEliminar.getStatus());

        Response respuestaBuscar = target("api/v1/sucursales/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();
        assertEquals(200, respuestaBuscar.getStatus());
        RespuestaApi<SucursalDTO> cuerpo = respuestaBuscar.readEntity(new GenericType<RespuestaApi<SucursalDTO>>() { });
        assertFalse(cuerpo.datos().estado());
    }

    @Test
    void certificadoEmision_deberiaCrearseLeerseYEliminarse() {
        Response respuestaCombo = target("api/v1/certificados-emisor/combo")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();
        assertEquals(200, respuestaCombo.getStatus());
        RespuestaApi<List<ComboDTO>> comboBody = respuestaCombo.readEntity(new GenericType<RespuestaApi<List<ComboDTO>>>() { });
        assumeTrue(!comboBody.datos().isEmpty(), "Requiere al menos un CertEmis activo cargado en la base de pruebas");
        Integer certEmisId = comboBody.datos().get(0).valor();

        Response respuestaCrear = crear(sucursalDTO(null, codigoAleatorio(), rucAleatorio(), false));
        Integer id = registrarParaLimpieza(respuestaCrear);

        SucuCertEmisDTO certificado = new SucuCertEmisDTO(certEmisId, "ruta-test", "Certificado de prueba", "clave-test");
        Response respuestaGuardar = target("api/v1/sucursales/" + id + "/certificado-emision")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(certificado, MediaType.APPLICATION_JSON));
        assertEquals(200, respuestaGuardar.getStatus());

        Response respuestaLeer = target("api/v1/sucursales/" + id + "/certificado-emision")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();
        assertEquals(200, respuestaLeer.getStatus());

        Response respuestaEliminarCertificado = target("api/v1/sucursales/" + id + "/certificado-emision")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
        assertEquals(200, respuestaEliminarCertificado.getStatus());
    }

    // ---------- helpers ----------

    private WebTarget target(String path) {
        try {
            return cliente.target(baseUrl.toURI()).path(path);
        } catch (URISyntaxException excepcion) {
            throw new IllegalStateException("URL de despliegue inválida: " + baseUrl, excepcion);
        }
    }

    private Response crear(SucursalDTO sucursalDTO) {
        return target("api/v1/sucursales")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(sucursalDTO, MediaType.APPLICATION_JSON));
    }

    private Integer registrarParaLimpieza(Response respuestaCrear) {
        RespuestaApi<SucursalDTO> cuerpo = respuestaCrear.readEntity(new GenericType<RespuestaApi<SucursalDTO>>() { });
        Integer id = cuerpo.datos().sucursalId();
        idsCreados.add(id);
        return id;
    }

    private SucursalDTO sucursalDTO(Integer sucursalId, String codigo, String ruc, boolean matriz) {
        return new SucursalDTO(sucursalId, codigo, matriz, "Sucursal de prueba", ruc, "Razón Social de Prueba",
                null, "Quito", "Av. de Prueba 123", "Av. de Prueba 123", "0999999999", null,
                "prueba@test.com", "Representante de Prueba", "0999999999", "Dirección Representante de Prueba",
                ruc, null, null, null, true);
    }

    private String codigoAleatorio() {
        return "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String rucAleatorio() {
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
            throw new IllegalStateException("Error limpiando datos de prueba", excepcion);
        }
    }
}
