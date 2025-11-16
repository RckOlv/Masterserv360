package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.PermisoDTO;
import com.masterserv.productos.entity.Permiso;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PermisoMapper {

    PermisoDTO toPermisoDTO(Permiso permiso);
    
    List<PermisoDTO> toPermisoDTOList(List<Permiso> permisos);

    Permiso toPermiso(PermisoDTO permisoDTO);
}