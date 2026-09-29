package com.saolan.soem.compartido.seguridad;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.JsonArray;
import jakarta.json.JsonString;

/**
 * Conversion de un claim JWT de tipo arreglo a {@code Set<String>}. El
 * contenedor puede devolverlo como {@link JsonArray} (Jakarta JSON-P) o como
 * {@link Collection} generica segun el proveedor MP-JWT; se soportan ambos
 * casos. Compartida entre {@link FiltroAutorizacion} (valida el permiso
 * requerido) y el endpoint {@code /auth/me} (expone la sesion actual), para
 * no duplicar la logica de extraccion.
 */
public final class UtilidadClaims {

    private UtilidadClaims() {
    }

    @SuppressWarnings("unchecked")
    public static Set<String> comoConjuntoDeTexto(Object valorClaim) {
        if (valorClaim instanceof JsonArray arregloJson) {
            return arregloJson.stream()
                    .filter(JsonString.class::isInstance)
                    .map(valor -> ((JsonString) valor).getString())
                    .collect(Collectors.toSet());
        }
        if (valorClaim instanceof Collection<?> coleccion) {
            return ((Collection<Object>) coleccion).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }
}
