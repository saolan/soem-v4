package com.saolan.soem.seguridad.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.saolan.soem.seguridad.modelo.PersUsua;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Emision del access token (JWT firmado RS256, via smallrye-jwt-build) y
 * generacion/hashing del refresh token crudo. El runtime de firma lo aporta
 * la misma capa de WildFly que valida los tokens via {@code JsonWebToken}
 * (ver comentario de {@code version.smallrye.jwt} en el pom.xml).
 */
@ApplicationScoped
public class TokenServicio {

    private static final Logger LOGGER = Logger.getLogger(TokenServicio.class.getName());

    private static final String CLAIM_PERMISOS = "permisos";
    private static final String CLAIM_SUCURSAL_ID = "sucursalId";
    private static final String CLAIM_CLAVE_CADUCADA = "claveCaducada";
    private static final int BYTES_REFRESH_TOKEN = 32;

    private final SecureRandom generadorAleatorio = new SecureRandom();

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    private String emisor;

    @Inject
    @ConfigProperty(name = "soem.jwt.access-token.duracion-minutos")
    private long duracionAccessTokenMinutos;

    /**
     * Vencimiento deslizante del refresh token: cada emision o rotacion lo
     * fija en {@code ahora + inactividad}, de modo que la sesion caduca
     * cuando el usuario deja de usar el sistema ese tiempo.
     */
    @Inject
    @ConfigProperty(name = "soem.jwt.refresh-token.inactividad-minutos")
    private long inactividadRefreshTokenMinutos;

    /** Segundos sin uso tras los que el frontend cierra la sesion (se informa en {@code LoginRespuestaDTO}). */
    @Inject
    @ConfigProperty(name = "soem.jwt.sesion.inactividad-segundos")
    private long inactividadSesionSegundos;

    /**
     * Falla el despliegue ante una configuracion incoherente en lugar de
     * emitir sesiones que vencen al instante.
     */
    @PostConstruct
    void validarConfiguracion() {
        if (duracionAccessTokenMinutos <= 0 || inactividadRefreshTokenMinutos <= 0 || inactividadSesionSegundos <= 0) {
            throw new IllegalStateException(
                    "La duración del access token y los tiempos de inactividad de la sesión deben ser positivos");
        }
        if (inactividadRefreshTokenMinutos * 60 <= inactividadSesionSegundos) {
            // El servidor debe vencer despues del frontend para que el usuario vea siempre el aviso previo.
            LOGGER.log(Level.WARNING,
                    "La inactividad del refresh token ({0} min) no supera la inactividad del frontend ({1} s)",
                    new Object[] { inactividadRefreshTokenMinutos, inactividadSesionSegundos });
        }
    }

    public long duracionAccessTokenMinutos() {
        return duracionAccessTokenMinutos;
    }

    public long inactividadRefreshTokenMinutos() {
        return inactividadRefreshTokenMinutos;
    }

    public long inactividadSesionSegundos() {
        return inactividadSesionSegundos;
    }

    /**
     * Firma el access token con los permisos y la sucursal activa ya
     * resueltos por {@code AutenticacionServicio}: se calculan una unica vez
     * al emitir el token y viajan embebidos como claim (ver comentario de
     * diseno en {@code FiltroAutorizacion}).
     * <p>
     * {@code sucursalId} nulo (seleccion de sucursal pendiente): el claim se
     * omite en lugar de emitirse con valor nulo.
     */
    public String emitirAccessToken(PersUsua usuario, Set<String> permisos, Integer sucursalId, boolean claveCaducada) {
        JwtClaimsBuilder claims = Jwt.claims()
                .issuer(emisor)
                .subject(String.valueOf(usuario.getPersonaId()))
                .upn(usuario.getUsuari())
                .claim(CLAIM_PERMISOS, permisos)
                .claim(CLAIM_CLAVE_CADUCADA, claveCaducada);
        if (sucursalId != null) {
            claims.claim(CLAIM_SUCURSAL_ID, sucursalId);
        }
        return claims.expiresIn(duracionAccessTokenMinutos * 60).sign();
    }

    /** 256 bits aleatorios, Base64 URL-safe: el valor que recibe el cliente en la cookie. */
    public String generarRefreshTokenCrudo() {
        byte[] valor = new byte[BYTES_REFRESH_TOKEN];
        generadorAleatorio.nextBytes(valor);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(valor);
    }

    /** SHA-256 del token crudo: es lo unico que se persiste en {@code refresh_token.token_hash}. */
    public String hashearRefreshToken(String tokenCrudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenCrudo.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalStateException("El algoritmo SHA-256 no está disponible en esta JVM", excepcion);
        }
    }
}
