package com.saolan.soem.seguridad.mapeador;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.saolan.soem.seguridad.dto.MenuDTO;
import com.saolan.soem.seguridad.modelo.Menu;

/**
 * Mapeo entidad/DTO de {@link Menu}. {@code menuSupe} y {@code paginaRuta}
 * no se copian desde el DTO: ambos los asigna {@code MenuServicio} tras
 * validar la jerarquia y la ruta, y en un PUT un {@code null} en ellos es un
 * valor real (mover a raiz, grupo sin ruta) que la estrategia
 * {@code IGNORE} de las actualizaciones parciales descartaria.
 */
@Mapper(componentModel = "cdi")
public interface MenuMapeador {

    @Mapping(target = "menuId", ignore = true)
    @Mapping(target = "menuSupe", ignore = true)
    @Mapping(target = "paginaRuta", ignore = true)
    Menu aMenu(MenuDTO menuDTO);

    @Mapping(target = "menuSupeId", source = "menuSupe.menuId")
    MenuDTO aMenuDTO(Menu menu);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "menuId", ignore = true)
    @Mapping(target = "menuSupe", ignore = true)
    @Mapping(target = "paginaRuta", ignore = true)
    void actualizarEntidadDesdeDto(MenuDTO menuDTO, @MappingTarget Menu menu);
}
