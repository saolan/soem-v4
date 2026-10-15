package com.saolan.soem.seguridad.controlador;

import java.util.List;

import com.saolan.soem.compartido.dto.ComboDTO;
import com.saolan.soem.compartido.dto.Paginacion;
import com.saolan.soem.compartido.dto.ParametrosBusqueda;
import com.saolan.soem.compartido.dto.RespuestaApi;
import com.saolan.soem.compartido.seguridad.Accion;
import com.saolan.soem.compartido.seguridad.RequierePermiso;
import com.saolan.soem.seguridad.dto.PermisoDTO;
import com.saolan.soem.seguridad.dto.PermisoFiltroDTO;
import com.saolan.soem.seguridad.servicio.PermisoServicio;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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
 * Endpoints REST del catalogo Permiso. Sin {@code DELETE}: la tabla no tiene
 * columna {@code estado} (ver Javadoc de {@code Permiso}), por lo que este
 * catalogo no admite baja logica ni fisica desde la API (un borrado fisico
 * podria romper cualquier {@code rol_perm} que ya lo referencie).
 */
@Path("/permisos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PermisoControlador {

    // El permiso se controla a nivel de modulo (permisos 7000-7003).
    private static final String RECURSO = "seguridad-crud";

    @Inject
    private PermisoServicio permisoServicio;

    @GET
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscar(
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanio") @DefaultValue("20") int tamanio,
            @QueryParam("ordenarPor") @DefaultValue("recurso") String ordenarPor,
            @QueryParam("direccion") @DefaultValue("asc") String direccion,
            @QueryParam("descri") String descri,
            @QueryParam("recurso") String recurso,
            @QueryParam("accion") Accion accion) {

        ParametrosBusqueda parametrosBusqueda = new ParametrosBusqueda(pagina, tamanio, ordenarPor, direccion);
        PermisoFiltroDTO filtro = new PermisoFiltroDTO(descri, recurso, accion);

        List<PermisoDTO> resultado = permisoServicio.buscar(filtro, parametrosBusqueda);
        long totalRegistros = permisoServicio.contar(filtro);
        Paginacion paginacion = Paginacion.de(parametrosBusqueda.pagina(), parametrosBusqueda.tamanio(), totalRegistros);

        return Response.ok(RespuestaApi.exitoPaginado(resultado, paginacion)).build();
    }

    @GET
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response buscarPorId(@PathParam("id") Integer id) {
        return Response.ok(RespuestaApi.exito(permisoServicio.buscarPorId(id))).build();
    }

    @GET
    @Path("/combo")
    @RequierePermiso(recurso = RECURSO, accion = Accion.BUSCAR)
    public Response combo() {
        List<ComboDTO> resultado = permisoServicio.combo();
        return Response.ok(RespuestaApi.exito(resultado)).build();
    }

    @POST
    @RequierePermiso(recurso = RECURSO, accion = Accion.CREAR)
    public Response crear(@Valid PermisoDTO permisoDTO) {
        PermisoDTO creado = permisoServicio.crear(permisoDTO);
        return Response.status(Response.Status.CREATED)
                .entity(RespuestaApi.exito(creado, "Permiso creado correctamente"))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RequierePermiso(recurso = RECURSO, accion = Accion.MODIFICAR)
    public Response modificar(@PathParam("id") Integer id, @Valid PermisoDTO permisoDTO) {
        PermisoDTO modificado = permisoServicio.modificar(id, permisoDTO);
        return Response.ok(RespuestaApi.exito(modificado, "Permiso modificado correctamente")).build();
    }
}
