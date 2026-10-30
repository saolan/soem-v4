package com.saolan.soem.configuracion.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Restricciones de Bean Validation de {@link SucursalDTO}. Se usa
 * {@link ParameterMessageInterpolator} porque los mensajes son literales y
 * asi la prueba no depende de una implementacion de Jakarta EL en el
 * classpath de test.
 */
class SucursalDTOTest {

    private static final String RUC_VALIDO = "1790012345001";

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

    @Test
    void deberiaSerValido_conRucYRucContableDe13Digitos() {
        assertTrue(validador.validate(sucursal(RUC_VALIDO, RUC_VALIDO, true)).isEmpty());
    }

    @Test
    void deberiaSerValido_sinEstado() {
        assertTrue(validador.validate(sucursal(RUC_VALIDO, RUC_VALIDO, null)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { "1790012345", "179001234500", "17900123450011", "179001234500A" })
    void deberiaRechazarRuc_cuandoNoTiene13Digitos(String ruc) {
        assertUnicaViolacionEn("ruc", validador.validate(sucursal(ruc, RUC_VALIDO, true)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1790012345", "179001234500", "17900123450011", "179001234500A" })
    void deberiaRechazarRucContable_cuandoNoTiene13Digitos(String rucCont) {
        assertUnicaViolacionEn("rucCont", validador.validate(sucursal(RUC_VALIDO, rucCont, true)));
    }

    private void assertUnicaViolacionEn(String campo, Set<ConstraintViolation<SucursalDTO>> violaciones) {
        assertEquals(1, violaciones.size());
        assertEquals(campo, violaciones.iterator().next().getPropertyPath().toString());
    }

    private SucursalDTO sucursal(String ruc, String rucCont, Boolean estado) {
        return new SucursalDTO(null, "S001", false, "Sucursal Test", ruc, "Razón Social Test",
                null, "Quito", "Av. de Prueba 123", "Av. de Prueba 123", "0999999999", null,
                "correo@test.com", "Representante Test", "0999999999", "Dirección Representante Test",
                rucCont, null, null, null, estado);
    }
}
