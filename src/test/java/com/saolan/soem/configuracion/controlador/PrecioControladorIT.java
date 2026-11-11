package com.saolan.soem.configuracion.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.math.BigDecimal;
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
import com.saolan.soem.compartido.prueba.SesionPrueba;
import com.saolan.soem.configuracion.dto.PrecioDTO;
import com.saolan.soem.configuracion.dto.SucuPrecDTO;
import com.saolan.soem.configuracion.dto.SucursalDTO;
import com.saolan.soem.configuracion.modelo.Precio;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;

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
 * Test de integracion end-to-end de {@code PrecioControlador} y del
 * sub-recurso {@code /sucursales/{id}/precios}: login real y llamadas HTTP.
 * <p>
 * <b>Precondicion</b>: usuario semilla con los permisos
 * {@code configuracion-crud:buscar/crear/modificar/eliminar}.
 */
@ExtendWith(ArquillianExtension.class)
class PrecioControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(PrecioControladorIT.class.getPackage());
        war.addClass(SesionPrueba.class);
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
                .post(Entity.entity(new LoginRequestDTO(USUARIO_PRUEBA, CLAVE_PRUEBA), MediaType.APPLICATION_JSON));
        assumeTrue(respuestaLogin.getStatus() == 200,
                "Requiere el usuario de prueba '" + USUARIO_PRUEBA + "' ya cargado en la base");
        accessToken = SesionPrueba.accessTokenConSucursal(respuestaLogin, this::target);
    }

    @AfterEach
    void cerrarClienteYLimpiar() {
        try {
            ejecutarEnTransaccion(() -> {
                // PROD_PREC (alcance "todos") referencia tanto a PRECIO como a las sucursales de prueba.
                for (Integer id : idsCreados) {
                    entityManager.createNativeQuery("DELETE FROM prod_prec WHERE precio_id = ?1").setParameter(1, id).executeUpdate();
                }
                // Luego las sucursales: su orphanRemoval borra sus SUCU_PREC.
                for (Integer id : idsSucursalesCreadas) {
                    Sucursal sucursal = entityManager.find(Sucursal.class, id);
                    if (sucursal != null) {
                        entityManager.remove(sucursal);
                    }
                }
                entityManager.flush();
                for (Integer id : idsCreados) {
                    entityManager.createNativeQuery("DELETE FROM sucu_prec WHERE precio_id = ?1").setParameter(1, id).executeUpdate();
                    Precio precio = entityManager.find(Precio.class, id);
                    if (precio != null) {
                        entityManager.remove(precio);
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
        assertEquals(200, get("api/v1/precios/combo").getStatus());
    }

    @Test
    void buscar_conToken_deberiaRetornarListadoPaginado() {
        assertEquals(200, get("api/v1/precios?pagina=1&tamanio=5&ordenarPor=codigo").getStatus());
    }

    @Test
    void crear_modificar_yEliminar_deberiaCompletarElCiclo() {
        Integer id = registrarParaLimpieza(crear(nuevoDTO(Precio.ALCANCE_NINGUNO)));

        PrecioDTO cambios = nuevoDTO(Precio.ALCANCE_TODOS);
        Response respuestaModificar = target("api/v1/precios/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(cambios, MediaType.APPLICATION_JSON));
        assertEquals(200, respuestaModificar.getStatus());
        PrecioDTO modificado = respuestaModificar.readEntity(new GenericType<RespuestaApi<PrecioDTO>>() { }).datos();
        assertEquals(cambios.descri(), modificado.descri());
        assertEquals(Precio.ALCANCE_NINGUNO, modificado.alcanc());

        assertEquals(200, eliminar(id).getStatus());
        Response respuestaBuscar = get("api/v1/precios/" + id);
        assertFalse(respuestaBuscar.readEntity(new GenericType<RespuestaApi<PrecioDTO>>() { }).datos().estado());
    }

    @Test
    void crear_conCodigoDuplicado_deberiaRetornar409() {
        PrecioDTO primero = nuevoDTO(Precio.ALCANCE_NINGUNO);
        registrarParaLimpieza(crear(primero));

        PrecioDTO segundo = new PrecioDTO(null, primero.codigo().toLowerCase(Locale.ROOT), descriAleatoria(),
                Precio.ALCANCE_NINGUNO, BigDecimal.ONE, true);

        assertEquals(409, crear(segundo).getStatus());
    }

    @Test
    void crear_conFactorCeroOAlcanceInvalido_deberiaRetornar400() {
        String codigo = codigoAleatorio();
        assertEquals(400, crear(new PrecioDTO(null, codigo, descriAleatoria(), 0, BigDecimal.ZERO, true)).getStatus());
        assertEquals(400, crear(new PrecioDTO(null, codigo, descriAleatoria(), 2, BigDecimal.ONE, true)).getStatus());
    }

    @Test
    void crear_conAlcanceTodos_deberiaHabilitarLaListaEnLasSucursalesActivas() {
        Integer sucursalId = crearSucursal();
        assumeTrue(sucursalId != null, "Requiere permisos para crear sucursales");

        Integer precioId = registrarParaLimpieza(crear(nuevoDTO(Precio.ALCANCE_TODOS)));

        List<SucuPrecDTO> precios = preciosDeSucursal(sucursalId);
        assertEquals(1, precios.stream().filter(p -> p.precioId().equals(precioId) && p.acceso()).count());
        // Habilitada en sucursales: no se puede desactivar.
        assertEquals(409, eliminar(precioId).getStatus());
    }

    @Test
    void preciosDeSucursal_deberianReemplazarseLeerseYBloquearLaBaja() {
        Integer precioId = registrarParaLimpieza(crear(nuevoDTO(Precio.ALCANCE_NINGUNO)));
        Integer sucursalId = crearSucursal();
        assumeTrue(sucursalId != null, "Requiere permisos para crear sucursales");
        List<AsignacionAccesoDTO> asignaciones = List.of(new AsignacionAccesoDTO(precioId, true));

        assertEquals(200, reemplazarPrecios(sucursalId, asignaciones).getStatus());
        assertEquals(200, reemplazarPrecios(sucursalId, asignaciones).getStatus());

        List<SucuPrecDTO> precios = preciosDeSucursal(sucursalId);
        assertEquals(1, precios.size());
        assertEquals(precioId, precios.get(0).precioId());

        assertEquals(409, eliminar(precioId).getStatus());

        assertEquals(200, reemplazarPrecios(sucursalId, List.of()).getStatus());
        assertEquals(200, eliminar(precioId).getStatus());
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

    private Response crear(PrecioDTO precioDTO) {
        return target("api/v1/precios")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(precioDTO, MediaType.APPLICATION_JSON));
    }

    private Response eliminar(Integer precioId) {
        return target("api/v1/precios/" + precioId)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
    }

    private List<SucuPrecDTO> preciosDeSucursal(Integer sucursalId) {
        return get("api/v1/sucursales/" + sucursalId + "/precios")
                .readEntity(new GenericType<RespuestaApi<List<SucuPrecDTO>>>() { }).datos();
    }

    private Response reemplazarPrecios(Integer sucursalId, List<AsignacionAccesoDTO> asignaciones) {
        return target("api/v1/sucursales/" + sucursalId + "/precios")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(asignaciones, MediaType.APPLICATION_JSON));
    }

    private Integer registrarParaLimpieza(Response respuestaCrear) {
        assertEquals(201, respuestaCrear.getStatus());
        Integer id = respuestaCrear.readEntity(new GenericType<RespuestaApi<PrecioDTO>>() { }).datos().precioId();
        idsCreados.add(id);
        return id;
    }

    /** Crea una sucursal por API; devuelve {@code null} si el usuario de prueba no tiene permisos. */
    private Integer crearSucursal() {
        long numero = Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000_000L;
        String ruc = String.format(Locale.ROOT, "%013d", numero);
        SucursalDTO sucursalDTO = new SucursalDTO(null, "PR" + UUID.randomUUID().toString().substring(0, 8), false,
                "Sucursal de prueba precios", ruc, "Razón Social de Prueba", null, "Ambato",
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

    private PrecioDTO nuevoDTO(int alcanc) {
        return new PrecioDTO(null, codigoAleatorio(), descriAleatoria(), alcanc, new BigDecimal("12"), true);
    }

    private String codigoAleatorio() {
        return "PR-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String descriAleatoria() {
        return "Precio de prueba " + UUID.randomUUID().toString().substring(0, 8);
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
