package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.CategoriaDTO;
import com.masterserv.productos.entity.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping; 
import org.mapstruct.MappingTarget;
import java.math.BigDecimal;
import java.util.List;

@Mapper(
    componentModel = "spring",
    imports = { BigDecimal.class } 
)
public interface CategoriaMapper {

    @Mapping(source = "estado", target = "estado") // Ahora 'estado' existe en el DTO
    CategoriaDTO toCategoriaDTO(Categoria categoria);

    List<CategoriaDTO> toCategoriaDTOList(List<Categoria> categorias);

    // Mapeo inverso (DTO -> Entidad)
    @Mapping(target = "id", ignore = true) 
    @Mapping(target = "estado", ignore = true) // El estado se maneja en el Service
    @Mapping(target = "fechaCreacion", ignore = true) 
    @Mapping(target = "fechaModificacion", ignore = true)
    Categoria toCategoria(CategoriaDTO categoriaDTO);

    /**
     * Actualiza una entidad Categoria existente desde un DTO.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true) 
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    void updateCategoriaFromDto(CategoriaDTO dto, @MappingTarget Categoria entity);
}