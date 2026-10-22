package com.saolan.soem.configuracion.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.saolan.soem.compartido.dto.AsignacionAccesoDTO;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.configuracion.dto.BodegaDTO;
import com.saolan.soem.configuracion.dto.SucuBodeDTO;
import com.saolan.soem.configuracion.dto.SucursalDTO;
import com.saolan.soem.configuracion.modelo.Bodega;
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
 * Test de integración end-to-end de {@code BodegaControlador} y del
 * sub-recurso {@code /sucursales/{id}/bodegas}: login real y llamadas HTTP.
 * <p>
 * <b>Precondición</b>: usuario semilla con los permisos
 * {@code configuracion-crud:buscar/crear/modificar/eliminar}.
 */
@ExtendWith(ArquillianExtension.class)
class BodegaControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(BodegaControladorIT.class.getPackage());
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
                // Primero las sucursales: su orphanRemoval borra SUCU_BODE, que referencia a BODEGA.
                for (Integer id : idsSucursalesCreadas) {
                    Sucursal sucursal = entityManager.find(Sucursal.class, id);
                    if (sucursal != null) {
                        entityManager.remove(sucursal);
                    }
                }
                entityManager.flush();
                for (Integer id : idsCreados) {
                    Bodega bodega = entityManager.find(Bodega.class, id);
                    if (bodega != null) {
                        entityManager.remove(bodega);
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
        assertEquals(200, get("api/v1/bodegas/combo").getStatus());
    }

    @Test
    void buscar_conToken_deberiaRetornarListadoPaginado() {
        assertEquals(200, get("api/v1/bodegas?pagina=1&tamanio=5").getStatus());
    }

    @Test
    void crear_modificar_yEliminar_deberiaCompletarElCiclo() {
        Integer id = registrarParaLimpieza(crear(new BodegaDTO(null, descriAleatoria(), true)));

        String descriNueva = descriAleatoria();
        Response respuestaModificar = target("api/v1/bodegas/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(new BodegaDTO(id, descriNueva, true), MediaType.APPLICATION_JSON));
        assertEquals(200, respuestaModificar.getStatus());
        assertEquals(descriNueva, respuestaModificar.readEntity(new GenericType<RespuestaApi<BodegaDTO>>() { }).datos().descri());

        assertEquals(200, eliminar(id).getStatus());
        Response respuestaBuscar = get("api/v1/bodegas/" + id);
        assertFalse(respuestaBuscar.readEntity(new GenericType<RespuestaApi<BodegaDTO>>() { }).datos().estado());
    }

    @Test
    void crear_conDescriDuplicada_deberiaRetornar409() {
        String descri = descriAleatoria();
        registrarParaLimpieza(crear(new BodegaDTO(null, descri, true)));

        Response respuestaSegunda = crear(new BodegaDTO(null, descri.toUpperCase(Locale.ROOT), true));

        assertEquals(409, respuestaSegunda.getStatus());
    }

    @Test
    void bodegasDeSucursal_deberiaReemplazarseLeerseYBloquearLaBajaDeLaBodega() {
        Integer bodegaId = registrarParaLimpieza(crear(new BodegaDTO(null, descriAleatoria(), true)));
        Integer sucursalId = crearSucursal();
        assumeTrue(sucursalId != null, "Requiere permisos para crear sucursales");
        List<AsignacionAccesoDTO> asignaciones = List.of(new AsignacionAccesoDTO(bodegaId, true));

        assertEquals(200, reemplazarBodegas(sucursalId, asignaciones).getStatus());
        assertEquals(200, reemplazarBodegas(sucursalId, asignaciones).getStatus());

        List<SucuBodeDTO> bodegas = get("api/v1/sucursales/" + sucursalId + "/bodegas")
                .readEntity(new GenericType<RespuestaApi<List<SucuBodeDTO>>>() { }).datos();
        assertEquals(1, bodegas.size());
        assertEquals(bodegaId, bodegas.get(0).bodegaId());

        assertEquals(409, eliminar(bodegaId).getStatus());

        assertEquals(200, reemplazarBodegas(sucursalId, List.of()).getStatus());
        assertEquals(200, eliminar(bodegaId).getStatus());
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

    private Response crear(BodegaDTO bodegaDTO) {
        return target("api/v1/bodegas")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(bodegaDTO, MediaType.APPLICATION_JSON));
    }

    private Response eliminar(Integer bodegaId) {
        return target("api/v1/bodegas/" + bodegaId)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
    }

    private Response reemplazarBodegas(Integer sucursalId, List<AsignacionAccesoDTO> asignaciones) {
        return target("api/v1/sucursales/" + sucursalId + "/bodegas")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(asignaciones, MediaType.APPLICATION_JSON));
    }

    private Integer registrarParaLimpieza(Response respuestaCrear) {
        assertEquals(201, respuestaCrear.getStatus());
        Integer id = respuestaCrear.readEntity(new GenericType<RespuestaApi<BodegaDTO>>() { }).datos().bodegaId();
        idsCreados.add(id);
        return id;
    }

    /** Crea una sucursal por API; devuelve {@code null} si el usuario de prueba no tiene permisos. */
    private Integer crearSucursal() {
        long numero = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000_000L;
        String ruc = String.format(Locale.ROOT, "%013d", numero);
        SucursalDTO sucursalDTO = new SucursalDTO(null, "BO" + UUID.randomUUID().toString().substring(0, 8), false,
                "Sucursal de prueba bodegas", ruc, "Razón Social de Prueba", null, "Ambato",
                "Av. de Prueba 123", "Av. de Prueba 123", "0999999999", null, "prueba@test.com",
                "Representante de Prueba", "0999999999", "Dirección Representante de Prueba", ruc, null, null, null, true);

        Response respuesta = target("api/v1/sucursales")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(sucursalDTO, MediaType.APPLICATION_JSON));
        if (respuesta.getStatus() != 201) {
            return null;
        }
        Integer sucursalId = respuesta.readEntity(new GenericType<RespuestaApi<SucursalDTO>>() { }).datos().sucursalId();
        idsSucursalesCreadas.add(sucursalId);
        return sucursalId;
    }

    private String descriAleatoria() {
        return "Bodega de prueba " + UUID.randomUUID().toString().substring(0, 8);
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
