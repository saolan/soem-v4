package com.saolan.soem.seguridad.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.seguridad.dto.UsuarioDTO;
import com.saolan.soem.seguridad.modelo.PersUsua;

/**
 * Mapeo entidad/DTO de {@link PersUsua}, combinando sus propios campos con
 * los de la {@link com.saolan.soem.seguridad.modelo.Persona} asociada en un
 * {@link UsuarioDTO} plano. {@code clave} nunca se mapea (ni de entrada ni
 * de salida): el alta la resuelve {@code UsuarioServicio} a partir de
 * {@link com.saolan.soem.seguridad.dto.UsuarioCreacionDTO} y el cambio de
 * clave es responsabilidad exclusiva de {@code AutenticacionServicio}.
 */
@Mapper(componentModel = "cdi")
public interface UsuarioMapeador {

    @Mapping(target = "cedulaRuc", source = "persona.cedulaRuc")
    @Mapping(target = "apelli", source = "persona.apelli")
    @Mapping(target = "nombre", source = "persona.nombre")
    @Mapping(target = "direcc", source = "persona.direcc")
    @Mapping(target = "telefo", source = "persona.telefo")
    @Mapping(target = "correo", source = "persona.correo")
    @Mapping(target = "fechaNaci", source = "persona.fechaNaci")
    UsuarioDTO aUsuarioDTO(PersUsua persUsua);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "personaId", ignore = true)
    @Mapping(target = "persona.personaId", ignore = true)
    @Mapping(target = "persona.estado", ignore = true)
    @Mapping(target = "persona.cedulaRuc", source = "cedulaRuc")
    @Mapping(target = "persona.apelli", source = "apelli")
    @Mapping(target = "persona.nombre", source = "nombre")
    @Mapping(target = "persona.direcc", source = "direcc")
    @Mapping(target = "persona.telefo", source = "telefo")
    @Mapping(target = "persona.correo", source = "correo")
    @Mapping(target = "persona.fechaNaci", source = "fechaNaci")
    @Mapping(target = "clave", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void actualizarEntidadDesdeDto(UsuarioDTO usuarioDTO, @MappingTarget PersUsua persUsua);
}
