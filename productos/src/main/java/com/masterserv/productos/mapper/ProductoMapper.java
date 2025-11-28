package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoPublicoDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    // ============================
    //      ADMIN / VENDEDOR
    // ============================
    @Mappings({
        @Mapping(source = "categoria.id", target = "categoriaId"),
        @Mapping(source = "categoria.nombre", target = "categoriaNombre"),
        @Mapping(source = "loteReposicion", target = "loteReposicion")
    })
    ProductoDTO toProductoDTO(Producto producto);

    List<ProductoDTO> toProductoDTOList(List<Producto> productos);

    @Mapping(source = "categoriaId", target = "categoria")
    Producto toProducto(ProductoDTO productoDTO);


    // ============================
    //   UPDATE desde DTO -> ENTIDAD
    // ============================
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "stockActual", ignore = true),
        @Mapping(target = "categoria", ignore = true),
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true),
        @Mapping(source = "loteReposicion", target = "loteReposicion")
    })
    void updateProductoFromDto(ProductoDTO dto, @MappingTarget Producto entity);


    // Helper para categoría
    default Categoria map(Long categoriaId) {
        if (categoriaId == null) return null;
        Categoria categoria = new Categoria();
        categoria.setId(categoriaId);
        return categoria;
    }


    // ============================
    //    PORTAL PÚBLICO (CLIENTE)
    // ============================
    @Mappings({
        @Mapping(source = "categoria.nombre", target = "nombreCategoria"),
        @Mapping(source = "codigo", target = "codigo") // <--- ASEGURATE DE QUE ESTÉ
    })
    ProductoPublicoDTO toProductoPublicoDTO(Producto producto);
}
