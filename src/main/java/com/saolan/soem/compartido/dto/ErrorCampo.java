package com.saolan.soem.compartido.dto;

/**
 * Detalle de un error de validacion asociado a un campo especifico del
 * request. Forma parte de la lista {@code errores} del contrato
 * {@link RespuestaApi}.
 *
 * @param campo   nombre del campo afectado (nombre del atributo del DTO)
 * @param detalle descripcion del error en espanol, apta para mostrar al usuario
 */
public record ErrorCampo(String campo, String detalle) {
}
