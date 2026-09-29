package com.saolan.soem.compartido.dto;

import java.util.List;

/**
 * Envoltorio JSON estandar de todas las respuestas de la API, segun el
 * contrato definido en AGENTS.md.
 *
 * @param exito      indica si la operacion se realizo correctamente
 * @param mensaje    mensaje descriptivo en espanol para el usuario
 * @param datos      contenido de la respuesta (puede ser {@code null})
 * @param errores    lista de errores de validacion; vacia si no hubo fallos
 * @param paginacion metadatos de paginacion; presente solo en listados paginados
 */
public record RespuestaApi<T>(boolean exito, String mensaje, T datos, List<ErrorCampo> errores, Paginacion paginacion) {

    private static final String MENSAJE_EXITO_PREDETERMINADO = "Operación realizada correctamente";

    public static <T> RespuestaApi<T> exito(T datos) {
        return new RespuestaApi<>(true, MENSAJE_EXITO_PREDETERMINADO, datos, List.of(), null);
    }

    public static <T> RespuestaApi<T> exito(T datos, String mensaje) {
        return new RespuestaApi<>(true, mensaje, datos, List.of(), null);
    }

    public static <T> RespuestaApi<T> exitoPaginado(T datos, Paginacion paginacion) {
        return new RespuestaApi<>(true, MENSAJE_EXITO_PREDETERMINADO, datos, List.of(), paginacion);
    }

    public static <T> RespuestaApi<T> error(String mensaje) {
        return new RespuestaApi<>(false, mensaje, null, List.of(), null);
    }

    public static <T> RespuestaApi<T> error(String mensaje, List<ErrorCampo> errores) {
        return new RespuestaApi<>(false, mensaje, null, errores, null);
    }
}
