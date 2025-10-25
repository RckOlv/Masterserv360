package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.RolDTO;
import com.masterserv.productos.entity.Rol;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RolMapper {

    RolDTO toRolDTO(Rol rol);

    List<RolDTO> toRolDTOList(List<Rol> roles);

    // DTO -> Entidad (Ignora ID y campos de auditoría)
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true), 
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    Rol toRol(RolDTO rolDTO);

    /**
     * Actualiza un rol existente.
     */
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    void updateRolFromDto(RolDTO dto, @MappingTarget Rol entity);
}