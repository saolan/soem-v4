package com.saolan.soem.seguridad.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para dar de alta un usuario nuevo: crea la {@code Persona} y el
 * {@code PersUsua} en la misma operacion. {@code clave} viaja en texto
 * plano solo en este request de entrada; el {@code Service} la hashea antes
 * de persistir y nunca la vuelve a exponer.
 */
public record UsuarioCreacionDTO(

        @NotBlank(message = "La cédula o RUC es obligatoria")
        @Size(max = 13, message = "La cédula o RUC no puede superar los 13 caracteres")
        String cedulaRuc,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apelli,

        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @Size(max = 100, message = "La dirección no puede superar los 100 caracteres")
        String direcc,

        @Size(max = 50, message = "El teléfono no puede superar los 50 caracteres")
        String telefo,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
        String correo,

        LocalDate fechaNaci,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 100, message = "El nombre de usuario no puede superar los 100 caracteres")
        String usuari,

        @NotBlank(message = "La clave es obligatoria")
        @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
        String clave,

        @NotNull(message = "Debe indicar si la clave caduca")
        Boolean caducaClav,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
