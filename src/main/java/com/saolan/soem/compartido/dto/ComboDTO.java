package com.saolan.soem.compartido.dto;

/**
 * Proyeccion minima para listas de seleccion (combos). Se construye siempre
 * por proyeccion Criteria, sin cargar la entidad completa.
 *
 * @param valor    identificador del registro (clave primaria)
 * @param etiqueta texto a mostrar al usuario
 */
public record ComboDTO(Integer valor, String etiqueta) {
}
