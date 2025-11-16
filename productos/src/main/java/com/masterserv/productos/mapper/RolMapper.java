package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.RolDTO;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.mapper.PermisoMapper; 
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PermisoMapper.class}) 
public interface RolMapper {

    // --- Mentor: CORRECCIÓN CRÍTICA ---
    // ¡Quitamos el @Mappings de aquí!
    // MapStruct es lo suficientemente inteligente para ignorar fechaCreacion
    // por sí solo, ya que no existe en RolDTO.
    RolDTO toRolDTO(Rol rol);
    // --- Fin Corrección ---

    List<RolDTO> toRolDTOList(List<Rol> roles);

    // ESTE ESTÁ CORRECTO (el target es la Entidad Rol)
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true), 
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    Rol toRol(RolDTO rolDTO);

    // ESTE TAMBIÉN ESTÁ CORRECTO
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        @Mapping(target = "permisos", ignore = true) // Los permisos se manejan en el servicio
    })
    void updateRolFromDto(RolDTO dto, @MappingTarget Rol entity);
}