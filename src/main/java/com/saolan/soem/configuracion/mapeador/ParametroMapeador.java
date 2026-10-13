package com.saolan.soem.configuracion.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.configuracion.dto.ParametroDTO;
import com.saolan.soem.configuracion.modelo.Parametro;

/**
 * Mapeo entidad/DTO de {@link Parametro}. El identificador nunca se toma del
 * cuerpo de la peticion: lo asigna la base (alta) o la ruta (modificacion).
 */
@Mapper(componentModel = "cdi")
public interface ParametroMapeador {

    @Mapping(target = "parametroId", ignore = true)
    Parametro aParametro(ParametroDTO parametroDTO);

    ParametroDTO aParametroDTO(Parametro parametro);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "parametroId", ignore = true)
    void actualizarEntidadDesdeDto(ParametroDTO parametroDTO, @MappingTarget Parametro parametro);
}
