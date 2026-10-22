package com.saolan.soem.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de una opcion de menu. Las reglas que dependen de otros registros
 * (menu superior existente, sin ciclos, profundidad maxima, ruta unica y
 * coherencia {@code detall}/{@code paginaRuta}) se validan en
 * {@code MenuServicio}.
 *
 * @param menuId     identificador; ignorado en el cuerpo de POST/PUT
 * @param menuSupeId menu superior; {@code null} para un menu raiz
 * @param paginaRuta ruta del frontend; obligatoria si {@code detall} es
 *                   {@code true} y nula si es un grupo
 * @param detall     {@code true} = pagina navegable, {@code false} = grupo
 */
public record MenuDTO(

        Integer menuId,

        Integer menuSupeId,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
        String descri,

        @Size(max = 255, message = "La ruta no puede superar los 255 caracteres")
        @Pattern(regexp = "^/[a-z0-9/-]+$",
                message = "La ruta debe iniciar con '/' y contener solo minúsculas, números, '-' y '/'")
        String paginaRuta,

        @NotNull(message = "Debe indicar si el menú es una página")
        Boolean detall,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
