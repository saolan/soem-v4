package com.saolan.soem.seguridad.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.seguridad.dto.PermisoDTO;
import com.saolan.soem.seguridad.modelo.Permiso;

/**
 * Mapeo entidad/DTO de {@link Permiso}.
 */
@Mapper(componentModel = "cdi")
public interface PermisoMapeador {

    @Mapping(target = "permisoId", ignore = true)
    Permiso aPermiso(PermisoDTO permisoDTO);

    PermisoDTO aPermisoDTO(Permiso permiso);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "permisoId", ignore = true)
    void actualizarEntidadDesdeDto(PermisoDTO permisoDTO, @MappingTarget Permiso permiso);
}
