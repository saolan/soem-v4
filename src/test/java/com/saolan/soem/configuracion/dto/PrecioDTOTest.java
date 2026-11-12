package com.saolan.soem.configuracion.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Restricciones de Bean Validation de {@link PrecioDTO} sobre {@code alcanc}
 * y {@code factor}. Mismo validador que {@code SucursalDTOTest}.
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
    @ValueSource(strings = { "0", "-1", "1234567", "1.1234567" })
    void factor_deberiaRechazarCeroNegativosYExcesoDeDigitos(String factor) {
        Set<ConstraintViolation<PrecioDTO>> violaciones = validador.validate(nuevoDTO("TODOS", new BigDecimal(factor)));

        assertEquals("factor", violaciones.iterator().next().getPropertyPath().toString());
    }

    private PrecioDTO nuevoDTO(String alcanc, BigDecimal factor) {
        return new PrecioDTO(null, "Und", "Unidad", alcanc, factor, true);
    }
}
