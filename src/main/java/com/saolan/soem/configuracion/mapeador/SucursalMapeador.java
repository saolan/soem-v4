package com.saolan.soem.configuracion.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.configuracion.dto.SucursalDTO;
import com.saolan.soem.configuracion.modelo.Sucursal;

/**
 * Mapeo entidad/DTO de {@link Sucursal}. Las composiciones
 * {@code certificadoEmisor} y {@code bodegas} no forman parte de
 * {@link SucursalDTO} (se administran como sub-recursos propios, ver
 * {@code SucursalServicio}); se
 * ignora explicitamente en ambas direcciones DTO -&gt; entidad para que
 * MapStruct no reporte "unmapped target property", ya que la entidad
 * {@link Sucursal} si tiene ese campo.
 */
@Mapper(componentModel = "cdi")
public interface SucursalMapeador {

    @Mapping(target = "sucursalId", ignore = true)
    @Mapping(target = "certificadoEmisor", ignore = true)
    @Mapping(target = "bodegas", ignore = true)
    Sucursal aSucursal(SucursalDTO sucursalDTO);

    SucursalDTO aSucursalDTO(Sucursal sucursal);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "sucursalId", ignore = true)
    @Mapping(target = "certificadoEmisor", ignore = true)
    @Mapping(target = "bodegas", ignore = true)
    void actualizarEntidadDesdeDto(SucursalDTO sucursalDTO, @MappingTarget Sucursal sucursal);
}
