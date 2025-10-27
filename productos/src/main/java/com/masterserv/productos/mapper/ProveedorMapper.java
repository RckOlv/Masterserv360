package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.ProveedorDTO;
import com.masterserv.productos.entity.Categoria; // Importar
import com.masterserv.productos.entity.Proveedor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import java.util.List;
import java.util.Set; // Importar
import java.util.stream.Collectors; // Importar

@Mapper(componentModel = "spring")
public interface ProveedorMapper {

    // --- Entidad -> DTO ---
    @Mappings({
        // Mapea el Set<Categoria> a Set<Long> usando el helper
        @Mapping(source = "categorias", target = "categoriaIds", qualifiedByName = "categoriasToCategoriaIds")
    })
    ProveedorDTO toProveedorDTO(Proveedor proveedor);

    List<ProveedorDTO> toProveedorDTOList(List<Proveedor> proveedores);

    // --- DTO -> Entidad ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        // Mapea el Set<Long> a Set<Categoria> usando el helper
        @Mapping(source = "categoriaIds", target = "categorias", qualifiedByName = "categoriaIdsToCategorias")
    })
    Proveedor toProveedor(ProveedorDTO proveedorDTO);

    // --- Actualización ---
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "estado", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        @Mapping(source = "categoriaIds", target = "categorias", qualifiedByName = "categoriaIdsToCategorias")
    })
    void updateProveedorFromDto(ProveedorDTO dto, @MappingTarget Proveedor entity);

    // --- Helpers para M:N ---

    @org.mapstruct.Named("categoriasToCategoriaIds")
    default Set<Long> categoriasToCategoriaIds(Set<Categoria> categorias) {
        if (categorias == null) {
            return null;
        }
        return categorias.stream().map(Categoria::getId).collect(Collectors.toSet());
    }

    @org.mapstruct.Named("categoriaIdsToCategorias")
    default Set<Categoria> categoriaIdsToCategorias(Set<Long> categoriaIds) {
        if (categoriaIds == null) {
            return null;
        }
        return categoriaIds.stream().map(id -> {
            Categoria categoria = new Categoria();
            categoria.setId(id);
            return categoria;
        }).collect(Collectors.toSet());
    }
}