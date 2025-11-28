package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.RecompensaDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Recompensa;
import com.masterserv.productos.entity.ReglaPuntos;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface RecompensaMapper {

    @Mappings({
        @Mapping(source = "reglaPuntos.id", target = "reglaPuntosId"),
        @Mapping(source = "categoria.id", target = "categoriaId"),
        @Mapping(source = "categoria.nombre", target = "categoriaNombre")
    })
    RecompensaDTO toDto(Recompensa recompensa);

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "reglaPuntos", source = "reglaPuntosId", qualifiedByName = "idToReglaPuntos"),
        @Mapping(target = "categoria", source = "categoriaId", qualifiedByName = "idToCategoria")
    })
    Recompensa toEntity(RecompensaDTO dto);

    // --- Métodos Helper para MapStruct ---
    
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
}