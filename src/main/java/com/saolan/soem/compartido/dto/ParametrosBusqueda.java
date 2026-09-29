package com.saolan.soem.compartido.dto;

/**
 * Parametros de paginacion y ordenamiento estandar para todos los listados
 * de la API: {@code ?pagina=1&tamanio=20&ordenarPor=campo&direccion=asc}.
 * <p>
 * Normaliza aqui solo lo generico (limites de {@code tamanio}, direccion
 * valida). La validacion de {@code ordenarPor} contra la lista de campos
 * permitidos de cada entidad es responsabilidad del repositorio que la
 * consume, porque esos campos son especificos de cada tabla.
 *
 * @param pagina     numero de pagina solicitada, en base 1
 * @param tamanio    cantidad de registros por pagina
 * @param ordenarPor nombre del campo por el cual ordenar
 * @param direccion  {@code asc} o {@code desc}
 */
public record ParametrosBusqueda(int pagina, int tamanio, String ordenarPor, String direccion) {

    public static final int TAMANIO_MINIMO = 1;
    public static final int TAMANIO_MAXIMO = 200;
    public static final int TAMANIO_PREDETERMINADO = 20;
    public static final String DIRECCION_ASCENDENTE = "asc";
    public static final String DIRECCION_DESCENDENTE = "desc";

    public ParametrosBusqueda {
        if (pagina < 1) {
            pagina = 1;
        }
        if (tamanio < TAMANIO_MINIMO) {
            tamanio = TAMANIO_PREDETERMINADO;
        } else if (tamanio > TAMANIO_MAXIMO) {
            tamanio = TAMANIO_MAXIMO;
        }
        if (!DIRECCION_DESCENDENTE.equalsIgnoreCase(direccion)) {
            direccion = DIRECCION_ASCENDENTE;
        } else {
            direccion = DIRECCION_DESCENDENTE;
        }
    }

    public boolean esDescendente() {
        return DIRECCION_DESCENDENTE.equals(direccion);
    }

    public int primerRegistro() {
        return (pagina - 1) * tamanio;
    }
}
