package com.saolan.soem.seguridad.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.seguridad.dto.MenuDTO;
import com.saolan.soem.seguridad.dto.MenuFiltroDTO;
import com.saolan.soem.seguridad.dto.MenuNodoDTO;
import com.saolan.soem.seguridad.servicio.MenuServicio;

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
 * Endpoints REST de administracion del catalogo de menus. El menu de la
 * sesion del usuario no se sirve aqui sino en {@code GET /auth/menu}, que
 * solo exige estar autenticado.
 */
@Path("/menus")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MenuControlador {

    // El permiso se controla a nivel de modulo (permisos 7000-7003).
    private static final String RECURSO = "seguridad-crud";

    @Inject
    private MenuServicio menuServicio;

    @GET
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscar(
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanio") @DefaultValue("20") int tamanio,
            @QueryParam("ordenarPor") @DefaultValue("menuId") String ordenarPor,
            @QueryParam("direccion") @DefaultValue("asc") String direccion,
            @QueryParam("descri") String descri,
            @QueryParam("menuSupeId") Integer menuSupeId,
            @QueryParam("estado") Boolean estado) {

        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(pagina, tamanio, ordenarPor, direccion);
        MenuFiltroDTO filtro = new MenuFiltroDTO(descri, menuSupeId, estado);

        List<MenuDTO> resultado = menuServicio.buscar(filtro, parametrosBusqueda);
        long totalRegistros = menuServicio.contar(filtro);
        Paginacion paginacion = Paginacion.de(parametrosBusqueda.pagina(), parametrosBusqueda.tamanio(), totalRegistros);

        return Response.ok(RespuestaApi.exitoPaginado(resultado, paginacion)).build();
    }

    @GET
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscarPorId(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(menuServicio.buscarPorId(id))).build();
    }

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = menuServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    /** Arbol completo, incluidos los inactivos, para asignar menus a roles. */
    @GET
    @Path("/arbol")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response arbol() {
        List<MenuNodoDTO> resultado = menuServicio.arbol();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @POST
    @RequierePermiso(recurso = RECURSO, accion = Accion.CREAR)
    public Response crear(@Valid MenuDTO menuDTO) {
        MenuDTO creado = menuServicio.crear(menuDTO);
        return Response.status(Response.Status.CREATED)
                .entity(RespuestaApi.exito(creado, "Menú creado correctamente"))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response modificar(@PathParam("id") Integer id, @Valid MenuDTO menuDTO) {
        MenuDTO modificado = menuServicio.modificar(id, menuDTO);
        return Response.ok(RespuestaApi.exito(modificado, "Menú modificado correctamente")).build();
    }

    @DELETE
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response eliminar(@PathParam("id") Integer id) {
        menuServicio.eliminar(id);
        return Response.ok(RespuestaApi.<Void>exito(null, "Menú eliminado correctamente")).build();
    }
}
