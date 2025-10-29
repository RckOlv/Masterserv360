package com.masterserv.productos.mapper;

import com.masterserv.productos.dto.CarritoDTO;
import com.masterserv.productos.dto.ItemCarritoDTO;
import com.masterserv.productos.entity.Carrito;
import com.masterserv.productos.entity.ItemCarrito;
import com.masterserv.productos.entity.Producto; // Importar Producto
// import com.masterserv.productos.entity.Usuario; // Importar Usuario (No se usa directamente aquí)
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List; // Importar List
import java.util.Set; // Importar Set (Para el parámetro del método de lista)

@Mapper(
    componentModel = "spring", // Para inyección de dependencias
    imports = { BigDecimal.class }, // Para usar BigDecimal en expressions
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE // Ignorar nulos
)
public interface CarritoMapper {

    // --- Mapeo ItemCarrito (Entidad -> DTO) ---
    @Mappings({
        @Mapping(source = "producto.id", target = "productoId"),
        @Mapping(source = "producto.nombre", target = "productoNombre"),
        @Mapping(source = "producto.codigo", target = "productoCodigo"),
        // Usamos el precio de VENTA del producto para mostrar en el carrito
        @Mapping(source = "producto.precioVenta", target = "precioUnitarioVenta"),
        // Mapeamos el stock actual para mostrarlo
        @Mapping(source = "producto.stockActual", target = "stockDisponible"),
        // Calculamos el subtotal (precio VENTA * cantidad)
        // ¡OJO! Asegurarse que producto.precioVenta no sea null
        @Mapping(target = "subtotal", expression = "java(item.getProducto() != null && item.getProducto().getPrecioVenta() != null ? item.getProducto().getPrecioVenta().multiply(new BigDecimal(item.getCantidad())) : BigDecimal.ZERO)")
    })
    ItemCarritoDTO toItemCarritoDTO(ItemCarrito item);

    // MapStruct usa el método anterior para mapear Set<Entidad> a List<DTO>
    List<ItemCarritoDTO> toItemCarritoDTOList(Set<ItemCarrito> items);

    // --- Mapeo Carrito (Entidad -> DTO) ---
    @Mappings({
        @Mapping(source = "vendedor.id", target = "vendedorId"),
        // Mapea la colección de items usando el método toItemCarritoDTOList
        @Mapping(source = "items", target = "items"),
        // Ignoramos totalCarrito y cantidadItems porque los calcula el Service
        @Mapping(target = "totalCarrito", ignore = true),
        @Mapping(target = "cantidadItems", ignore = true)
    })
    CarritoDTO toCarritoDTO(Carrito carrito);

    // --- Mapeos DTO -> Entidad (No necesarios para CarritoService) ---
    // (Correcto no tenerlos definidos por ahora)
}