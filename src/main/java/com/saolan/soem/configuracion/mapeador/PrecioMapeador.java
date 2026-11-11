package com.saolan.soem.configuracion.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.configuracion.dto.PrecioDTO;
import com.saolan.soem.configuracion.modelo.Precio;

/**
 * Mapeo entidad/DTO de {@link Precio}. El identificador nunca se toma del
 * cuerpo de la peticion: lo asigna la base (alta) o la ruta (modificacion).
 * {@code alcanc} solo se toma en el alta: es inmutable una vez creada la
 * lista, porque ya decidio si se generaron sus precios.
 */
@Mapper(componentModel = "cdi")
public interface PrecioMapeador {

    @Mapping(target = "precioId", ignore = true)
    Precio aPrecio(PrecioDTO precioDTO);

    PrecioDTO aPrecioDTO(Precio precio);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "precioId", ignore = true)
    @Mapping(target = "alcanc", ignore = true)
    void actualizarEntidadDesdeDto(PrecioDTO precioDTO, @MappingTarget Precio precio);
}
