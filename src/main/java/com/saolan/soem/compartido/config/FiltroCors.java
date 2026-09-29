package com.saolan.soem.compartido.config;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * CORS restringido unicamente al origen del frontend configurado en
 * {@code soem.cors.origen-permitido} (MicroProfile Config); nunca {@code "*"}
 * en producción, segun AGENTS.md. Responde el preflight {@code OPTIONS}
 * directamente y agrega los encabezados {@code Access-Control-*} a toda
 * respuesta.
 */
@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class FiltroCors implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String METODOS_PERMITIDOS = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String ENCABEZADOS_PERMITIDOS = "Authorization, Content-Type, Accept";

    @Inject
    @ConfigProperty(name = "soem.cors.origen-permitido")
    private String origenPermitido;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (HttpMethod.OPTIONS.equalsIgnoreCase(requestContext.getMethod())) {
            requestContext.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", origenPermitido);
        responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", METODOS_PERMITIDOS);
        responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", ENCABEZADOS_PERMITIDOS);
        responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
    }
}
