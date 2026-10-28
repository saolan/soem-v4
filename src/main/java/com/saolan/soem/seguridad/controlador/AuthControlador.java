package com.saolan.soem.seguridad.controlador;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.excepcion.ExcepcionNoAutenticado;
import com.saolan.soem.compartido.seguridad.UtilidadClaims;
import com.saolan.soem.seguridad.dto.CambioClaveDTO;
import com.saolan.soem.seguridad.dto.CambioSucursalDTO;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.MenuNodoDTO;
import com.saolan.soem.seguridad.dto.SesionActualDTO;
import com.saolan.soem.seguridad.servicio.AutenticacionServicio;
import com.saolan.soem.seguridad.servicio.MenuServicio;
import com.saolan.soem.seguridad.servicio.ResultadoAutenticacion;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * Login, renovacion, cambio de sucursal, cambio de clave y cierre de sesion.
 * {@code /login}, {@code /refresh} y {@code /logout} son {@code @PermitAll}:
 * no pasan por el mecanismo MP-JWT estandar del contenedor (el access token
 * puede no existir o estar vencido, que es justamente el caso de uso de
 * {@code /refresh}); estos dos ultimos se autentican leyendo el refresh
 * token de la cookie, no del encabezado {@code Authorization}.
 * {@code /me}, {@code /menu}, {@code /cambiar-clave} y {@code /cambiar-sucursal} exigen un
 * access token valido (ver {@link #personaIdAutenticada()}).
 */
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthControlador {

    private static final String COOKIE_REFRESH_TOKEN = "refreshToken";
    /** Ruta de los endpoints de auth relativa al contexto de despliegue (p. ej. /soem). */
    private static final String RUTA_AUTH = "/api/v1/auth";
    private static final String CLAIM_SUCURSAL_ID = "sucursalId";
    private static final String CLAIM_PERMISOS = "permisos";
    private static final String CLAIM_CLAVE_CADUCADA = "claveCaducada";
    private static final String ENCABEZADO_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final int LONGITUD_MAXIMA_IP = 45;
    private static final int LONGITUD_MAXIMA_AGENTE = 255;

    @Inject
    private AutenticacionServicio autenticacionServicio;

    @Inject
    private MenuServicio menuServicio;

    @Inject
    private JsonWebToken jsonWebToken;

    @Inject
    @ConfigProperty(name = "soem.seguridad.confiar-x-forwarded-for", defaultValue = "false")
    private boolean confiarXForwardedFor;

    @Context
    private HttpServletRequest httpServletRequest;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequestDTO loginRequestDTO, @HeaderParam("User-Agent") String agenteUsuario) {
        ResultadoAutenticacion resultado = autenticacionServicio.login(loginRequestDTO, ipOrigen(), recortar(agenteUsuario));
        return respuestaConSesion(resultado, "Inicio de sesión exitoso");
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Response refrescar(@CookieParam(COOKIE_REFRESH_TOKEN) String refreshTokenCrudo,
            @HeaderParam("User-Agent") String agenteUsuario) {
        exigirCookiePresente(refreshTokenCrudo);

        ResultadoAutenticacion resultado = autenticacionServicio.refrescar(refreshTokenCrudo, ipOrigen(), recortar(agenteUsuario));
        return respuestaConSesion(resultado, "Sesión renovada");
    }

    /**
     * Cambia la sucursal activa. Requiere el access token (identifica a la
     * persona) y la cookie del refresh token (se rota para conservar la
     * sucursal elegida en las siguientes renovaciones).
     */
    @POST
    @Path("/cambiar-sucursal")
    public Response cambiarSucursal(@Valid CambioSucursalDTO cambioSucursalDTO,
            @CookieParam(COOKIE_REFRESH_TOKEN) String refreshTokenCrudo,
            @HeaderParam("User-Agent") String agenteUsuario) {
        Integer personaId = personaIdAutenticada();
        exigirCookiePresente(refreshTokenCrudo);

        ResultadoAutenticacion resultado = autenticacionServicio.cambiarSucursal(personaId, refreshTokenCrudo,
                cambioSucursalDTO.sucursalId(), ipOrigen(), recortar(agenteUsuario));
        return respuestaConSesion(resultado, "Sucursal cambiada correctamente");
    }

    @POST
    @Path("/logout")
    @PermitAll
    public Response logout(@CookieParam(COOKIE_REFRESH_TOKEN) String refreshTokenCrudo) {
        if (refreshTokenCrudo != null && !refreshTokenCrudo.isBlank()) {
            autenticacionServicio.logout(refreshTokenCrudo);
        }
        return Response.ok(RespuestaApi.<Void>exito(null, "Sesión cerrada"))
                .cookie(construirCookieVencida())
                .build();
    }

    @GET
    @Path("/me")
    public Response me() {
        Integer personaId = personaIdAutenticada();
        Integer sucursalId = comoEntero(jsonWebToken.getClaim(CLAIM_SUCURSAL_ID));
        List<String> permisos = UtilidadClaims.comoConjuntoDeTexto(jsonWebToken.getClaim(CLAIM_PERMISOS))
                .stream().sorted().toList();
        boolean claveCaducada = comoBooleano(jsonWebToken.getClaim(CLAIM_CLAVE_CADUCADA));

        SesionActualDTO sesionActual = new SesionActualDTO(personaId, jsonWebToken.getName(), sucursalId, permisos,
                claveCaducada);
        return Response.ok(RespuestaApi.exito(sesionActual)).build();
    }

    /**
     * Menu lateral de la sesion para la sucursal activa del token. El
     * frontend lo pide tras {@code /login}, {@code /refresh} y
     * {@code /cambiar-sucursal}: como el token nuevo trae la sucursal
     * elegida, el menu se recarga sin estado adicional en el servidor. Con
     * la clave caducada el menu es vacio (el token no tiene permisos).
     */
    @GET
    @Path("/menu")
    public Response menu() {
        Integer personaId = personaIdAutenticada();
        List<MenuNodoDTO> menu = comoBooleano(jsonWebToken.getClaim(CLAIM_CLAVE_CADUCADA))
                ? List.of()
                : menuServicio.menuDeSesion(personaId, comoEntero(jsonWebToken.getClaim(CLAIM_SUCURSAL_ID)));
        return Response.ok(RespuestaApi.exito(menu))
                .cacheControl(sinCache())
                .build();
    }

    /**
     * Cambia la clave y cierra las demas sesiones. Si llega la cookie de la
     * sesion actual, responde con una sesion nueva (token con permisos
     * completos aunque la clave estuviera caducada); si no, la sesion queda
     * cerrada y el frontend debe volver a iniciar sesion.
     */
    @POST
    @Path("/cambiar-clave")
    public Response cambiarClave(@Valid CambioClaveDTO cambioClaveDTO,
            @CookieParam(COOKIE_REFRESH_TOKEN) String refreshTokenCrudo,
            @HeaderParam("User-Agent") String agenteUsuario) {
        Integer personaId = personaIdAutenticada();
        Optional<ResultadoAutenticacion> resultado = autenticacionServicio.cambiarClave(personaId, cambioClaveDTO,
                refreshTokenCrudo, ipOrigen(), recortar(agenteUsuario));

        if (resultado.isPresent()) {
            return respuestaConSesion(resultado.get(), "Clave actualizada correctamente");
        }
        return Response.ok(RespuestaApi.<Void>exito(null, "Clave actualizada correctamente, inicie sesión nuevamente"))
                .cookie(construirCookieVencida())
                .build();
    }

    /**
     * Estos endpoints no usan {@code @RolesAllowed}, por lo que el contenedor
     * no rechaza solicitudes sin token: se valida aqui para responder 401 en
     * lugar de fallar al leer el {@code sub}.
     */
    private Integer personaIdAutenticada() {
        if (jsonWebToken == null || jsonWebToken.getRawToken() == null || jsonWebToken.getSubject() == null) {
            throw new ExcepcionNoAutenticado("Debe autenticarse para acceder a este recurso");
        }
        try {
            return Integer.valueOf(jsonWebToken.getSubject());
        } catch (NumberFormatException excepcion) {
            throw new ExcepcionNoAutenticado("Debe autenticarse para acceder a este recurso");
        }
    }

    private Response respuestaConSesion(ResultadoAutenticacion resultado, String mensaje) {
        return Response.ok(RespuestaApi.exito(resultado.respuesta(), mensaje))
                .cookie(construirCookieRefreshToken(resultado.refreshTokenCrudo()))
                .build();
    }

    private void exigirCookiePresente(String refreshTokenCrudo) {
        if (refreshTokenCrudo == null || refreshTokenCrudo.isBlank()) {
            throw new ExcepcionNoAutenticado("No se encontró una sesión activa");
        }
    }

    /**
     * IP del cliente para auditoria de sesiones. {@code X-Forwarded-For}
     * solo se considera si se configuro explicitamente un proxy confiable
     * ({@code soem.seguridad.confiar-x-forwarded-for}).
     */
    private String ipOrigen() {
        String ip = null;
        if (confiarXForwardedFor) {
            String reenviado = httpServletRequest.getHeader(ENCABEZADO_X_FORWARDED_FOR);
            if (reenviado != null && !reenviado.isBlank()) {
                ip = reenviado.split(",")[0].trim();
            }
        }
        if (ip == null) {
            ip = httpServletRequest.getRemoteAddr();
        }
        return ip == null ? null : ip.substring(0, Math.min(ip.length(), LONGITUD_MAXIMA_IP));
    }

    private String recortar(String agenteUsuario) {
        if (agenteUsuario == null) {
            return null;
        }
        return agenteUsuario.substring(0, Math.min(agenteUsuario.length(), LONGITUD_MAXIMA_AGENTE));
    }

    /**
     * La cookie solo debe viajar a los endpoints de auth. Incluye el contexto
     * de despliegue ({@code /soem}): con una ruta fija sin el contexto, el
     * navegador nunca la enviaria a {@code /soem/api/v1/auth/refresh}.
     */
    private String rutaCookie() {
        return httpServletRequest.getContextPath() + RUTA_AUTH;
    }

    /**
     * Cookie de sesion (sin {@code Max-Age} ni {@code Expires}): el navegador
     * la descarta al cerrarse, de modo que al reabrirlo se exige de nuevo
     * usuario y clave. Con una cookie persistente, el {@code /refresh} que el
     * frontend ejecuta al arrancar restauraba la sesion sin credenciales. La
     * vigencia maxima del refresh token se sigue controlando en el servidor
     * ({@code fecha_hora_expira}).
     */
    private NewCookie construirCookieRefreshToken(String valor) {
        return new NewCookie.Builder(COOKIE_REFRESH_TOKEN)
                .value(valor)
                .path(rutaCookie())
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();
    }

    private NewCookie construirCookieVencida() {
        return new NewCookie.Builder(COOKIE_REFRESH_TOKEN)
                .value("")
                .path(rutaCookie())
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();
    }

    /** El menu depende del usuario y de la sucursal: ningun cache debe guardarlo. */
    private CacheControl sinCache() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoStore(true);
        cacheControl.setNoCache(true);
        cacheControl.setPrivate(true);
        return cacheControl;
    }

    private Integer comoEntero(Object valorClaim) {
        if (valorClaim instanceof Number numero) {
            return numero.intValue();
        }
        if (valorClaim instanceof JsonNumber jsonNumero) {
            return jsonNumero.intValue();
        }
        return null;
    }

    private boolean comoBooleano(Object valorClaim) {
        if (valorClaim instanceof Boolean booleano) {
            return booleano;
        }
        return valorClaim instanceof JsonValue jsonValor && jsonValor.getValueType() == JsonValue.ValueType.TRUE;
    }
}
