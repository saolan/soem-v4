package com.saolan.soem.configuracion.mapeador;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.saolan.soem.configuracion.dto.SucuCertEmisDTO;
import com.saolan.soem.configuracion.modelo.SucuCertEmis;

/**
 * Mapeo de {@link SucuCertEmis} solo en direccion entidad -&gt; DTO. La
 * direccion DTO -&gt; entidad no se automatiza: construir/actualizar esta
 * composicion 1:1 implica decidir si se crea o se reutiliza el registro
 * existente y si se conserva la clave almacenada, lo cual es una regla de
 * negocio que resuelve {@code SucursalServicio}, no el mapper.
 */
@Mapper(componentModel = "cdi")
public interface SucuCertEmisMapeador {

    @Mapping(target = "certEmisId", source = "certEmis.certEmisId")
    @Mapping(target = "clave", ignore = true)
    SucuCertEmisDTO aSucuCertEmisDTO(SucuCertEmis sucuCertEmis);
}
