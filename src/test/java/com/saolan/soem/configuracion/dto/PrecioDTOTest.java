package com.saolan.soem.configuracion.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.saolan.soem.compartido.excepcion.ExcepcionDecimalInvalido;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Restricciones de Bean Validation de {@link PrecioDTO} sobre {@code alcanc}
 * y {@code factor} (mismo validador que {@code SucursalDTOTest}) y
 * representacion JSON de {@code factor} como string decimal.
 */
class PrecioDTOTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    @ParameterizedTest
    @ValueSource(strings = { "TODOS", "NINGUNO" })
    void alcanc_deberiaAceptarLosValoresDelEnum(String alcanc) {
        assertTrue(validador.validate(nuevoDTO(alcanc, BigDecimal.ONE)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Todos", "ninguno", "1", "", "TODOS " })
    void alcanc_deberiaRechazarValoresFueraDelEnum(String alcanc) {
        Set<ConstraintViolation<PrecioDTO>> violaciones = validador.validate(nuevoDTO(alcanc, BigDecimal.ONE));

        assertEquals(1, violaciones.size());
        assertEquals("alcanc", violaciones.iterator().next().getPropertyPath().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "0.001", "0.000001", "0.5", "12", "999999.999999" })
    void factor_deberiaAceptarPositivosConDecimales(String factor) {
        assertTrue(validador.validate(nuevoDTO("TODOS", new BigDecimal(factor))).isEmpty());
    }

    @Test
    void estado_deberiaSerOpcional_porqueEsDeSoloLectura() {
        assertTrue(validador.validate(new PrecioDTO(null, "Und", "Unidad", "TODOS", BigDecimal.ONE, null)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "1234567", "1.1234567" })
    void factor_deberiaRechazarCeroNegativosYExcesoDeDigitos(String factor) {
        Set<ConstraintViolation<PrecioDTO>> violaciones = validador.validate(nuevoDTO("TODOS", new BigDecimal(factor)));

        assertEquals("factor", violaciones.iterator().next().getPropertyPath().toString());
    }

    @Test
    void factor_deberiaSerializarseComoStringDecimal() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            String json = jsonb.toJson(nuevoDTO("TODOS", new BigDecimal("12.000000")));

            assertTrue(json.contains("\"factor\":\"12\""), json);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "\"12.5\"", "12.5" })
    void factor_deberiaLeerseComoStringOComoNumero(String factorJson) throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            PrecioDTO dto = jsonb.fromJson("{\"factor\":" + factorJson + "}", PrecioDTO.class);

            assertEquals(new BigDecimal("12.5"), dto.factor());
        }
    }

    @Test
    void factor_noNumerico_deberiaFallarConExcepcionDecimalInvalido() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            JsonbException excepcion = assertThrows(JsonbException.class,
                    () -> jsonb.fromJson("{\"factor\":\"abc\"}", PrecioDTO.class));

            Throwable causa = excepcion;
            while (causa.getCause() != null) {
                causa = causa.getCause();
            }
            assertInstanceOf(ExcepcionDecimalInvalido.class, causa);
        }
    }

    private PrecioDTO nuevoDTO(String alcanc, BigDecimal factor) {
        return new PrecioDTO(null, "Und", "Unidad", alcanc, factor, true);
    }
}
