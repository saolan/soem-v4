package com.saolan.soem.compartido.seguridad;

/**
 * Acciones de permiso soportadas por el sistema, segun AGENTS.md. Cada
 * endpoint protegido declara el recurso y la accion concreta que requiere,
 * en lugar de proteger solo por rol.
 */
public enum Accion {
    BUSCAR,
    CREAR,
    MODIFICAR,
    ELIMINAR,
    ANULAR,
    DESPROCESAR,
    CONVERTIR,
    IMPRIMIR
}
