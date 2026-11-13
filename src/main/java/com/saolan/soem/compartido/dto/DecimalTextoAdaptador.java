package com.saolan.soem.compartido.dto;

import java.math.BigDecimal;

import com.saolan.soem.compartido.excepcion.ExcepcionDecimalInvalido;

import jakarta.json.bind.adapter.JsonbAdapter;

/**
 * Representa un {@link BigDecimal} como string decimal en el JSON de la API
 * ({@code "factor": "12.5"}), convencion del contrato con el frontend: un
 * numero JSON pierde precision al pasar por {@code Number} de JavaScript.
 * <p>
 * Aplica a todo importe o cantidad {@code BigDecimal} de los DTO mediante
 * {@code @JsonbTypeAdapter(DecimalTextoAdaptador.class)}. Solo cambia la
 * representacion JSON: el tipo en Java y en la base de datos no se altera.
 * <p>
 * Salida: sin ceros de relleno y sin notacion cientifica
 * ({@code 12.000000 → "12"}, {@code 1E+1 → "10"}). Entrada: un texto no
 * numerico lanza {@link ExcepcionDecimalInvalido}, que el manejador global
 * traduce a 400 con el campo afectado.
 */
public class DecimalTextoAdaptador implements JsonbAdapter<BigDecimal, String> {

    @Override
    public String adaptToJson(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros().toPlainString();
    }

    @Override
    public BigDecimal adaptFromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException excepcion) {
            throw new ExcepcionDecimalInvalido();
        }
    }
}
