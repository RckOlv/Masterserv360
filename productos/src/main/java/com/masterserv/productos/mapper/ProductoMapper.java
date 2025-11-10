package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoPublicoDTO; // <-- ¡Importar el nuevo DTO!
import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.entity.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring") // Le dice a MapStruct que genere un Bean de Spring
public interface ProductoMapper {

    // --- MÉTODOS EXISTENTES (PARA ADMIN/VENDEDOR) ---

    // Mapea los campos complejos (Entidad -> DTO Admin)
    @Mappings({
        @Mapping(source = "categoria.id", target = "categoriaId"),
        @Mapping(source = "categoria.nombre", target = "categoriaNombre")
        // Nota: Si 'stockActual' está en otra entidad (ej. 'stock.stockActual'),
        // deberías añadir ese mapping aquí también.
    })
    ProductoDTO toProductoDTO(Producto producto);

    // Mapea una lista de Entidades a una lista de DTOs Admin
    List<ProductoDTO> toProductoDTOList(List<Producto> productos);

    // Mapea los campos complejos (DTO Admin -> Entidad)
    @Mapping(source = "categoriaId", target = "categoria")
    Producto toProducto(ProductoDTO productoDTO);

    // Método helper para que MapStruct sepa cómo convertir un Long (categoriaId)
    // en una entidad Categoria.
    default Categoria map(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        Categoria categoria = new Categoria();
        categoria.setId(categoriaId);
        return categoria;
    }


    // --- ¡NUEVOS MÉTODOS AÑADIDOS! (PARA EL PORTAL DE CLIENTE) ---

    /**
     * Mapea la Entidad Producto (completa) al DTO Público (simple y seguro).
     *
     * @param producto La entidad JPA.
     * @return El DTO para el catálogo público.
     */
    @Mapping(source = "categoria.nombre", target = "nombreCategoria")
    // MapStruct mapeará automáticamente los campos con el mismo nombre:
    // - id -> id
    // - nombre -> nombre
    // - descripcion -> descripcion
    // - precioVenta -> precioVenta
    // - stockActual -> stockActual  (¡Asumiendo que 'stockActual' está en la entidad Producto!)
    // - imagenUrl -> imagenUrl
    ProductoPublicoDTO toProductoPublicoDTO(Producto producto);

    /**
     * Mapea una lista de Entidades a una lista de DTOs Públicos.
     */
    List<ProductoPublicoDTO> toProductoPublicoDTOList(List<Producto> productos);
}