package com.saolan.soem.compartido.config;

import org.eclipse.microprofile.auth.LoginConfig;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Punto de entrada JAX-RS de toda la API y habilitacion del mecanismo de
 * autenticacion MicroProfile JWT (Bearer) segun AGENTS.md. La emision real
 * de tokens -y la clave publica de verificacion, configurada via
 * {@code mp.jwt.verify.*} en {@code META-INF/microprofile-config.properties}-
 * es responsabilidad del modulo {@code com.saolan.soem.seguridad}
 * ({@code AuthControlador}/{@code TokenServicio}).
 */
@ApplicationPath("/api/v1")
@LoginConfig(authMethod = "MP-JWT", realmName = "soem")
public class AplicacionRest extends Application {
}
