package com.saolan.soem.seguridad.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.seguridad.dto.UsuarioCreacionDTO;
import com.saolan.soem.seguridad.dto.UsuarioDTO;
import com.saolan.soem.seguridad.dto.UsuarioFiltroDTO;
import com.saolan.soem.seguridad.servicio.UsuarioServicio;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoints REST del agregado Usuario (raiz {@code seguridad/PersUsua}). El
 * alta usa {@link UsuarioCreacionDTO} (incluye la clave inicial); la
 * modificacion usa {@link UsuarioDTO} y nunca toca la clave (ver
 * {@code /auth/cambiar-clave}). Los roles asignados se administran como
 * sub-recurso propio bajo {@code /{id}/roles}.
 */
@Path("/usuarios")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UsuarioControlador {

    // El permiso se controla a nivel de modulo (permisos 7000-7003).
    private static final String RECURSO = "seguridad-crud";

    @Inject
    private UsuarioServicio usuarioServicio;

    @GET
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscar(
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanio") @DefaultValue("20") int tamanio,
            @QueryParam("ordenarPor") @DefaultValue("apelli") String ordenarPor,
            @QueryParam("direccion") @DefaultValue("asc") String direccion,
            @QueryParam("cedulaRuc") String cedulaRuc,
            @QueryParam("apelli") String apelli,
            @QueryParam("usuari") String usuari,
            @QueryParam("estado") Boolean estado) {

        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(pagina, tamanio, ordenarPor, direccion);
        UsuarioFiltroDTO filtro = new UsuarioFiltroDTO(cedulaRuc, apelli, usuari, estado);

        List<UsuarioDTO> resultado = usuarioServicio.buscar(filtro, parametrosBusqueda);
        long totalRegistros = usuarioServicio.contar(filtro);
        Paginacion paginacion = Paginacion.de(parametrosBusqueda.pagina(), parametrosBusqueda.tamanio(), totalRegistros);

        return Response.ok(RespuestaApi.exitoPaginado(resultado, paginacion)).build();
    }

    @GET
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscarPorId(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(usuarioServicio.buscarPorId(id))).build();
    }

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = usuarioServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @POST
    @RequierePermiso(recurso = RECURSO, accion = Accion.CREAR)
    public Response crear(@Valid UsuarioCreacionDTO usuarioCreacionDTO) {
        UsuarioDTO creado = usuarioServicio.crear(usuarioCreacionDTO);
        return Response.status(Response.Status.CREATED)
                .entity(RespuestaApi.exito(creado, "Usuario creado correctamente"))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response modificar(@PathParam("id") Integer id, @Valid UsuarioDTO usuarioDTO) {
        UsuarioDTO modificado = usuarioServicio.modificar(id, usuarioDTO);
        return Response.ok(RespuestaApi.exito(modificado, "Usuario modificado correctamente")).build();
    }

    @DELETE
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response eliminar(@PathParam("id") Integer id) {
        usuarioServicio.eliminar(id);
        return Response.ok(RespuestaApi.<Void>exito(null, "Usuario eliminado correctamente")).build();
    }

    @PUT
    @Path("/{id}/roles")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response reemplazarRoles(@PathParam("id") Integer id, @Valid List<Integer> rolIds) {
        usuarioServicio.reemplazarRoles(id, rolIds);
        return Response.ok(RespuestaApi.<Void>exito(null, "Roles del usuario actualizados correctamente")).build();
    }
}
