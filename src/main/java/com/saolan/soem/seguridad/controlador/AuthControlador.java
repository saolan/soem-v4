package com.saolan.soem.seguridad.controlador;

import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.excepcion.ExcepcionNoAutenticado;
import com.saolan.soem.compartido.seguridad.UtilidadClaims;
import com.saolan.soem.seguridad.dto.CambioClaveDTO;
import com.saolan.soem.seguridad.dto.LoginRequestDTO;
import com.saolan.soem.seguridad.dto.SesionActualDTO;
import com.saolan.soem.seguridad.servicio.AutenticacionServicio;
import com.saolan.soem.seguridad.servicio.ResultadoAutenticacion;
import com.saolan.soem.seguridad.servicio.TokenServicio;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * Login, renovacion y cierre de sesion. {@code /login}, {@code /refresh} y
 * {@code /logout} son {@code @PermitAll}: no pasan por el mecanismo MP-JWT
 * estandar del contenedor (el access token puede no existir o estar
 * vencido, que es justamente el caso de uso de {@code /refresh}); estos dos
 * ultimos se autentican leyendo el refresh token de la cookie, no del
 * encabezado {@code Authorization}. {@code /me} y {@code /cambiar-clave} si
 * requieren un access token valido (autenticacion MP-JWT normal del
 * contenedor).
 */
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthControlador {

    private static final String COOKIE_REFRESH_TOKEN = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final String CLAIM_SUCURSAL_ID = "sucursalId";
    private static final String CLAIM_PERMISOS = "permisos";

    @Inject
    private AutenticacionServicio autenticacionServicio;

    @Inject
    private TokenServicio tokenServicio;

    @Inject
    private JsonWebToken jsonWebToken;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequestDTO loginRequestDTO, @HeaderParam("User-Agent") String agenteUsuario) {
        ResultadoAutenticacion resultado = autenticacionServicio.login(loginRequestDTO, null, agenteUsuario);
        return Response.ok(RespuestaApi.exito(resultado.respuesta(), "Inicio de sesión exitoso"))
                .cookie(construirCookieRefreshToken(resultado.refreshTokenCrudo()))
                .build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Response refrescar(@CookieParam(COOKIE_REFRESH_TOKEN) String refreshTokenCrudo,
            @HeaderParam("User-Agent") String agenteUsuario) {
        exigirCookiePresente(refreshTokenCrudo);

        ResultadoAutenticacion resultado = autenticacionServicio.refrescar(refreshTokenCrudo, null, agenteUsuario);
        return Response.ok(RespuestaApi.exito(resultado.respuesta(), "Sesión renovada"))
                .cookie(construirCookieRefreshToken(resultado.refreshTokenCrudo()))
                .build();
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
        Integer personaId = Integer.valueOf(jsonWebToken.getSubject());
        Integer sucursalId = comoEntero(jsonWebToken.getClaim(CLAIM_SUCURSAL_ID));
        List<String> permisos = UtilidadClaims.comoConjuntoDeTexto(jsonWebToken.getClaim(CLAIM_PERMISOS))
                .stream().sorted().toList();

        SesionActualDTO sesionActual = new SesionActualDTO(personaId, jsonWebToken.getName(), sucursalId, permisos);
        return Response.ok(RespuestaApi.exito(sesionActual)).build();
    }

    @POST
    @Path("/cambiar-clave")
    public Response cambiarClave(@Valid CambioClaveDTO cambioClaveDTO) {
        Integer personaId = Integer.valueOf(jsonWebToken.getSubject());
        autenticacionServicio.cambiarClave(personaId, cambioClaveDTO);
        return Response.ok(RespuestaApi.<Void>exito(null, "Clave actualizada correctamente")).build();
    }

    private void exigirCookiePresente(String refreshTokenCrudo) {
        if (refreshTokenCrudo == null || refreshTokenCrudo.isBlank()) {
            throw new ExcepcionNoAutenticado("No se encontró una sesión activa");
        }
    }

    private NewCookie construirCookieRefreshToken(String valor) {
        int maxAgeSegundos = (int) (tokenServicio.duracionRefreshTokenDias() * 24 * 60 * 60);
        return new NewCookie.Builder(COOKIE_REFRESH_TOKEN)
                .value(valor)
                .path(COOKIE_PATH)
                .maxAge(maxAgeSegundos)
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();
    }

    private NewCookie construirCookieVencida() {
        return new NewCookie.Builder(COOKIE_REFRESH_TOKEN)
                .value("")
                .path(COOKIE_PATH)
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();
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
}
