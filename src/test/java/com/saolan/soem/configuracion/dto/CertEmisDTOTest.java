package com.saolan.soem.configuracion.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Restricciones de Bean Validation de {@link CertEmisDTO}, en particular el
 * OID del atributo organizationIdentifier ({@code cert_organi_iden}).
 * Se usa {@link ParameterMessageInterpolator} porque los mensajes son
 * literales y asi la prueba no depende de Jakarta EL en el classpath de test.
 */
class CertEmisDTOTest {

    private static final String OID_ORGANIZATION_IDENTIFIER = "2.5.4.97";

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
    void deberiaSerValido_conOidOrganizationIdentifier() {
        assertTrue(validador.validate(certEmis(OID_ORGANIZATION_IDENTIFIER)).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void deberiaRechazarCertOrganiIden_cuandoFalta(String certOrganiIden) {
        assertViolacionSoloEn("certOrganiIden", validador.validate(certEmis(certOrganiIden)));
    }

    @ParameterizedTest
    @ValueSource(strings = { "2.5.4.", "3.5.4.97", "2", "2.5.4.a", "2..5", "organizationIdentifier" })
    void deberiaRechazarCertOrganiIden_cuandoNoEsUnOid(String certOrganiIden) {
        assertViolacionSoloEn("certOrganiIden", validador.validate(certEmis(certOrganiIden)));
    }

    @Test
    void deberiaRechazarCertOrganiIden_cuandoSuperaLos24Caracteres() {
        String oidLargo = "2.5.4.97.1234567890123456"; // 25 caracteres, formato OID valido
        assertViolacionSoloEn("certOrganiIden", validador.validate(certEmis(oidLargo)));
    }

    private void assertViolacionSoloEn(String campo, Set<ConstraintViolation<CertEmisDTO>> violaciones) {
        assertTrue(!violaciones.isEmpty(), "Se esperaba al menos una violacion en " + campo);
        violaciones.forEach(violacion -> assertEquals(campo, violacion.getPropertyPath().toString()));
    }

    private CertEmisDTO certEmis(String certOrganiIden) {
        return new CertEmisDTO(null, "Emisor de prueba", "CN Test", "OU Test", "O Test", "EC", "1.2.3.4",
                certOrganiIden, true);
    }
}
