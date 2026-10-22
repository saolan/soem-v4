package com.saolan.soem.seguridad.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.seguridad.dto.RolDTO;
import com.saolan.soem.seguridad.modelo.Rol;

/**
 * Mapeo entidad/DTO de {@link Rol}. Las composiciones {@code permisos},
 * {@code sucursales} y {@code menus} no forman parte de {@link RolDTO} (se administran como
 * sub-recursos propios, ver {@code RolServicio}); se ignoran explicitamente
 * para que MapStruct no reporte "unmapped target property".
 */
@Mapper(componentModel = "cdi")
public interface RolMapeador {

    @Mapping(target = "rolId", ignore = true)
    @Mapping(target = "permisos", ignore = true)
    @Mapping(target = "sucursales", ignore = true)
    @Mapping(target = "menus", ignore = true)
    Rol aRol(RolDTO rolDTO);

    RolDTO aRolDTO(Rol rol);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "rolId", ignore = true)
    @Mapping(target = "permisos", ignore = true)
    @Mapping(target = "sucursales", ignore = true)
    @Mapping(target = "menus", ignore = true)
    void actualizarEntidadDesdeDto(RolDTO rolDTO, @MappingTarget Rol rol);
}
