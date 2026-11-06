package com.saolan.soem.compartido.prueba;

import java.util.function.Function;

import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.seguridad.dto.CambioSucursalDTO;
import com.saolan.soem.seguridad.dto.LoginRespuestaDTO;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

/**
 * Apoyo de los tests de integracion para obtener un access token con
 * permisos: si el usuario de prueba tiene varias sucursales, el login deja la
 * seleccion pendiente (token sin permisos) y aqui se completa eligiendo la
 * primera con {@code POST /auth/cambiar-sucursal}, igual que haria el
 * frontend.
 * <p>
 * Cada IT debe agregar esta clase a su despliegue
 * ({@code war.addClass(SesionPrueba.class)}): los tests corren dentro del
 * contenedor.
 */
public final class SesionPrueba {

    public static final String COOKIE_REFRESH_TOKEN = "refreshToken";

    private SesionPrueba() {
    }

    /**
     * @param respuestaLogin respuesta 200 de {@code POST /auth/login}
     * @param target         fabrica de {@link WebTarget} del IT a partir de una ruta relativa
     * @return access token con la sucursal ya elegida
     */
    public static String accessTokenConSucursal(Response respuestaLogin, Function<String, WebTarget> target) {
        NewCookie cookie = respuestaLogin.getCookies().get(COOKIE_REFRESH_TOKEN);
        LoginRespuestaDTO login = leerSesion(respuestaLogin);
        if (!login.requiereSeleccionSucursal()) {
            return login.accessToken();
        }

        Response respuestaSeleccion = target.apply("api/v1/auth/cambiar-sucursal")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken())
                .cookie(cookie)
                .post(Entity.entity(new CambioSucursalDTO(login.sucursales().get(0).valor()), MediaType.APPLICATION_JSON));
        if (respuestaSeleccion.getStatus() != 200) {
            throw new IllegalStateException("No se pudo seleccionar la sucursal de prueba: HTTP " + respuestaSeleccion.getStatus());
        }
        return leerSesion(respuestaSeleccion).accessToken();
    }

    public static LoginRespuestaDTO leerSesion(Response respuesta) {
        return respuesta.readEntity(new GenericType<RespuestaApi<LoginRespuestaDTO>>() { }).datos();
    }
}
