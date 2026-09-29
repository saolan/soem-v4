package com.saolan.soem.compartido.dto;

/**
 * Metadatos de paginacion incluidos en las respuestas de listados. Solo debe
 * estar presente en respuestas paginadas.
 *
 * @param pagina          numero de pagina actual, en base 1
 * @param tamanio         cantidad de registros solicitados por pagina
 * @param totalRegistros  total de registros que cumplen el criterio de busqueda
 * @param totalPaginas    total de paginas disponibles segun {@code tamanio}
 */
public record Paginacion(int pagina, int tamanio, long totalRegistros, int totalPaginas) {

    public static Paginacion de(int pagina, int tamanio, long totalRegistros) {
        int totalPaginas = tamanio > 0 ? (int) Math.ceil((double) totalRegistros / tamanio) : 0;
        return new Paginacion(pagina, tamanio, totalRegistros, totalPaginas);
    }
}
