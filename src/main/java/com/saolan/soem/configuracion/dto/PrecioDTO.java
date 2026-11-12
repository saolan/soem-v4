package com.saolan.soem.configuracion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de una lista de precio.
 *
 * @param precioId identificador; ignorado en el cuerpo de POST/PUT
 * @param alcanc   {@code "TODOS"} genera los precios en cero de todos los
 *                 productos activos al crear la lista, {@code "NINGUNO"} no
 *                 genera nada; se ignora en PUT (inmutable). Ver
 *                 {@code AlcancePrecio}
 * @param factor   unidades que representa la lista (Unidad = 1, Docena = 12)
 */
public record PrecioDTO(

        Integer precioId,

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 32, message = "El código no puede superar los 32 caracteres")
        String codigo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        // Texto validado (y no el enum) para que un valor invalido responda 400 con el campo, no un error de lectura del JSON.
        @NotNull(message = "El alcance es obligatorio")
        @Pattern(regexp = "TODOS|NINGUNO", message = "El alcance debe ser TODOS o NINGUNO")
        String alcanc,

        @NotNull(message = "El factor es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El factor debe ser mayor que cero")
        @Digits(integer = 6, fraction = 6, message = "El factor admite hasta 6 enteros y 6 decimales")
        BigDecimal factor,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
