package com.saolan.soem.configuracion.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.configuracion.dto.BodegaDTO;
import com.saolan.soem.configuracion.modelo.Bodega;

/**
 * Mapeo entidad/DTO de {@link Bodega}. El identificador nunca se toma del
 * cuerpo de la peticion: lo asigna la base (alta) o la ruta (modificacion).
 */
@Mapper(componentModel = "cdi")
public interface BodegaMapeador {

    @Mapping(target = "bodegaId", ignore = true)
    Bodega aBodega(BodegaDTO bodegaDTO);

    BodegaDTO aBodegaDTO(Bodega bodega);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bodegaId", ignore = true)
    void actualizarEntidadDesdeDto(BodegaDTO bodegaDTO, @MappingTarget Bodega bodega);
}
