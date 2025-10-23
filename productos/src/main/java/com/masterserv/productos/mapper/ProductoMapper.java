package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring") // Le dice a MapStruct que genere un Bean de Spring
public interface ProductoMapper {

    // Mapea los campos complejos (Entidad -> DTO)
    @Mappings({
        @Mapping(source = "categoria.id", target = "categoriaId"),
        @Mapping(source = "categoria.nombre", target = "categoriaNombre")
    })
    ProductoDTO toProductoDTO(Producto producto);

    // Mapea una lista de Entidades a una lista de DTOs
    List<ProductoDTO> toProductoDTOList(List<Producto> productos);

    // Mapea los campos complejos (DTO -> Entidad)
    @Mapping(source = "categoriaId", target = "categoria")
    Producto toProducto(ProductoDTO productoDTO);

    // Método helper para que MapStruct sepa cómo convertir un Long (categoriaId)
    // en una entidad Categoria. El ProductoService se encargará de esto.
    default Categoria map(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        Categoria categoria = new Categoria();
        categoria.setId(categoriaId);
        return categoria;
    }
}