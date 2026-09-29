package com.saolan.soem.seguridad.servicio;

import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;

/**
 * Resultado interno de un login/refresh exitoso: separa el cuerpo JSON de
 * respuesta ({@link LoginRespuestaDTO}) del valor crudo del refresh token,
 * que nunca debe serializarse en el body y solo debe viajar como cookie
 * HttpOnly (armada por {@code AuthControlador}, no por este Service).
 */
public record ResultadoAutenticacion(LoginRespuestaDTO respuesta, String refreshTokenCrudo) {
}
