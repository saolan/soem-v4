package com.saolan.soem.configuracion.modelo;

/**
 * Alcance de una lista de {@link Precio}: decide, solo al crearla, si se
 * generan los precios en cero ({@code PROD_PREC}) de todos los productos
 * activos. No habilita la lista en las sucursales ({@code SUCU_PREC}). Se persiste
 * por nombre en {@code PRECIO.ALCANC} (VARCHAR con {@code chk_precio_alcanc},
 * ver V9) y viaja igual en la API ({@code "TODOS"} / {@code "NINGUNO"}).
 */
public enum AlcancePrecio {

    /** No se generan precios: el usuario los va creando. */
    NINGUNO,

    /** Se generan precios en cero para todos los productos activos en todas las sucursales activas. */
    TODOS
}
