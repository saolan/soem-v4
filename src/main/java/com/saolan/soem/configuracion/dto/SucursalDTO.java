package com.saolan.soem.configuracion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de una sucursal. La configuracion del certificado de firma
 * electronica ({@link SucuCertEmisDTO}) no viaja aqui: se administra como
 * sub-recurso independiente en {@code /api/v1/sucursales/{id}/certificado-emision},
 * porque el frontend lo carga en una pantalla separada, en un momento
 * distinto al alta/edicion de los datos generales de la sucursal.
 *
 * @param sucursalId identificador; ignorado en el cuerpo de POST/PUT
 */
public record SucursalDTO(

        Integer sucursalId,

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 32, message = "El código no puede superar los 32 caracteres")
        String codigo,

        @NotNull(message = "Debe indicar si la sucursal es matriz")
        Boolean matriz,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "^[0-9]{10,13}$", message = "El RUC debe tener entre 10 y 13 dígitos numéricos")
        String ruc,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 256, message = "La razón social no puede superar los 256 caracteres")
        String razonSoci,

        @Size(max = 256, message = "El nombre comercial no puede superar los 256 caracteres")
        String nombreCome,

        @NotBlank(message = "La ciudad es obligatoria")
        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        String ciudad,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 100, message = "La dirección no puede superar los 100 caracteres")
        String direcc,

        @NotBlank(message = "La dirección del establecimiento es obligatoria")
        @Size(max = 100, message = "La dirección del establecimiento no puede superar los 100 caracteres")
        String direccEsta,

        @NotBlank(message = "El teléfono principal es obligatorio")
        @Size(max = 10, message = "El teléfono principal no puede superar los 10 caracteres")
        String telef1,

        @Size(max = 10, message = "El teléfono secundario no puede superar los 10 caracteres")
        String telef2,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
        String correo,

        @NotBlank(message = "El representante legal es obligatorio")
        @Size(max = 100, message = "El representante legal no puede superar los 100 caracteres")
        String repres,

        @NotBlank(message = "La cédula del representante legal es obligatoria")
        @Size(max = 13, message = "La cédula del representante legal no puede superar los 13 caracteres")
        String represCedu,

        @NotBlank(message = "La dirección del representante legal es obligatoria")
        @Size(max = 100, message = "La dirección del representante legal no puede superar los 100 caracteres")
        String represDire,

        @NotBlank(message = "El RUC contable es obligatorio")
        @Size(max = 13, message = "El RUC contable no puede superar los 13 caracteres")
        String rucCont,

        @Size(max = 13, message = "El contribuyente especial no puede superar los 13 caracteres")
        String contriEspe,

        Boolean obligaCont,

        @Size(max = 255, message = "La ruta del logotipo no puede superar los 255 caracteres")
        String logoti,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
