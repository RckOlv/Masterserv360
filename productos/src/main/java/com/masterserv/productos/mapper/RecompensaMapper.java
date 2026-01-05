package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.RecompensaDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Recompensa;
import com.masterserv.productos.entity.ReglaPuntos;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RecompensaMapper {

    // --- MENTOR: CORRECCIÓN DEL MAPPEO ---
    // Forzamos la extracción de ID y Nombre de la categoría
    @Mappings({
        @Mapping(source = "categoria.id", target = "categoriaId"),
        @Mapping(source = "categoria.nombre", target = "categoriaNombre")
    })
    RecompensaDTO toDto(Recompensa recompensa);

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "categoria", source = "categoriaId", qualifiedByName = "idToCategoria"),
        @Mapping(target = "stock", source = "stock") 
    })
    Recompensa toEntity(RecompensaDTO dto);

    // --- Métodos Helper ---
    
    @Named("idToReglaPuntos")
    default ReglaPuntos idToReglaPuntos(Long id) {
        if (id == null) return null;
        ReglaPuntos regla = new ReglaPuntos();
        regla.setId(id);
        return regla;
    }

    @Named("idToCategoria")
    default Categoria idToCategoria(Long id) {
        if (id == null) return null;
        Categoria categoria = new Categoria();
        categoria.setId(id);
        return categoria;
    }
    
    // --- MENTOR: IMPORTANTE ---
    // A veces MapStruct necesita ayuda extra si la relación es Lazy
    @AfterMapping
    default void afterToDto(Recompensa entity, @MappingTarget RecompensaDTO dto) {
        if (entity.getCategoria() != null) {
            dto.setCategoriaId(entity.getCategoria().getId());
            dto.setCategoriaNombre(entity.getCategoria().getNombre());
        }
    }
}