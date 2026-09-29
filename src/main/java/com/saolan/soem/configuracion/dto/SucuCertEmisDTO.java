package com.saolan.soem.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Configuracion del certificado de firma electronica de una sucursal.
 * {@code clave} solo se exige al crear (validado en el Service, no aqui): si
 * en una modificacion llega en blanco o nula, se conserva la almacenada.
 */
public record SucuCertEmisDTO(

        @NotNull(message = "El certificado emisor es obligatorio")
        Integer certEmisId,

        @NotBlank(message = "La ruta del certificado es obligatoria")
        @Size(max = 100, message = "La ruta no puede superar los 100 caracteres")
        String ruta,

        @NotBlank(message = "La descripción del certificado es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        @Size(max = 32, message = "La clave no puede superar los 32 caracteres")
        String clave) {
}
