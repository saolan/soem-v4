package com.saolan.soem.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de un emisor de certificados de firma electronica (atributos del
 * sujeto X.509 del emisor: CN, OU, O, C, OID de politica y OID del atributo
 * organizationIdentifier).
 *
 * @param certEmisId     identificador; ignorado en el cuerpo de POST/PUT
 * @param certOrganiIden OID del atributo X.520 organizationIdentifier (p. ej. {@code 2.5.4.97})
 */
public record CertEmisDTO(

        Integer certEmisId,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        @NotBlank(message = "El nombre común (CN) es obligatorio")
        @Size(max = 100, message = "El nombre común (CN) no puede superar los 100 caracteres")
        String certCn,

        @NotBlank(message = "La unidad organizacional (OU) es obligatoria")
        @Size(max = 100, message = "La unidad organizacional (OU) no puede superar los 100 caracteres")
        String certOu,

        @NotBlank(message = "La organización (O) es obligatoria")
        @Size(max = 100, message = "La organización (O) no puede superar los 100 caracteres")
        String certO,

        @NotBlank(message = "El país (C) es obligatorio")
        @Size(max = 100, message = "El país (C) no puede superar los 100 caracteres")
        String certC,

        @NotBlank(message = "El OID es obligatorio")
        @Size(max = 100, message = "El OID no puede superar los 100 caracteres")
        String certOid,

        @NotBlank(message = "El OID del identificador de organización es obligatorio")
        @Size(max = 24, message = "El OID del identificador de organización no puede superar los 24 caracteres")
        @Pattern(regexp = PATRON_OID, message = "El OID del identificador de organización no tiene un formato válido (ej. 2.5.4.97)")
        String certOrganiIden,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {

    /** OID en notación punto: primer arco 0, 1 o 2 seguido de uno o más arcos numéricos. */
    public static final String PATRON_OID = "^[0-2](\\.\\d+)+$";
}
