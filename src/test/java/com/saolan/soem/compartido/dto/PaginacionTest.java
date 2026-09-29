package com.saolan.soem.compartido.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link Paginacion#de(int, int, long)}: el cálculo de
 * {@code totalPaginas} es la única lógica no trivial del record.
 */
class PaginacionTest {

    @Test
    void de_deberiaRedondearHaciaArriba_cuandoNoEsDivisionExacta() {
        Paginacion paginacion = Paginacion.de(1, 20, 45);

        assertEquals(3, paginacion.totalPaginas());
    }

    @Test
    void de_deberiaCalcularExacto_cuandoLaDivisionEsExacta() {
        Paginacion paginacion = Paginacion.de(1, 20, 20);

        assertEquals(1, paginacion.totalPaginas());
    }

    @Test
    void de_deberiaSerCero_cuandoNoHayRegistros() {
        Paginacion paginacion = Paginacion.de(1, 20, 0);

        assertEquals(0, paginacion.totalPaginas());
    }

    @Test
    void de_deberiaSerCero_cuandoElTamanioEsCeroOMenor() {
        assertEquals(0, Paginacion.de(1, 0, 100).totalPaginas());
        assertEquals(0, Paginacion.de(1, -5, 100).totalPaginas());
    }

    @Test
    void de_deberiaConservarPaginaTamanioYTotalRegistros() {
        Paginacion paginacion = Paginacion.de(2, 20, 45);

        assertEquals(2, paginacion.pagina());
        assertEquals(20, paginacion.tamanio());
        assertEquals(45, paginacion.totalRegistros());
    }
}
