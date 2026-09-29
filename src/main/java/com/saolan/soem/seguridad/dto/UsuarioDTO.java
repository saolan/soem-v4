package com.saolan.soem.seguridad.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos generales de un usuario (persona + credenciales de acceso, sin la
 * contrasena). Se usa tanto para lectura como para modificacion; el alta
 * usa {@link UsuarioCreacionDTO} (incluye la clave inicial) y el cambio de
 * clave se hace exclusivamente via {@code /auth/cambiar-clave}, nunca por
 * esta via.
 *
 * @param personaId identificador; ignorado en el cuerpo de PUT
 */
public record UsuarioDTO(

        Integer personaId,

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

        @NotNull(message = "Debe indicar si la clave caduca")
        Boolean caducaClav,

        OffsetDateTime caducaFechHora,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado) {
}
