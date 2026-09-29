package com.saolan.soem.compartido.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonArray;

/**
 * Tests unitarios de {@link UtilidadClaims#comoConjuntoDeTexto(Object)}: el
 * claim puede llegar como {@link JsonArray} (Jakarta JSON-P, el caso real en
 * producción) o como {@link java.util.Collection} genérica según el
 * proveedor MP-JWT.
 */
class UtilidadClaimsTest {

    @Test
    void comoConjuntoDeTexto_deberiaExtraerStrings_deUnJsonArray() {
        JsonArray arreglo = Json.createArrayBuilder().add("sucursales:buscar").add("sucursales:crear").build();

        Set<String> resultado = UtilidadClaims.comoConjuntoDeTexto(arreglo);

        assertEquals(Set.of("sucursales:buscar", "sucursales:crear"), resultado);
    }

    @Test
    void comoConjuntoDeTexto_deberiaExtraerStrings_deUnaColeccion() {
        Set<String> resultado = UtilidadClaims.comoConjuntoDeTexto(List.of("roles:buscar", "roles:crear"));

        assertEquals(Set.of("roles:buscar", "roles:crear"), resultado);
    }

    @Test
    void comoConjuntoDeTexto_deberiaSerVacio_cuandoElValorEsNulo() {
        assertTrue(UtilidadClaims.comoConjuntoDeTexto(null).isEmpty());
    }

    @Test
    void comoConjuntoDeTexto_deberiaSerVacio_cuandoElTipoNoEsSoportado() {
        assertTrue(UtilidadClaims.comoConjuntoDeTexto("permisos:no-es-un-arreglo").isEmpty());
        assertTrue(UtilidadClaims.comoConjuntoDeTexto(42).isEmpty());
    }

    @Test
    void comoConjuntoDeTexto_deberiaSerVacio_cuandoElJsonArrayEstaVacio() {
        assertTrue(UtilidadClaims.comoConjuntoDeTexto(Json.createArrayBuilder().build()).isEmpty());
    }
}
