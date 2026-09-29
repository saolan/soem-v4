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

import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.configuracion.dto.CertEmisDTO;
import com.saolan.soem.configuracion.dto.SucuCertEmisDTO;
import com.saolan.soem.configuracion.dto.SucursalDTO;
import com.saolan.soem.configuracion.modelo.CertEmis;
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
 * Test de integración end-to-end de {@code CertEmisControlador}: login real
 * y llamadas HTTP a {@code /api/v1/certificados-emisor}.
 * <p>
 * <b>Precondición</b>: usuario semilla con los permisos
 * {@code certificados-emisor:buscar/crear/modificar/eliminar} (V5 + V6, ver el
 * bloque de resincronización de {@code db/seed-usuario-prueba.sql}) y
 * {@code sucursales:crear/modificar} para el caso de emisor en uso.
 */
@ExtendWith(ArquillianExtension.class)
class CertEmisControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(CertEmisControladorIT.class.getPackage());
        return war;
    }

    @ArquillianResource
    private URL baseUrl;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsCreados = new ArrayList<>();
    private final List<Integer> idsSucursalesCreadas = new ArrayList<>();
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
                // Primero las sucursales: su orphanRemoval borra SUCU_CERT_EMIS, que referencia a CERT_EMIS.
                for (Integer id : idsSucursalesCreadas) {
                    Sucursal sucursal = entityManager.find(Sucursal.class, id);
                    if (sucursal != null) {
                        entityManager.remove(sucursal);
                    }
                }
                entityManager.flush();
                for (Integer id : idsCreados) {
                    CertEmis certEmis = entityManager.find(CertEmis.class, id);
                    if (certEmis != null) {
                        entityManager.remove(certEmis);
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
        assertEquals(200, get("api/v1/certificados-emisor/combo").getStatus());
    }

    @Test
    void combo_sinToken_deberiaRetornar401() {
        Response respuesta = target("api/v1/certificados-emisor/combo").request(MediaType.APPLICATION_JSON).get();

        assertEquals(401, respuesta.getStatus());
    }

    @Test
    void buscar_conToken_deberiaRetornarListadoPaginado() {
        Response respuesta = target("api/v1/certificados-emisor")
                .queryParam("pagina", 1)
                .queryParam("tamanio", 5)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
        RespuestaApi<List<CertEmisDTO>> cuerpo = respuesta.readEntity(new GenericType<RespuestaApi<List<CertEmisDTO>>>() { });
        assertTrue(cuerpo.exito());
        assertNotNull(cuerpo.paginacion());
    }

    @Test
    void crear_modificar_yEliminar_deberiaCompletarElCiclo() {
        Response respuestaCrear = crear(certEmisDTO(descriAleatoria(), true));
        assertEquals(201, respuestaCrear.getStatus());
        Integer id = registrarParaLimpieza(respuestaCrear);

        String descriNueva = descriAleatoria();
        Response respuestaModificar = target("api/v1/certificados-emisor/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(certEmisDTO(descriNueva, true), MediaType.APPLICATION_JSON));
        assertEquals(200, respuestaModificar.getStatus());
        assertEquals(descriNueva, respuestaModificar.readEntity(new GenericType<RespuestaApi<CertEmisDTO>>() { }).datos().descri());

        Response respuestaEliminar = target("api/v1/certificados-emisor/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
        assertEquals(200, respuestaEliminar.getStatus());

        Response respuestaBuscar = get("api/v1/certificados-emisor/" + id);
        assertEquals(200, respuestaBuscar.getStatus());
        assertFalse(respuestaBuscar.readEntity(new GenericType<RespuestaApi<CertEmisDTO>>() { }).datos().estado());
    }

    @Test
    void crear_conDescriDuplicada_deberiaRetornar409() {
        String descri = descriAleatoria();
        Response respuestaPrimera = crear(certEmisDTO(descri, true));
        assertEquals(201, respuestaPrimera.getStatus());
        registrarParaLimpieza(respuestaPrimera);

        Response respuestaSegunda = crear(certEmisDTO(descri.toUpperCase(Locale.ROOT), true));

        assertEquals(409, respuestaSegunda.getStatus());
    }

    @Test
    void eliminar_emisorAsignadoASucursal_deberiaRetornar409() {
        Response respuestaCrear = crear(certEmisDTO(descriAleatoria(), true));
        Integer certEmisId = registrarParaLimpieza(respuestaCrear);
        Integer sucursalId = crearSucursalConCertificado(certEmisId);
        assumeTrue(sucursalId != null, "Requiere permisos de sucursales para preparar el emisor en uso");

        Response respuestaEliminar = target("api/v1/certificados-emisor/" + certEmisId)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();

        assertEquals(409, respuestaEliminar.getStatus());
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

    private Response crear(CertEmisDTO certEmisDTO) {
        return target("api/v1/certificados-emisor")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(certEmisDTO, MediaType.APPLICATION_JSON));
    }

    private Integer registrarParaLimpieza(Response respuestaCrear) {
        Integer id = respuestaCrear.readEntity(new GenericType<RespuestaApi<CertEmisDTO>>() { }).datos().certEmisId();
        idsCreados.add(id);
        return id;
    }

    /**
     * Crea una sucursal por API y le asigna el emisor; devuelve {@code null}
     * si el usuario de prueba no tiene permisos para hacerlo.
     */
    private Integer crearSucursalConCertificado(Integer certEmisId) {
        long numero = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000_000L;
        String ruc = String.format(Locale.ROOT, "%013d", numero);
        SucursalDTO sucursalDTO = new SucursalDTO(null, "CE" + UUID.randomUUID().toString().substring(0, 8), false,
                "Sucursal de prueba certificado emisor", ruc, "Razón Social de Prueba", null, "Ambato",
                "Av. de Prueba 123", "Av. de Prueba 123", "0999999999", null, "prueba@test.com",
                "Representante de Prueba", "0999999999", "Dirección Representante de Prueba", ruc, null, null, null, true);

        Response respuestaSucursal = target("api/v1/sucursales")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(sucursalDTO, MediaType.APPLICATION_JSON));
        if (respuestaSucursal.getStatus() != 201) {
            return null;
        }
        Integer sucursalId = respuestaSucursal.readEntity(new GenericType<RespuestaApi<SucursalDTO>>() { }).datos().sucursalId();
        idsSucursalesCreadas.add(sucursalId);

        SucuCertEmisDTO certificado = new SucuCertEmisDTO(certEmisId, "ruta-test", "Certificado de prueba", "clave-test");
        Response respuestaCertificado = target("api/v1/sucursales/" + sucursalId + "/certificado-emision")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(certificado, MediaType.APPLICATION_JSON));
        return respuestaCertificado.getStatus() == 200 ? sucursalId : null;
    }

    private CertEmisDTO certEmisDTO(String descri, boolean estado) {
        return new CertEmisDTO(null, descri, "CN Test", "OU Test", "O Test", "EC", "1.2.3.4", estado);
    }

    private String descriAleatoria() {
        return "Emisor de prueba " + UUID.randomUUID().toString().substring(0, 8);
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
