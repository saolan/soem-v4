package com.saolan.soem.compartido.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.saolan.soem.compartido.excepcion.ExcepcionDecimalInvalido;

/** Conversion BigDecimal &harr; string decimal de {@link DecimalTextoAdaptador}. */
class DecimalTextoAdaptadorTest {

    private final DecimalTextoAdaptador adaptador = new DecimalTextoAdaptador();

    @ParameterizedTest
    @CsvSource({ "12.000000, 12", "0.5, 0.5", "1E+1, 10", "0.000000, 0", "-3.250, -3.25", "0.001, 0.001" })
    void adaptToJson_deberiaQuitarCerosYEvitarNotacionCientifica(String valor, String esperado) {
        assertEquals(esperado, adaptador.adaptToJson(new BigDecimal(valor)));
    }

    @Test
    void adaptToJson_deberiaConservarNull() {
        assertNull(adaptador.adaptToJson(null));
    }

    @ParameterizedTest
    @CsvSource({ "12.5, 12.5", "' 7 ', 7", "0.001, 0.001" })
    void adaptFromJson_deberiaConvertirElTexto(String valor, String esperado) {
        assertEquals(new BigDecimal(esperado), adaptador.adaptFromJson(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void adaptFromJson_deberiaRetornarNull_paraTextoVacio(String valor) {
        assertNull(adaptador.adaptFromJson(valor));
    }

    @Test
    void adaptFromJson_deberiaRetornarNull_paraNull() {
        assertNull(adaptador.adaptFromJson(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "12,5", "1.2.3", "NaN" })
    void adaptFromJson_deberiaRechazarTextoNoNumerico(String valor) {
        assertThrows(ExcepcionDecimalInvalido.class, () -> adaptador.adaptFromJson(valor));
    }
}
