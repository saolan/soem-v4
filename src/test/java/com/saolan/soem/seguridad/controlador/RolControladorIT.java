package com.saolan.soem.seguridad.controlador;

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
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.configuracion.modelo.Sucursal;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;
import com.saolan.soem.seguridad.dto.PermisoDTO;
import com.saolan.soem.seguridad.dto.RolDTO;
import com.saolan.soem.seguridad.modelo.Permiso;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolPerm;
import com.saolan.soem.seguridad.modelo.RolSucu;

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
 * Test de integración end-to-end de {@code RolControlador}: login real
 * (usuario semilla con permisos {@code seguridad-crud:*}) y CRUD + sub-recursos
 * {@code /permisos} y {@code /sucursales} vía HTTP.
 */
@ExtendWith(ArquillianExtension.class)
class RolControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(RolControladorIT.class.getPackage());
        return war;
    }

    @ArquillianResource
    private URL baseUrl;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsRolCreados = new ArrayList<>();
    private final List<Integer> idsPermisoCreados = new ArrayList<>();
    private final List<Integer> idsSucursalCreadas = new ArrayList<>();
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
                eliminarPorRol(RolPerm.class, "rol", idsRolCreados);
                eliminarPorRol(RolSucu.class, "rol", idsRolCreados);
                eliminarPorId(Rol.class, idsRolCreados);
                eliminarPorId(Permiso.class, idsPermisoCreados);
                eliminarPorId(Sucursal.class, idsSucursalCreadas);
            });
        } finally {
            if (cliente != null) {
                cliente.close();
            }
        }
    }

    @Test
    void buscar_conToken_deberiaRetornarPaginado() {
        Response respuesta = target("api/v1/roles")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
    }

    @Test
    void combo_conToken_deberiaRetornar200() {
        Response respuesta = target("api/v1/roles/combo")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
    }

    @Test
    void crear_deberiaRetornar201() {
        Response respuesta = crearRol();

        assertEquals(201, respuesta.getStatus());
        registrarRolParaLimpieza(respuesta);
    }

    @Test
    void modificar_deberiaActualizarDatos() {
        Integer id = registrarRolParaLimpieza(crearRol());
        String descriNueva = "Rol modificado " + UUID.randomUUID();

        Response respuestaModificar = target("api/v1/roles/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(new RolDTO(id, descriNueva, true), MediaType.APPLICATION_JSON));

        assertEquals(200, respuestaModificar.getStatus());
        RespuestaApi<RolDTO> cuerpo = respuestaModificar.readEntity(new GenericType<RespuestaApi<RolDTO>>() { });
        assertEquals(descriNueva, cuerpo.datos().descri());
    }

    @Test
    void eliminar_deberiaMarcarEstadoFalse() {
        Integer id = registrarRolParaLimpieza(crearRol());

        Response respuestaEliminar = target("api/v1/roles/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
        assertEquals(200, respuestaEliminar.getStatus());

        Response respuestaBuscar = target("api/v1/roles/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();
        RespuestaApi<RolDTO> cuerpo = respuestaBuscar.readEntity(new GenericType<RespuestaApi<RolDTO>>() { });
        assertFalse(cuerpo.datos().estado());
    }

    @Test
    void reemplazarPermisos_deberiaAsignarLosPermisosAlRol() {
        Integer idRol = registrarRolParaLimpieza(crearRol());

        String recurso = "recurso-test-" + UUID.randomUUID().toString().substring(0, 8);
        Response respuestaCrearPermiso = target("api/v1/permisos")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(new PermisoDTO(null, "Permiso de prueba", recurso, Accion.BUSCAR, null), MediaType.APPLICATION_JSON));
        assertEquals(201, respuestaCrearPermiso.getStatus());
        Integer idPermiso = respuestaCrearPermiso.readEntity(new GenericType<RespuestaApi<PermisoDTO>>() { }).datos().permisoId();
        idsPermisoCreados.add(idPermiso);

        Response respuestaAsignar = target("api/v1/roles/" + idRol + "/permisos")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(List.of(new AsignacionAccesoDTO(idPermiso, true)), MediaType.APPLICATION_JSON));

        assertEquals(200, respuestaAsignar.getStatus());
    }

    @Test
    void reemplazarSucursales_deberiaAceptarReenviarLasMismasSucursales() {
        Integer idRol = registrarRolParaLimpieza(crearRol());
        List<AsignacionAccesoDTO> asignaciones = List.of(new AsignacionAccesoDTO(crearSucursalDirecta(), true));

        for (int intento = 0; intento < 2; intento++) {
            Response respuesta = target("api/v1/roles/" + idRol + "/sucursales")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .put(Entity.entity(asignaciones, MediaType.APPLICATION_JSON));
            assertEquals(200, respuesta.getStatus());
        }
    }

    @Test
    void reemplazarSucursales_deberiaAsignarLasSucursalesAlRol() {
        Integer idRol = registrarRolParaLimpieza(crearRol());
        Integer idSucursal = crearSucursalDirecta();

        Response respuestaAsignar = target("api/v1/roles/" + idRol + "/sucursales")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(List.of(new AsignacionAccesoDTO(idSucursal, true)), MediaType.APPLICATION_JSON));

        assertEquals(200, respuestaAsignar.getStatus());
    }

    // ---------- helpers ----------

    private Response crearRol() {
        RolDTO dto = new RolDTO(null, "Rol de prueba " + UUID.randomUUID(), true);
        return target("api/v1/roles")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(dto, MediaType.APPLICATION_JSON));
    }

    private Integer registrarRolParaLimpieza(Response respuestaCrear) {
        Integer id = respuestaCrear.readEntity(new GenericType<RespuestaApi<RolDTO>>() { }).datos().rolId();
        idsRolCreados.add(id);
        return id;
    }

    private Integer crearSucursalDirecta() {
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

        ejecutarEnTransaccion(() -> entityManager.persist(sucursal));
        idsSucursalCreadas.add(sucursal.getSucursalId());
        return sucursal.getSucursalId();
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

    private <T> void eliminarPorRol(Class<T> clase, String campoRol, List<Integer> rolIds) {
        if (rolIds.isEmpty()) {
            return;
        }
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(clase);
        Root<T> raiz = query.from(clase);
        query.where(raiz.get(campoRol).get("rolId").in(rolIds));
        for (T entidad : entityManager.createQuery(query).getResultList()) {
            entityManager.remove(entidad);
        }
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
