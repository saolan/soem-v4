package com.saolan.soem.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de un emisor de certificados de firma electronica (atributos del
 * sujeto X.509 del emisor: CN, OU, O, C y OID de politica).
 *
 * @param certEmisId identificador; ignorado en el cuerpo de POST/PUT
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

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
