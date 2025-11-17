package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoPublicoDTO; 
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.MappingTarget; // <-- Importar MappingTarget

import java.util.List;

@Mapper(componentModel = "spring") 
public interface ProductoMapper {

    // --- MÉTODOS EXISTENTES (PARA ADMIN/VENDEDOR) ---
    @Mappings({
        @Mapping(source = "categoria.id", target = "categoriaId"),
        @Mapping(source = "categoria.nombre", target = "categoriaNombre")
    })
    ProductoDTO toProductoDTO(Producto producto);

    List<ProductoDTO> toProductoDTOList(List<Producto> productos);

    // Mapea los campos complejos (DTO Admin -> Entidad)
    @Mapping(source = "categoriaId", target = "categoria")
    Producto toProducto(ProductoDTO productoDTO);
    
    // --- Mentor: INICIO DE CAMBIOS ---
    // (Este método soluciona el BUG de stock=0 y añade el loteReposicion)
    /**
     * Actualiza una entidad Producto desde un DTO, ignorando campos clave.
     */
    @Mappings({
        @Mapping(target = "id", ignore = true), // Nunca actualiza el ID
        @Mapping(target = "stockActual", ignore = true), // ¡IGNORA EL STOCK ACTUAL!
        @Mapping(target = "categoria", ignore = true), // La categoría se maneja en el servicio
        @Mapping(target = "fechaCreacion", ignore = true),
        @Mapping(target = "fechaModificacion", ignore = true)
    })
    void updateProductoFromDto(ProductoDTO dto, @MappingTarget Producto entity);
    // --- Mentor: FIN DE CAMBIOS ---

    // Método helper
    default Categoria map(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        Categoria categoria = new Categoria();
        categoria.setId(categoriaId);
        return categoria;
    }


    // --- ¡NUEVOS MÉTODOS AÑADIDOS! (PARA EL PORTAL DE CLIENTE) ---
    @Mapping(source = "categoria.nombre", target = "nombreCategoria")
    ProductoPublicoDTO toProductoPublicoDTO(Producto producto);

    List<ProductoPublicoDTO> toProductoPublicoDTOList(List<Producto> productos);
}