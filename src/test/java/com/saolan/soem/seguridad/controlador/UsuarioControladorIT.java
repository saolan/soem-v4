package com.saolan.soem.seguridad.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;
import com.saolan.soem.seguridad.dto.RolDTO;
import com.saolan.soem.seguridad.dto.UsuarioCreacionDTO;
import com.saolan.soem.seguridad.dto.UsuarioDTO;
import com.saolan.soem.seguridad.modelo.PersUsua;
import com.saolan.soem.seguridad.modelo.Persona;
import com.saolan.soem.seguridad.modelo.Rol;
import com.saolan.soem.seguridad.modelo.RolPersUsua;

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
 * Test de integración end-to-end de {@code UsuarioControlador}: login real
 * (mismo usuario semilla que {@code SucursalControladorIT}, con permisos
 * {@code seguridad-crud:*}) y CRUD completo vía HTTP.
 */
@ExtendWith(ArquillianExtension.class)
class UsuarioControladorIT {

    private static final String USUARIO_PRUEBA = System.getProperty("test.sucursales.usuario", "admin.test");
    private static final String CLAVE_PRUEBA = System.getProperty("test.sucursales.clave", "Prueba$2026");

    @Deployment
    public static Archive<?> crearDespliegue() {
        File archivo = new File("./target/soem.war");
        if (!archivo.exists()) {
            throw new RuntimeException("File " + archivo.getAbsolutePath() + " does not exist.");
        }
        WebArchive war = ShrinkWrap.create(ZipImporter.class, "soem.war").importFrom(archivo).as(WebArchive.class);
        war.addPackage(UsuarioControladorIT.class.getPackage());
        return war;
    }

    @ArquillianResource
    private URL baseUrl;

    @PersistenceContext(unitName = "soemPU")
    private EntityManager entityManager;

    @Resource
    private UserTransaction userTransaction;

    private final List<Integer> idsUsuarioCreados = new ArrayList<>();
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
                eliminarAsignacionesRolPersUsua(idsUsuarioCreados, idsRolCreados);
                for (Integer id : idsUsuarioCreados) {
                    PersUsua usuario = entityManager.find(PersUsua.class, id);
                    if (usuario != null) {
                        entityManager.remove(usuario);
                    }
                    Persona persona = entityManager.find(Persona.class, id);
                    if (persona != null) {
                        entityManager.remove(persona);
                    }
                }
                for (Integer id : idsRolCreados) {
                    Rol rol = entityManager.find(Rol.class, id);
                    if (rol != null) {
                        entityManager.remove(rol);
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
        Response respuesta = target("api/v1/usuarios")
                .queryParam("pagina", 1)
                .queryParam("tamanio", 5)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
    }

    @Test
    void combo_conToken_deberiaRetornar200() {
        Response respuesta = target("api/v1/usuarios/combo")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuesta.getStatus());
    }

    @Test
    void crear_deberiaRetornar201_yQuedarDisponibleParaBuscarPorId() {
        Response respuestaCrear = crearUsuario(usuariAleatorio());
        assertEquals(201, respuestaCrear.getStatus());
        Integer id = registrarUsuarioParaLimpieza(respuestaCrear);

        Response respuestaBuscar = target("api/v1/usuarios/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();

        assertEquals(200, respuestaBuscar.getStatus());
    }

    @Test
    void crear_conUsuariDuplicado_deberiaRetornar409() {
        String usuari = usuariAleatorio();
        Response respuestaPrimera = crearUsuario(usuari);
        assertEquals(201, respuestaPrimera.getStatus());
        registrarUsuarioParaLimpieza(respuestaPrimera);

        Response respuestaSegunda = crearUsuario(usuari);

        assertEquals(409, respuestaSegunda.getStatus());
    }

    @Test
    void modificar_deberiaActualizarDatos() {
        Response respuestaCrear = crearUsuario(usuariAleatorio());
        Integer id = registrarUsuarioParaLimpieza(respuestaCrear);
        String usuariNuevo = usuariAleatorio();

        UsuarioDTO modificacion = new UsuarioDTO(id, "0999999999", "Apellido Modificado", "Nombre", null, null, null,
                null, usuariNuevo, false, null, true);
        Response respuestaModificar = target("api/v1/usuarios/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(modificacion, MediaType.APPLICATION_JSON));

        assertEquals(200, respuestaModificar.getStatus());
        RespuestaApi<UsuarioDTO> cuerpo = respuestaModificar.readEntity(new GenericType<RespuestaApi<UsuarioDTO>>() { });
        assertEquals(usuariNuevo, cuerpo.datos().usuari());
    }

    @Test
    void eliminar_deberiaMarcarEstadoFalse_sinBorrarFisicamente() {
        Response respuestaCrear = crearUsuario(usuariAleatorio());
        Integer id = registrarUsuarioParaLimpieza(respuestaCrear);

        Response respuestaEliminar = target("api/v1/usuarios/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .delete();
        assertEquals(200, respuestaEliminar.getStatus());

        Response respuestaBuscar = target("api/v1/usuarios/" + id)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .get();
        RespuestaApi<UsuarioDTO> cuerpo = respuestaBuscar.readEntity(new GenericType<RespuestaApi<UsuarioDTO>>() { });
        assertFalse(cuerpo.datos().estado());
    }

    @Test
    void reemplazarRoles_deberiaAsignarElRolAlUsuario() {
        Response respuestaCrearUsuario = crearUsuario(usuariAleatorio());
        Integer idUsuario = registrarUsuarioParaLimpieza(respuestaCrearUsuario);

        Response respuestaCrearRol = target("api/v1/roles")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(new RolDTO(null, "Rol de prueba " + UUID.randomUUID(), true), MediaType.APPLICATION_JSON));
        assertEquals(201, respuestaCrearRol.getStatus());
        RespuestaApi<RolDTO> cuerpoRol = respuestaCrearRol.readEntity(new GenericType<RespuestaApi<RolDTO>>() { });
        Integer idRol = cuerpoRol.datos().rolId();
        idsRolCreados.add(idRol);

        Response respuestaAsignar = target("api/v1/usuarios/" + idUsuario + "/roles")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .put(Entity.entity(List.of(idRol), MediaType.APPLICATION_JSON));

        assertEquals(200, respuestaAsignar.getStatus());
    }

    // ---------- helpers ----------

    private Response crearUsuario(String usuari) {
        UsuarioCreacionDTO dto = new UsuarioCreacionDTO("0999999999", "Apellido de Prueba", "Nombre de Prueba",
                null, null, null, null, usuari, "ClaveInicial$123", false, true);
        return target("api/v1/usuarios")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .post(Entity.entity(dto, MediaType.APPLICATION_JSON));
    }

    private Integer registrarUsuarioParaLimpieza(Response respuestaCrear) {
        RespuestaApi<UsuarioDTO> cuerpo = respuestaCrear.readEntity(new GenericType<RespuestaApi<UsuarioDTO>>() { });
        Integer id = cuerpo.datos().personaId();
        idsUsuarioCreados.add(id);
        return id;
    }

    private String usuariAleatorio() {
        return "usuario.it." + UUID.randomUUID().toString().substring(0, 8);
    }

    private void eliminarAsignacionesRolPersUsua(List<Integer> personaIds, List<Integer> rolIds) {
        if (personaIds.isEmpty() && rolIds.isEmpty()) {
            return;
        }
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RolPersUsua> query = criteriaBuilder.createQuery(RolPersUsua.class);
        Root<RolPersUsua> raiz = query.from(RolPersUsua.class);
        query.where(criteriaBuilder.or(
                raiz.get("usuario").get("personaId").in(personaIds.isEmpty() ? List.of(-1) : personaIds),
                raiz.get("rol").get("rolId").in(rolIds.isEmpty() ? List.of(-1) : rolIds)));
        for (RolPersUsua asignacion : entityManager.createQuery(query).getResultList()) {
            entityManager.remove(asignacion);
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
