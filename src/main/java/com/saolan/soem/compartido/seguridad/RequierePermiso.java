package com.saolan.soem.compartido.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * Declara, sobre un recurso o metodo JAX-RS, el permiso puntual requerido
 * para invocarlo: la combinacion {@code recurso:accion}. {@link FiltroAutorizacion}
 * intercepta cualquier endpoint anotado y valida ese permiso contra el JWT
 * autenticado de la solicitud.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequierePermiso {

    /** Nombre del recurso protegido, en kebab-case (p. ej. "sucursales"). */
    String recurso();

    /** Accion de negocio requerida sobre el recurso. */
    Accion accion();
}
