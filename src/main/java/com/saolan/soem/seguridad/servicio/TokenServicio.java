package com.saolan.soem.seguridad.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.saolan.soem.seguridad.modelo.PersUsua;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Emision del access token (JWT firmado RS256, via smallrye-jwt-build) y
 * generacion/hashing del refresh token crudo. El runtime de firma lo aporta
 * la misma capa de WildFly que valida los tokens via {@code JsonWebToken}
 * (ver comentario de {@code version.smallrye.jwt} en el pom.xml).
 */
@ApplicationScoped
public class TokenServicio {

    private static final String CLAIM_PERMISOS = "permisos";
    private static final String CLAIM_SUCURSAL_ID = "sucursalId";
    private static final int BYTES_REFRESH_TOKEN = 32;

    private final SecureRandom generadorAleatorio = new SecureRandom();

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    private String emisor;

    @ConfigProperty(name = "soem.jwt.access-token.duracion-minutos")
    private long duracionAccessTokenMinutos;

    @ConfigProperty(name = "soem.jwt.refresh-token.duracion-dias")
    private long duracionRefreshTokenDias;

    public long duracionAccessTokenMinutos() {
        return duracionAccessTokenMinutos;
    }

    public long duracionRefreshTokenDias() {
        return duracionRefreshTokenDias;
    }

    /**
     * Firma el access token con los permisos y la sucursal activa ya
     * resueltos por {@code AutenticacionServicio}: se calculan una unica vez
     * al emitir el token y viajan embebidos como claim (ver comentario de
     * diseno en {@code FiltroAutorizacion}).
     */
    public String emitirAccessToken(PersUsua usuario, Set<String> permisos, Integer sucursalId) {
        return Jwt.claims()
                .issuer(emisor)
                .subject(String.valueOf(usuario.getPersonaId()))
                .upn(usuario.getUsuari())
                .claim(CLAIM_PERMISOS, permisos)
                .claim(CLAIM_SUCURSAL_ID, sucursalId)
                .expiresIn(duracionAccessTokenMinutos * 60)
                .sign();
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
