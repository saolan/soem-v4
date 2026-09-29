package com.saolan.soem.configuracion.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.configuracion.dto.CertEmisDTO;
import com.saolan.soem.configuracion.modelo.CertEmis;

/**
 * Mapeo entidad/DTO de {@link CertEmis}. El identificador nunca se toma del
 * cuerpo de la peticion: lo asigna la base (alta) o la ruta (modificacion).
 */
@Mapper(componentModel = "cdi")
public interface CertEmisMapeador {

    @Mapping(target = "certEmisId", ignore = true)
    CertEmis aCertEmis(CertEmisDTO certEmisDTO);

    CertEmisDTO aCertEmisDTO(CertEmis certEmis);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "certEmisId", ignore = true)
    void actualizarEntidadDesdeDto(CertEmisDTO certEmisDTO, @MappingTarget CertEmis certEmis);
}
