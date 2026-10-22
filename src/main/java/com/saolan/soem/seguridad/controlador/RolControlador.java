package com.saolan.soem.seguridad.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.seguridad.dto.AsignacionAccesoDTO;
import com.saolan.soem.seguridad.dto.RolDTO;
import com.saolan.soem.seguridad.dto.RolFiltroDTO;
import com.saolan.soem.seguridad.servicio.RolServicio;

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
 * Endpoints REST del agregado Rol. Los permisos, las sucursales y los menus
 * habilitados se administran como sub-recursos propios bajo
 * {@code /{id}/permisos}, {@code /{id}/sucursales} y {@code /{id}/menus}
 * (reemplazo en bloque), no viajan embebidos en
 * {@link RolDTO}.
 */
@Path("/roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RolControlador {

    // El permiso se controla a nivel de modulo (permisos 7000-7003).
    private static final String RECURSO = "seguridad-crud";

    @Inject
    private RolServicio rolServicio;

    @GET
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscar(
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanio") @DefaultValue("20") int tamanio,
            @QueryParam("ordenarPor") @DefaultValue("descri") String ordenarPor,
            @QueryParam("direccion") @DefaultValue("asc") String direccion,
            @QueryParam("descri") String descri,
            @QueryParam("estado") Boolean estado) {

        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(pagina, tamanio, ordenarPor, direccion);
        RolFiltroDTO filtro = new RolFiltroDTO(descri, estado);

        List<RolDTO> resultado = rolServicio.buscar(filtro, parametrosBusqueda);
        long totalRegistros = rolServicio.contar(filtro);
        Paginacion paginacion = Paginacion.de(parametrosBusqueda.pagina(), parametrosBusqueda.tamanio(), totalRegistros);

        return Response.ok(RespuestaApi.exitoPaginado(resultado, paginacion)).build();
    }

    @GET
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscarPorId(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(rolServicio.buscarPorId(id))).build();
    }

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = rolServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @POST
    @RequierePermiso(recurso = RECURSO, accion = Accion.CREAR)
    public Response crear(@Valid RolDTO rolDTO) {
        RolDTO creado = rolServicio.crear(rolDTO);
        return Response.status(Response.Status.CREATED)
                .entity(RespuestaApi.exito(creado, "Rol creado correctamente"))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response modificar(@PathParam("id") Integer id, @Valid RolDTO rolDTO) {
        RolDTO modificado = rolServicio.modificar(id, rolDTO);
        return Response.ok(RespuestaApi.exito(modificado, "Rol modificado correctamente")).build();
    }

    @DELETE
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.ELIMINAR)
    public Response eliminar(@PathParam("id") Integer id) {
        rolServicio.eliminar(id);
        return Response.ok(RespuestaApi.<Void>exito(null, "Rol eliminado correctamente")).build();
    }

    @PUT
    @Path("/{id}/permisos")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response reemplazarPermisos(@PathParam("id") Integer id, @Valid List<AsignacionAccesoDTO> asignaciones) {
        rolServicio.reemplazarPermisos(id, asignaciones);
        return Response.ok(RespuestaApi.<Void>exito(null, "Permisos del rol actualizados correctamente")).build();
    }

    @PUT
    @Path("/{id}/sucursales")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response reemplazarSucursales(@PathParam("id") Integer id, @Valid List<AsignacionAccesoDTO> asignaciones) {
        rolServicio.reemplazarSucursales(id, asignaciones);
        return Response.ok(RespuestaApi.<Void>exito(null, "Sucursales del rol actualizadas correctamente")).build();
    }

    @PUT
    @Path("/{id}/menus")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response reemplazarMenus(@PathParam("id") Integer id, @Valid List<AsignacionAccesoDTO> asignaciones) {
        rolServicio.reemplazarMenus(id, asignaciones);
        return Response.ok(RespuestaApi.<Void>exito(null, "Menús del rol actualizados correctamente")).build();
    }
}
