package com.saolan.soem.compartido.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link ParametrosBusqueda}: la normalización ocurre en
 * el constructor canónico del record, sin dependencias externas.
 */
class ParametrosBusquedaTest {

    @Test
    void pagina_deberiaNormalizarseA1_cuandoEsMenorA1() {
        assertEquals(1, new ParametrosBusqueda(0, 20, "descri", "asc").pagina());
        assertEquals(1, new ParametrosBusqueda(-5, 20, "descri", "asc").pagina());
    }

    @Test
    void pagina_deberiaConservarse_cuandoEsValida() {
        assertEquals(3, new ParametrosBusqueda(3, 20, "descri", "asc").pagina());
    }

    @Test
    void tamanio_deberiaUsarPredeterminado_cuandoEsMenorAlMinimo() {
        assertEquals(ParametrosBusqueda.TAMANIO_PREDETERMINADO,
                new ParametrosBusqueda(1, 0, "descri", "asc").tamanio());
    }

    @Test
    void tamanio_deberiaLimitarseAlMaximo_cuandoLoSupera() {
        assertEquals(ParametrosBusqueda.TAMANIO_MAXIMO,
                new ParametrosBusqueda(1, 500, "descri", "asc").tamanio());
    }

    @Test
    void tamanio_deberiaConservarse_cuandoEsValido() {
        assertEquals(50, new ParametrosBusqueda(1, 50, "descri", "asc").tamanio());
    }

    @Test
    void direccion_deberiaSerAsc_cuandoEsInvalida() {
        assertEquals("asc", new ParametrosBusqueda(1, 20, "descri", "").direccion());
        assertEquals("asc", new ParametrosBusqueda(1, 20, "descri", "invalida").direccion());
        assertEquals("asc", new ParametrosBusqueda(1, 20, "descri", null).direccion());
    }

    @Test
    void direccion_deberiaSerDesc_sinImportarMayusculas() {
        assertEquals("desc", new ParametrosBusqueda(1, 20, "descri", "desc").direccion());
        assertEquals("desc", new ParametrosBusqueda(1, 20, "descri", "DESC").direccion());
        assertEquals("desc", new ParametrosBusqueda(1, 20, "descri", "Desc").direccion());
    }

    @Test
    void esDescendente_deberiaReflejarLaDireccionNormalizada() {
        assertTrue(new ParametrosBusqueda(1, 20, "descri", "desc").esDescendente());
        assertFalse(new ParametrosBusqueda(1, 20, "descri", "asc").esDescendente());
    }

    @Test
    void primerRegistro_deberiaCalcularseSegunPaginaYTamanio() {
        assertEquals(0, new ParametrosBusqueda(1, 20, "descri", "asc").primerRegistro());
        assertEquals(20, new ParametrosBusqueda(2, 20, "descri", "asc").primerRegistro());
        assertEquals(100, new ParametrosBusqueda(6, 20, "descri", "asc").primerRegistro());
    }
}
